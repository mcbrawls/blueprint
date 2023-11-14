package net.mcbrawls.blueprint.structure

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.util.math.BlockPos

/**
 * A paletted block state.
 */
data class PalettedState(
    /**
     * The offset position.
     */
    val blockPos: BlockPos,

    /**
     * The index of the block state in the palette.
     */
    val paletteIndex: Int
) {
    companion object {
        /**
         * The codec of this class.
         */
        val CODEC: Codec<PalettedState> = RecordCodecBuilder.create { instance ->
            instance.group(
                BlockPos.CODEC.fieldOf("offset").forGetter(PalettedState::blockPos),
                Codec.INT.fieldOf("index").forGetter(PalettedState::paletteIndex)
            ).apply(instance, ::PalettedState)
        }
    }
}
