package net.mcbrawls.blueprint.structure

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.andante.codex.encodeQuick
import net.mcbrawls.blueprint.editor.block.RegionBlock
import net.mcbrawls.blueprint.region.serialization.SerializableRegion
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtOps
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockBox
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i
import java.nio.file.Path
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
     * The size of the blueprint.
     */
    val size: Vec3i,

    /**
     * The regions stored within this blueprint.
     */
    val regions: Map<String, SerializableRegion>
) {
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
     * Places a position's block data to the world.
     */
    private fun placePosition(world: ServerWorld, position: BlockPos, offset: BlockPos, state: BlockState, blockEntityNbt: NbtCompound?, processor: BlockStateProcessor?) {
        val trueState = processor?.process(state) ?: state
        val truePos = position.add(offset)

        // state
        world.setBlockState(truePos, trueState, Block.NOTIFY_LISTENERS or Block.FORCE_STATE or Block.NO_REDRAW)

        // block entity
        if (blockEntityNbt != null) {
            val blockEntity = world.getBlockEntity(truePos)
            blockEntity?.read(blockEntityNbt, world.registryManager)
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
                    .orElseGet(::emptyMap)
                    .forGetter(Blueprint::blockEntities),
                Vec3i.CODEC
                    .fieldOf("size")
                    .forGetter(Blueprint::size),
                Codec.unboundedMap(Codec.STRING, SerializableRegion.CODEC)
                    .fieldOf("regions")
                    .orElseGet(::emptyMap)
                    .forGetter(Blueprint::regions)
            ).apply(instance, ::Blueprint)
        }

        /**
         * An entirely empty blueprint.
         */
        val EMPTY = Blueprint(emptyList(), emptyList(), emptyMap(), Vec3i.ZERO, emptyMap())

        /**
         * Flattens a set of placed blueprint futures into one progressive future.
         * @return a progressive future of combined futures and progress provider
         */
        fun flattenFutures(vararg futures: ProgressiveFuture<PlacedBlueprint>): ProgressiveFuture<*> {
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

        fun save(world: ServerWorld, min: BlockPos, max: BlockPos, blueprintId: Identifier): String {
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
                if (!RegionBlock.trySaveRegion(world, pos, state, regions)) {
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

            // create size
            val blockBox = BlockBox(min.x, min.y, min.z, max.x, max.y, max.z)
            val size = Vec3i(blockBox.blockCountX, blockBox.blockCountZ, blockBox.blockCountZ)

            // create blueprint
            val blueprint = Blueprint(palette, palettedBlockStates, blockEntities.associateBy(BlueprintBlockEntity::blockPos), size, regions)
            val nbt = CODEC.encodeQuick(NbtOps.INSTANCE, blueprint)

            // save blueprint
            val blueprintNamespace = blueprintId.namespace
            val blueprintPath = blueprintId.path

            val pathString = "generated/$blueprintNamespace/blueprints/$blueprintPath.nbt"
            val path = Path.of(pathString)

            path.parent.toFile().mkdirs()
            NbtIo.writeCompressed(nbt as NbtCompound, path)
            return pathString
        }
    }
}
