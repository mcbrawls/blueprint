package net.mcbrawls.blueprint.editor.block

import com.mojang.serialization.MapCodec
import eu.pb4.polymer.core.api.block.PolymerBlock
import net.mcbrawls.blueprint.block.entity.RegionIdBlockEntity
import net.mcbrawls.blueprint.region.PointRegion
import net.mcbrawls.blueprint.region.serialization.SerializableRegion
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.Blocks
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

class PointRegionBlock(settings: Settings) : RegionBlock(settings), PolymerBlock {
    override fun saveRegion(
        world: World,
        pos: BlockPos,
        relativePos: BlockPos,
        blockEntity: RegionIdBlockEntity,
    ) : SerializableRegion {
        return PointRegion(Vec3d.of(relativePos))
    }

    override fun getPolymerBlockState(state: BlockState): BlockState {
        return Blocks.YELLOW_WOOL.defaultState
    }

    override fun getCodec(): MapCodec<out BlockWithEntity> {
        return CODEC
    }

    companion object {
        val CODEC: MapCodec<RegionBlock> = createCodec(::PointRegionBlock)
    }
}
