package net.mcbrawls.blueprint.structure

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.blueprint.region.serialization.SerializableRegion
import net.minecraft.block.BlockState
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i
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
    fun placeWithProgress(world: ServerWorld, position: BlockPos, processor: BlockStateProcessor? = null): Pair<CompletableFuture<PlacedBlueprint>, ProgressProvider> {
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

        return future to ProgressProvider(progress::get)
    }

    /**
     * Places a position's block data to the world.
     */
    private fun placePosition(world: ServerWorld, position: BlockPos, offset: BlockPos, state: BlockState, blockEntityNbt: NbtCompound?, processor: BlockStateProcessor?) {
        val trueState = processor?.process(state) ?: state
        val truePos = position.add(offset)

        // state
        world.setBlockState(truePos, trueState)

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
    }
}
