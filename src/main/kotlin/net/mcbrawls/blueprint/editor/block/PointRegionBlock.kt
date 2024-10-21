package net.mcbrawls.blueprint.editor.block

import eu.pb4.polymer.core.api.block.PolymerBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks

class PointRegionBlock(settings: Settings) : Block(settings), PolymerBlock, RegionBlock {
    override fun getPolymerBlockState(state: BlockState): BlockState {
        return Blocks.YELLOW_WOOL.defaultState
    }
}
