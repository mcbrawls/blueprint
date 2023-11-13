package net.mcbrawls.blueprint.region

import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box

/**
 * A region is a defined volume of space within a blueprint.
 */
interface Region {
    /**
     * All positions stored within this region.
     */
    val positions: Set<BlockPos>

    /**
     * A predicate to check if an entity is within the region.
     */
    fun contains(entity: Entity): Boolean

    /**
     * Loops through each position in the region and performs the given action.
     */
    fun forEachPosition(action: (BlockPos) -> Unit) {
        positions.forEach(action)
    }

    companion object {
        /**
         * Creates an iterable from a box.
         * @return a block position iterator
         */
        fun iterateBoxBlockPositions(box: Box): Iterable<BlockPos> {
            val minX = box.minX.toInt()
            val minY = box.minY.toInt()
            val minZ = box.minZ.toInt()
            val maxX = box.maxX.toInt()
            val maxY = box.maxY.toInt()
            val maxZ = box.maxZ.toInt()
            return BlockPos.iterate(minX, minY, minZ, maxX, maxY, maxZ)
        }
    }
}
