package net.mcbrawls.blueprint.structure

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.andante.codex.ExtraCodecs
import net.mcbrawls.blueprint.BlueprintMod
import net.mcbrawls.blueprint.anchor.Anchor
import net.mcbrawls.blueprint.block.BlueprintBlocks
import net.mcbrawls.blueprint.block.entity.RegionIdBlockEntity
import net.mcbrawls.blueprint.block.region.RegionBlock
import net.mcbrawls.blueprint.entity.AnchorEntity
import net.mcbrawls.blueprint.entity.BlueprintEntityTypes
import net.mcbrawls.blueprint.region.PointRegion
import net.mcbrawls.blueprint.region.serialization.SerializableRegion
import net.mcbrawls.slate.Slate.Companion.slate
import net.mcbrawls.slate.tile.Tile.Companion.tile
import net.mcbrawls.slate.tile.TileGrid
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.SpawnReason
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.storage.NbtReadView
import net.minecraft.text.Text
import net.minecraft.util.ErrorReporter
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import net.minecraft.world.World
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

/**
 * Represents a structure blueprint.
 */
data class Blueprint(
    /**
     * The block state palette.
     */
    val palette: List<BlockState>,

    /**
     * Every block of the blueprint: an offset position and an index into [palette], packed into primitive arrays.
     */
    val blocks: BlockStore,

    /**
     * A list of block entities stored within the blueprint.
     */
    val blockEntities: Map<BlockPos, BlueprintBlockEntity>,

    /**
     * The regions stored within this blueprint.
     */
    val regions: Map<String, SerializableRegion>,

    /**
     * The anchors stored within this blueprint.
     */
    val anchors: List<Pair<String, Anchor>>,
) {
    init {
        require(blocks.maxPaletteIndex < palette.size) {
            "Blocks reference palette index ${blocks.maxPaletteIndex}, but the palette holds ${palette.size} entries"
        }
    }

    /**
     * The size of the blueprint.
     */
    val size: Vec3i = blocks.size

    /**
     * The centre of this blueprint.
     */
    val center: Vec3i = Vec3i(size.x / 2, size.y / 2, size.z / 2)

    /**
     * The total amount of blocks placed from this blueprint.
     */
    val totalBlocks: Int = blocks.count

    /**
     * Places this blueprint in the world at the given position.
     * @return a placed blueprint
     */
    fun place(world: ServerWorld, position: BlockPos, processor: BlockStateProcessor? = null): PlacedBlueprint {
        BulkPlacement(world).use { placement ->
            forEach(processedPalette(processor)) { x, y, z, state, blockEntityNbt ->
                placement.setBlock(position.x + x, position.y + y, position.z + z, state, blockEntityNbt)
            }
        }

        return PlacedBlueprint(this, position)
    }

    /**
     * Launches a completable future placing this blueprint in the world at the given position.
     * @return a placed blueprint future and a progress provider
     */
    fun placeWithProgress(world: ServerWorld, position: BlockPos, processor: BlockStateProcessor? = null): ProgressiveFuture<PlacedBlueprint> {
        val progress = AtomicReference(0.0f)

        val future: CompletableFuture<PlacedBlueprint> = CompletableFuture.supplyAsync {
            synchronized(world) {
                var i = 0
                BulkPlacement(world).use { placement ->
                    forEach(processedPalette(processor)) { x, y, z, state, blockEntityNbt ->
                        placement.setBlock(position.x + x, position.y + y, position.z + z, state, blockEntityNbt)
                        progress.set(++i / totalBlocks.toFloat())
                    }
                }
            }

            PlacedBlueprint(this, position)
        }

        return ProgressiveFuture(future, ProgressProvider(progress::get))
    }

    /**
     * Places creator markers in the world from this blueprint.
     */
    fun placeCreatorMarkers(world: ServerWorld, pos: BlockPos) {
        placeRegions(world, pos)
        placeAnchors(world, pos)
    }

    fun placeRegions(world: ServerWorld, pos: BlockPos) {
        regions.forEach { id, region ->
            if (region !is PointRegion) {
                return@forEach
            }

            val offset = BlockPos.ofFloored(region.pointPosition)
            placePointRegion(world, pos.add(offset), id)
        }
    }

    fun placePointRegion(world: ServerWorld, pos: BlockPos, id: String) {
        val state = BlueprintBlocks.POINT_REGION.defaultState
        world.setBlockState(pos, state)

        val blockEntity = RegionIdBlockEntity(pos, state)
        blockEntity.id = id
        world.addBlockEntity(blockEntity)
    }

    fun placeAnchors(world: ServerWorld, pos: BlockPos) {
        anchors.forEach { (id, anchor) ->
            BlueprintEntityTypes.ANCHOR.create(world, SpawnReason.CHUNK_GENERATION)?.also { entity ->
                entity.anchorId = id
                entity.data = anchor.data
                entity.setPosition(anchor.position.add(Vec3d.of(pos)))
                entity.rotate(anchor.rotation.x, false, anchor.rotation.y, false)
                world.spawnEntity(entity)
            }
        }
    }

    /**
     * Performs the given action for every block's offset position, block state and block entity nbt, in storage order.
     * [palette] is the palette to read states from, which defaults to this blueprint's own.
     */
    inline fun forEach(palette: List<BlockState> = this.palette, action: (x: Int, y: Int, z: Int, state: BlockState, blockEntityNbt: NbtCompound?) -> Unit) {
        val blockEntities = blockEntities
        val lookupPos = if (blockEntities.isEmpty()) null else BlockPos.Mutable()

        blocks.forEach { x, y, z, paletteIndex ->
            val blockEntityNbt = lookupPos?.let { pos -> blockEntities[pos.set(x, y, z)]?.nbt }
            action(x, y, z, palette[paletteIndex], blockEntityNbt)
        }
    }

    /**
     * This blueprint's palette with [processor] applied to each entry.
     *
     * A processed state depends only on the state it came from, so a placement processes each palette entry once
     * rather than once per block.
     * @return the palette itself when there is no processor
     */
    fun processedPalette(processor: BlockStateProcessor?): List<BlockState> {
        return if (processor == null) palette else palette.map(processor::process)
    }

    /**
     * Performs the given action for every block's offset position, in storage order.
     */
    inline fun forEachPosition(action: (x: Int, y: Int, z: Int) -> Unit) {
        blocks.forEach { x, y, z, _ -> action(x, y, z) }
    }

    companion object {
        /**
         * The codec of this class.
         */
        val CODEC: Codec<Blueprint> = RecordCodecBuilder.create { instance ->
            instance.group(
                BlockState.CODEC
                    .orElse(Consumer { error ->
                        BlueprintMod.logger.error("Could not load blockstate: $error")
                    }, Blocks.AIR.defaultState)
                    .listOf()
                    .fieldOf("palette")
                    .forGetter(Blueprint::palette),
                BlockStore.MAP_CODEC
                    .forGetter(Blueprint::blocks),
                BlueprintBlockEntity.CODEC.listOf()
                    .fieldOf("block_entities")
                    .xmap({ entry -> entry.associateBy(BlueprintBlockEntity::blockPos) }, { map -> map.values.toList() })
                    .orElse(emptyMap())
                    .forGetter(Blueprint::blockEntities),
                Codec.unboundedMap(Codec.STRING, SerializableRegion.CODEC)
                    .fieldOf("regions")
                    .orElse(emptyMap())
                    .forGetter(Blueprint::regions),
                Codec.withAlternative(
                    ExtraCodecs.nativePair(Codec.STRING.fieldOf("id").codec(), Anchor.CODEC).listOf(),
                    Codec.unboundedMap(Codec.STRING, Anchor.CODEC).xmap({ it.toList() }, { it.toMap() })
                )
                    .fieldOf("anchors")
                    .orElse(emptyList())
                    .forGetter(Blueprint::anchors),
            ).apply(instance, ::Blueprint)
        }

        /**
         * An entirely empty blueprint.
         */
        val EMPTY = Blueprint(emptyList(), BlockStore.EMPTY, emptyMap(), emptyMap(), emptyList())

        /**
         * Flattens a set of progressive futures into one progressive future.
         * @return a progressive future of combined futures and progress provider
         */
        fun flattenFutures(vararg futures: ProgressiveFuture<*>): ProgressiveFuture<*> {
            // create compounded future
            val future = CompletableFuture.runAsync {
                val completableFutures = futures.map(ProgressiveFuture<*>::future)
                completableFutures.forEach(CompletableFuture<*>::join)
            }

            // provide average progress
            val provider = ProgressProvider {
                val providers = futures.map(ProgressiveFuture<*>::progressProvider)
                val progresses = providers.map(ProgressProvider::getProgress)
                val average = progresses.average()
                average.toFloat()
            }

            return ProgressiveFuture(future, provider)
        }

        /**
         * Places a position's block data to the world.
         */
        fun placePosition(world: ServerWorld, position: BlockPos, offset: BlockPos, state: BlockState, blockEntityNbt: NbtCompound?, processor: BlockStateProcessor?) {
            placePosition(world, position.add(offset), state, blockEntityNbt, processor)
        }

        /**
         * Places a position's block data to the world.
         */
        fun placePosition(world: ServerWorld, pos: BlockPos, state: BlockState, blockEntityNbt: NbtCompound?, processor: BlockStateProcessor?) {
            val trueState = processor?.process(state) ?: state

            // state; block entities hold onto the position they are created with, so never hand them a mutable one
            val truePos = if (trueState.hasBlockEntity()) pos.toImmutable() else pos
            world.setBlockState(truePos, trueState, Block.NOTIFY_LISTENERS or Block.FORCE_STATE or Block.NO_REDRAW)

            // block entity
            if (blockEntityNbt != null) {
                val blockEntity = world.getBlockEntity(truePos)
                blockEntity?.read(NbtReadView.create(ErrorReporter.Logging(BlueprintMod.logger), world.registryManager, blockEntityNbt))
            }
        }

        fun save(world: ServerWorld, min: BlockPos, max: BlockPos): Blueprint {
            // list positions
            val positions = BlockPos.iterate(min, max)

            // create paletted positions
            val palette = mutableListOf<BlockState>()
            val paletteIndexes = mutableMapOf<BlockState, Int>()
            val blockEntities = mutableListOf<BlueprintBlockEntity>()
            val blocks = BlockStore.Builder()
            val regions = mutableMapOf<String, SerializableRegion>()

            positions.forEach { pos ->
                val relativePos = pos.subtract(min)

                // state
                val state = world.getBlockState(pos)
                if (!RegionBlock.trySaveRegion(world, pos, relativePos, state, regions)) {
                    if (!state.isAir) {
                        // build palette
                        val paletteId = paletteIndexes.getOrPut(state) {
                            val index = palette.size
                            require(index < BlockStore.MAX_PALETTE_SIZE) {
                                "Blueprint palette cannot hold more than ${BlockStore.MAX_PALETTE_SIZE} block states"
                            }

                            palette.add(state)
                            index
                        }

                        // store block
                        blocks.add(relativePos.x, relativePos.y, relativePos.z, paletteId)
                    }

                    // block entity
                    val blockEntity = world.getBlockEntity(pos)
                    if (blockEntity != null) {
                        val nbt = blockEntity.createNbt(world.registryManager)
                        blockEntities.add(BlueprintBlockEntity(relativePos, nbt))
                    }
                }
            }

            // create anchors
            val anchors = mutableListOf<Pair<String, Anchor>>()
            world.iterateEntities().filterIsInstance<AnchorEntity>().forEach { anchorEntity ->
                val id = anchorEntity.getOrCreateId()
                val anchor = anchorEntity.createAnchor(min)
                anchors.add(id to anchor)
            }

            // create blueprint
            return Blueprint(palette, blocks.build(), blockEntities.associateBy(BlueprintBlockEntity::blockPos), regions, anchors)
        }

        /**
         * Creates a storable identifier from the world key and position.
         * @return an id
         */
        fun createUniqueId(world: World, pos: Vec3d): String {
            val key = world.registryKey
            val worldId = key.value

            val data = worldId.toString() + pos.hashCode().toString()
            val uuid = UUID.nameUUIDFromBytes(data.toByteArray(StandardCharsets.UTF_8))

            return uuid.toString()
        }

        fun openInputGui(player: ServerPlayerEntity, slateTitle: Text, initialInput: String, closeCallback: (input: String) -> Unit) {
            slate {
                tiles = TileGrid.create(ScreenHandlerType.ANVIL)
                title = slateTitle

                tiles {
                    this[0] = tile(Items.PAPER) {
                        tooltip(initialInput)
                    }
                }

                callbacks {
                    var input = initialInput

                    onInput { _, _, newInput ->
                        input = newInput
                    }

                    onClose { slate, player ->
                        closeCallback.invoke(input)
                    }
                }
            }.open(player)
        }
    }
}
