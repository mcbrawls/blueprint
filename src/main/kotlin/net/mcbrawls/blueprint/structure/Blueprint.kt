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
import java.util.function.BiConsumer

/**
 * Represents a structure blueprint.
 */
data class Blueprint(
    /**
     * The block state palette.
     */
    val palette: List<BlockState>,

    /**
     * A list of paletted states, mapping palette indexes to their positions.
     */
    val palettedBlockStates: List<PalettedState>,

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
    /**
     * The size of the blueprint.
     */
    val size: Vec3i = calculateBlueprintSize(palettedBlockStates.map(PalettedState::blockPos))

    /**
     * The centre of this blueprint.
     */
    val center: Vec3i = Vec3i(size.x / 2, size.y / 2, size.z / 2)

    /**
     * The total amount of blocks placed from this blueprint.
     */
    val totalBlocks: Int = palettedBlockStates.size

    /**
     * Places this blueprint in the world at the given position.
     * @return a placed blueprint
     */
    fun place(world: ServerWorld, position: BlockPos, processor: BlockStateProcessor? = null): PlacedBlueprint {
        forEach { offset, (state, blockEntityNbt) -> placePosition(world, position, offset, state, blockEntityNbt, processor) }
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
                forEach { offset, (state, blockEntityNbt) ->
                    placePosition(world, position, offset, state, blockEntityNbt, processor)
                    progress.set(++i / totalBlocks.toFloat())
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
                entity.rotate(anchor.rotation.x, anchor.rotation.y)
                world.spawnEntity(entity)
            }
        }
    }

    /**
     * Performs the given action for every position in the blueprint.
     */
    fun forEach(action: BiConsumer<BlockPos, Pair<BlockState, NbtCompound?>>) {
        palettedBlockStates.forEach { (offset, index) ->
            val state = palette[index]
            val blockEntity = blockEntities[offset]?.nbt
            action.accept(offset, state to blockEntity)
        }
    }

    companion object {
        /**
         * The codec of this class.
         */
        val CODEC: Codec<Blueprint> = RecordCodecBuilder.create { instance ->
            instance.group(
                BlockState.CODEC.listOf()
                    .fieldOf("palette")
                    .forGetter(Blueprint::palette),
                PalettedState.CODEC.listOf()
                    .fieldOf("block_states")
                    .forGetter(Blueprint::palettedBlockStates),
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
        val EMPTY = Blueprint(emptyList(), emptyList(), emptyMap(), emptyMap(), emptyList())

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
            val trueState = processor?.process(state) ?: state
            val truePos = position.add(offset)

            // state
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
            val blockEntities = mutableListOf<BlueprintBlockEntity>()
            val palettedBlockStates = mutableListOf<PalettedState>()
            val regions = mutableMapOf<String, SerializableRegion>()

            positions.forEach { pos ->
                val relativePos = pos.subtract(min)

                // state
                val state = world.getBlockState(pos)
                if (!RegionBlock.trySaveRegion(world, pos, relativePos, state, regions)) {
                    if (!state.isAir) {
                        // build palette
                        if (state !in palette) {
                            palette.add(state)
                        }

                        // create paletted state
                        val paletteId = palette.indexOf(state)
                        palettedBlockStates.add(PalettedState(relativePos, paletteId))
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
            return Blueprint(palette, palettedBlockStates, blockEntities.associateBy(BlueprintBlockEntity::blockPos), regions, anchors)
        }

        /**
         * Calculates the size of a blueprint from its positions.
         * @return the blueprint size
         */
        fun calculateBlueprintSize(positions: List<BlockPos>): BlockPos {
            if (positions.isEmpty()) {
                return BlockPos.ORIGIN
            }

            val minX = positions.minOf { it.x }
            val minY = positions.minOf { it.y }
            val minZ = positions.minOf { it.z }

            val maxX = positions.maxOf { it.x }
            val maxY = positions.maxOf { it.y }
            val maxZ = positions.maxOf { it.z }

            return BlockPos(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1)
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
