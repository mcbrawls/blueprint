package net.mcbrawls.blueprint.region

import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.floor

/**
 * A region is a defined volume of space within a blueprint.
 */
interface Region {
    /**
     * All positions stored within this region.
     */
    fun getBlockPositions(offset: Vec3d = Vec3d.ZERO): Iterable<BlockPos>

    /**
     * A predicate to check if an entity is within the region.
     */
    fun contains(entity: Entity, offset: Vec3d = Vec3d.ZERO): Boolean

    /**
     * Loops through each position in the region and performs the given action.
     */
    fun forEachPosition(offset: Vec3d = Vec3d.ZERO, action: (BlockPos) -> Unit) {
        getBlockPositions(offset).forEach(action)
    }

    companion object {
        /**
         * Creates an iterable from a box.
         * @return a block position iterator
         */
        fun createBlockPositionSequence(min: Vec3d, max: Vec3d): Sequence<BlockPos> {
            val minX = floor(min.x).toInt()
            val minY = floor(min.y).toInt()
            val minZ = floor(min.z).toInt()
            val maxX = floor(max.x).toInt()
            val maxY = floor(max.y).toInt()
            val maxZ = floor(max.z).toInt()

            return sequence {
                for (x in minX..maxX) {
                    for (y in minY..maxY) {
                        for (z in minZ..maxZ) {
                            yield(BlockPos(x, y, z))
                        }
                    }
                }
            }
        }
    }
}
