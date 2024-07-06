package net.mcbrawls.blueprint.structure

import net.minecraft.block.BlockState

/**
 * Processes a block state.
 */
fun interface BlockStateProcessor {
    /**
     * Processes a block state.
     * @return the output block state
     */
    fun process(
        /**
         * The input block state.
         */
        state: BlockState
    ): BlockState
}
