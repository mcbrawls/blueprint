package net.mcbrawls.blueprint.region

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.blueprint.region.Region.Companion.iterateBoxBlockPositions
import net.mcbrawls.blueprint.region.serialization.SerializableRegionTypes
import net.mcbrawls.blueprint.serialization.SerializableRegion
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

/**
 * A region defined by a cuboid.
 */
data class CuboidRegion(
    /**
     * The root position of the region.
     */
    val rootPosition: Vec3d,

    /**
     * The size of the cuboid, expanding from the root position.
     */
    val size: Vec3d
) : SerializableRegion(SerializableRegionTypes.CUBOID) {
    /**
     * The cached box of this cuboid region.
     */
    private val box: Box = createBox()

    override val positions: Set<BlockPos> = computePositions()

    /**
     * A predicate to check if an entity is within the cuboid bounding box.
     */
    override fun contains(entity: Entity): Boolean {
        return entity.boundingBox.intersects(box)
    }

    /**
     * Creates a box of this cuboid region.
     */
    private fun createBox(): Box {
        val rootBox = Box.from(rootPosition)

        val endPosition = rootPosition.add(size)
        val endBox = Box.from(endPosition)

        return rootBox.intersection(endBox)
    }

    /**
     * Calculates all positions for this cuboid region.
     */
    private fun computePositions(): Set<BlockPos> {
        return iterateBoxBlockPositions(box).toSet()
    }

    companion object {
        /**
         * The codec of a cuboid region.
         */
        val CODEC: Codec<CuboidRegion> = RecordCodecBuilder.create { instance ->
            instance.group(
                Vec3d.CODEC.fieldOf("root_position").forGetter(CuboidRegion::rootPosition),
                Vec3d.CODEC.fieldOf("size").forGetter(CuboidRegion::size)
            ).apply(instance, ::CuboidRegion)
        }
    }
}
