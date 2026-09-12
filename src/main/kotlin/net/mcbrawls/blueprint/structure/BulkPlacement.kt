package net.mcbrawls.blueprint.structure

import net.fabricmc.fabric.api.networking.v1.PlayerLookup
import net.minecraft.block.BlockState
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.ChunkSectionPos
import net.minecraft.world.Heightmap
import net.minecraft.world.chunk.WorldChunk
import net.minecraft.world.chunk.light.ChunkLightProvider

/**
 * Writes a large batch of blocks into a [ServerWorld] without paying [ServerWorld.setBlockState]'s per-block overhead:
 * the chunk lookup on every write, the read-back of the state just written, the collision shape comparison and the
 * sweep over every loaded mob's navigation, and the block update packet sent to each viewer of each block.
 *
 * Blocks go straight into the chunk section. The bookkeeping that must stay correct is still done per block —
 * heightmaps, section emptiness, the light engine's block check, the path node cache and points of interest — and each
 * touched chunk is marked for saving and resent to its viewers exactly once when the session is closed, as one chunk
 * packet or, for a chunk that took only a handful of blocks, as individual block updates.
 *
 * Positions holding a block entity, before or after the write, are handed back to [Blueprint.placePosition]: creating,
 * replacing and removing block entities is the chunk's own bookkeeping and is not worth reimplementing for the fraction
 * of a blueprint that needs it.
 *
 * Unlike [ServerWorld.setBlockState] this never runs neighbour or comparator updates, so it is only suitable for bulk
 * writes that would have been made with [net.minecraft.block.Block.FORCE_STATE] anyway. Use it through [use] so the
 * flush always runs.
 *
 * Writing straight into a chunk section is only safe on the server thread, which owns the chunk map: off it a chunk can
 * be unloaded and replaced part way through a placement, and every write made after that lands in a chunk the world no
 * longer holds. Use [onServerThread] to get there from a worker thread.
 */
class BulkPlacement(private val world: ServerWorld) : AutoCloseable {
    private val chunkManager = world.chunkManager
    private val lightingProvider = chunkManager.lightingProvider
    private val pathNodeTypeCache = world.pathNodeTypeCache

    private val bottomY = world.bottomY
    private val topY = world.topYInclusive

    private val touched = mutableMapOf<Long, TouchedChunk>()

    private var cachedPos: Long = NO_CHUNK
    private var cachedChunk: TouchedChunk? = null

    /**
     * Reused for the calls below, none of which hold onto the position they are given.
     */
    private val mutablePos = BlockPos.Mutable()

    init {
        val server = checkNotNull(world.server) { "Bulk placement needs the world's server" }
        check(server.isOnThread) { "Bulk placement must run on the server thread; see BulkPlacement.onServerThread" }
    }

    /**
     * Writes [state] at ([x], [y], [z]), reading [blockEntityNbt] into the block entity there if the state has one.
     * Positions outside the world's height limit are dropped, as [ServerWorld.setBlockState] drops them.
     */
    fun setBlock(x: Int, y: Int, z: Int, state: BlockState, blockEntityNbt: NbtCompound? = null) {
        if (y < bottomY || y > topY) {
            return
        }

        val entry = resolve(x shr 4, z shr 4)
        val chunk = entry.chunk

        val localX = x and 15
        val localY = y and 15
        val localZ = z and 15

        val sectionIndex = chunk.getSectionIndex(y)
        val section = chunk.getSection(sectionIndex)
        val previous = section.getBlockState(localX, localY, localZ)

        // block entities are the chunk's own bookkeeping; let the world place these few positions
        if (previous.hasBlockEntity() || state.hasBlockEntity()) {
            Blueprint.placePosition(world, BlockPos(x, y, z), state, blockEntityNbt, null)
            return
        }

        if (previous == state) {
            return
        }

        val wasEmpty = section.isEmpty
        section.setBlockState(localX, localY, localZ, state)

        // WorldChunk.setBlockState marks the chunk on every write; without it an unload pass drops what was written
        chunk.markNeedsSaving()

        entry.heightmaps.forEach { heightmap -> heightmap.trackUpdate(localX, y, localZ, state) }

        val isEmpty = section.isEmpty
        if (wasEmpty != isEmpty) {
            val sectionCoord = chunk.sectionIndexToCoord(sectionIndex)
            lightingProvider.setSectionStatus(ChunkSectionPos.from(chunk.pos, sectionCoord), isEmpty)
            chunkManager.onSectionStatusChanged(chunk.pos.x, sectionCoord, chunk.pos.z, isEmpty)
        }

        val pos = mutablePos.set(x, y, z)

        if (ChunkLightProvider.needsLightUpdate(previous, state)) {
            chunk.chunkSkyLight.isSkyLightAccessible(chunk, localX, y, localZ)
            lightingProvider.checkBlock(pos)
        }

        pathNodeTypeCache.invalidate(pos)
        world.onBlockStateChanged(pos, previous, state)

        entry.mark(x, y, z)
    }

    /**
     * Resolves the chunk covering the given chunk coordinates, loading it if needed. The last resolved chunk is
     * memoised, so a placement that walks its blocks in any spatially coherent order rarely touches the map.
     */
    private fun resolve(chunkX: Int, chunkZ: Int): TouchedChunk {
        val pos = ChunkPos.toLong(chunkX, chunkZ)
        val cached = cachedChunk
        if (cached != null && pos == cachedPos) {
            return cached
        }

        val entry = touched.getOrPut(pos) { TouchedChunk(world.getChunk(chunkX, chunkZ)) }

        cachedPos = pos
        cachedChunk = entry

        return entry
    }

    /**
     * Resends every touched chunk to its viewers.
     */
    override fun close() {
        if (touched.isEmpty()) {
            return
        }

        val entries = touched.values.toList()

        touched.clear()
        cachedPos = NO_CHUNK
        cachedChunk = null

        flush(entries)
    }

    private fun flush(entries: List<TouchedChunk>) {
        entries.forEach { entry ->
            val chunk = entry.chunk
            val players = PlayerLookup.tracking(world, chunk.pos)
            if (players.isEmpty()) {
                return@forEach
            }

            if (entry.tracksEveryChange) {
                // few enough blocks changed that individual updates cost less than the whole chunk
                entry.forEachChange { packed ->
                    val pos = BlockPos.fromLong(packed)
                    val packet = BlockUpdateS2CPacket(pos, chunk.getBlockState(pos))
                    players.forEach { player -> player.networkHandler.sendPacket(packet) }
                }
            } else {
                val packet = ChunkDataS2CPacket(chunk, lightingProvider, null, null)
                players.forEach { player -> player.networkHandler.sendPacket(packet) }
            }
        }
    }

    /**
     * A chunk written to by this session, and the blocks that were changed in it while there were few enough of them to
     * be worth remembering.
     */
    private class TouchedChunk(val chunk: WorldChunk) {
        /**
         * The heightmaps [WorldChunk.setBlockState] keeps up to date, resolved once for the whole session.
         */
        val heightmaps: Array<Heightmap> = arrayOf(
            chunk.getHeightmap(Heightmap.Type.MOTION_BLOCKING),
            chunk.getHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES),
            chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR),
            chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE),
        )

        private val changes = LongArray(MAX_TRACKED_CHANGES)
        private var changeCount = 0

        /**
         * Whether every change to this chunk was remembered, and so can be sent as individual block updates.
         */
        val tracksEveryChange: Boolean get() = changeCount <= MAX_TRACKED_CHANGES

        fun mark(x: Int, y: Int, z: Int) {
            if (changeCount < MAX_TRACKED_CHANGES) {
                changes[changeCount] = BlockPos.asLong(x, y, z)
            }

            changeCount++
        }

        fun forEachChange(action: (packed: Long) -> Unit) {
            for (index in 0 until changeCount.coerceAtMost(MAX_TRACKED_CHANGES)) {
                action(changes[index])
            }
        }
    }

    companion object {
        /**
         * Runs [action] on [world]'s server thread, blocking the caller until it has run, or inline when already there.
         *
         * Placement writes into chunk sections directly, which only the thread owning the chunk map may do. Running the
         * whole placement in one go also costs the server thread far less than a block at a time would: every
         * [ServerWorld.setBlockState] from a worker thread hands the chunk lookup back to the server thread and waits
         * for it, so a blueprint placed that way occupies the server thread once per block for as long as it takes.
         */
        fun onServerThread(world: ServerWorld, action: Runnable) {
            val server = checkNotNull(world.server) { "Bulk placement needs the world's server" }
            server.submitAndJoin(action)
        }

        /**
         * A chunk that took at most this many blocks is resent as individual block updates rather than as a whole
         * chunk, so that placing a handful of blocks does not cost every viewer a chunk's worth of data.
         */
        const val MAX_TRACKED_CHANGES: Int = 64

        /**
         * Stands for "no chunk memoised". Holds chunk z of [Int.MIN_VALUE], which is far outside the world's limits, so
         * it can never equal a real [ChunkPos.toLong].
         */
        private const val NO_CHUNK: Long = Long.MIN_VALUE
    }
}
