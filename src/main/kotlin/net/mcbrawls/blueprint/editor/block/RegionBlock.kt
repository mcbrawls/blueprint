package net.mcbrawls.blueprint.editor.block

import net.mcbrawls.blueprint.region.PointRegion
import net.mcbrawls.blueprint.region.serialization.SerializableRegion
import net.minecraft.block.BlockState
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

interface RegionBlock {
    companion object {
        fun trySaveRegion(
            world: ServerWorld,
            pos: BlockPos,
            relativePos: BlockPos,
            state: BlockState,
            regions: MutableMap<String, SerializableRegion>
        ): Boolean {
            if (state.block is PointRegionBlock) {
                regions["${regions.size}"] = PointRegion(Vec3d.ofCenter(relativePos))
                return true
            }

            return false
        }
    }
}
