package net.mcbrawls.blueprint

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.blueprint.region.serialization.SerializableRegion
import net.minecraft.block.BlockState
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

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
    val regions: List<SerializableRegion>,
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
                SerializableRegion.CODEC.listOf()
                    .fieldOf("regions")
                    .forGetter(Blueprint::regions),
            ).apply(instance, ::Blueprint)
        }

        /**
         * Creates a blueprint from block states in the world.
         */
        fun create(world: World, corner1: BlockPos, corner2: BlockPos): Blueprint {
            val palette: MutableList<BlockState> = mutableListOf()
            val palettedBlockStates: MutableList<PalettedState> = mutableListOf()

            val (min, max) = corner1.compared(corner2)
            BlockPos.iterate(min, max).forEach { pos ->
                // filter out air
                val state = world.getBlockState(pos)
                if (state.isAir) {
                    return@forEach
                }

                // add to palette if not present already
                if (!palette.contains(state)) {
                    palette.add(state)
                }

                // add to paletted block states
                val relativePos = pos.subtract(min)
                val paletteId = palette.indexOf(state)
                palettedBlockStates.add(PalettedState(relativePos, paletteId))
            }

            return Blueprint(palette, palettedBlockStates, listOf())
        }

        private fun BlockPos.compared(other: BlockPos): Pair<BlockPos, BlockPos> {
            val box = Box(Vec3d.of(this), Vec3d.of(other))
            val min = BlockPos.ofFloored(box.minX, box.minY, box.minZ)
            val max = BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ)
            return min to max
        }
    }
}
