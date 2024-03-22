package net.mcbrawls.blueprint.structure

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.blueprint.region.serialization.SerializableRegion
import net.minecraft.block.BlockState
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i
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
     * Places this blueprint in the world at the given position.
     * @return a placed blueprint
     */
    fun place(world: ServerWorld, position: BlockPos): PlacedBlueprint {
        forEach { offset, state ->
            world.setBlockState(position.add(offset), state)
        }

        return PlacedBlueprint(this, position)
    }

    /**
     * Performs the given action for every position in the blueprint.
     */
    fun forEach(action: BiConsumer<BlockPos, BlockState>) {
        palettedBlockStates.forEach { (offset, index) ->
            val state = palette[index]
            action.accept(offset, state)
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
                Vec3i.CODEC
                    .fieldOf("size")
                    .forGetter(Blueprint::size),
                Codec.unboundedMap(Codec.STRING, SerializableRegion.CODEC)
                    .fieldOf("regions")
                    .forGetter(Blueprint::regions)
            ).apply(instance, ::Blueprint)
        }

        /**
         * An entirely empty blueprint.
         */
        val EMPTY = Blueprint(emptyList(), emptyList(), Vec3i.ZERO, emptyMap())
    }
}
