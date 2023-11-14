package net.mcbrawls.blueprint.structure

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.blueprint.region.serialization.SerializableRegion
import net.minecraft.block.BlockState
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

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
    val size: Vec3d,

    /**
     * The regions stored within this blueprint.
     */
    val regions: Map<String, SerializableRegion>
) {
    /**
     * A list of the positions to their actual block states.
     */
    private val blockStates: Map<BlockPos, BlockState> = palettedBlockStates
        .associate { palettedState -> palettedState.blockPos to palette[palettedState.paletteIndex] }

    /**
     * Places this blueprint in the world at the given position.
     * @return a placed blueprint
     */
    fun place(world: ServerWorld, position: BlockPos): PlacedBlueprint {
        blockStates.forEach { (offset, state) ->
            world.setBlockState(position.add(offset), state)
        }

        return PlacedBlueprint(this, position)
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
                Vec3d.CODEC
                    .fieldOf("size")
                    .forGetter(Blueprint::size),
                Codec.unboundedMap(
                    Codec.STRING,
                    SerializableRegion.CODEC
                ).fieldOf("regions").forGetter(Blueprint::regions)
            ).apply(instance, ::Blueprint)
        }
    }
}
