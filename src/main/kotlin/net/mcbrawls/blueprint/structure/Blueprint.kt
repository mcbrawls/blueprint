package net.mcbrawls.blueprint.structure

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.blueprint.region.serialization.SerializableRegion
import net.minecraft.block.BlockState
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos

/**
 * Represents a structure blueprint.
 */
data class Blueprint(
    /**
     * The block state palette.
     */
    val palette: List<BlockState>,

    /**
     * The block states stored as their palette ids.
     */
    val palettedBlockStates: List<PalettedState>,

    /**
     * The regions stored within this blueprint.
     */
    val regions: Map<String, SerializableRegion>
) {
    /**
     * A list of the positions to their actual block states.
     */
    private val blockStates: Map<BlockPos, BlockState> = palettedBlockStates.associate { it.blockPos to palette[it.paletteIndex] }

    /**
     * Places this blueprint in the world at the given position.
     */
    fun place(world: ServerWorld, pos: BlockPos): Int {
        blockStates.forEach { (offset, state) ->
            world.setBlockState(pos.add(offset), state)
        }

        return blockStates.size
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
                Codec.unboundedMap(
                    Codec.STRING,
                    SerializableRegion.CODEC
                ).fieldOf("regions").forGetter(Blueprint::regions)
            ).apply(instance, ::Blueprint)
        }
    }
}
