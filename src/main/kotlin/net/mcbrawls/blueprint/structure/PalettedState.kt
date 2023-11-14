package net.mcbrawls.blueprint.structure

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.util.math.BlockPos

/**
 * The placement of a palette block state within a blueprint.
 */
data class PalettedState(
    /**
     * The offset position from the root of the blueprint placement.
     */
    val blockPos: BlockPos,

    /**
     * The index of the block state in the blueprint's palette.
     */
    val paletteIndex: Int
) {
    override fun toString(): String {
        return "PalettedState[#$paletteIndex, $blockPos]"
    }

    companion object {
        /**
         * The codec of a paletted state.
         */
        val CODEC: Codec<PalettedState> = RecordCodecBuilder.create { instance ->
            instance.group(
                BlockPos.CODEC.fieldOf("offset").forGetter(PalettedState::blockPos),
                Codec.INT.fieldOf("index").forGetter(PalettedState::paletteIndex)
            ).apply(instance, ::PalettedState)
        }
    }
}
