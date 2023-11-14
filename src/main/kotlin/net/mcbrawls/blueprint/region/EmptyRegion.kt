package net.mcbrawls.blueprint.region

import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

/**
 * A region with no size or position.
 */
object EmptyRegion : Region {
    override fun getBlockPositions(offset: Vec3d): Set<BlockPos> {
        return emptySet()
    }

    override fun contains(entity: Entity, offset: Vec3d): Boolean {
        return false
    }
}
