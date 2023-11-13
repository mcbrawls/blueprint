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

// TODO: test: see if radii need squaring

/**
 * A region defined by a sphere.
 */
data class SphericalRegion(
    /**
     * The root position of the region.
     */
    val rootPosition: Vec3d,

    /**
     * The radius of the sphere, expanding from the root position.
     */
    val radius: Double
) : SerializableRegion(SerializableRegionTypes.SPHERE) {
    override val positions: Set<BlockPos> = computePositions()

    override fun contains(entity: Entity): Boolean {
        return entity.squaredDistanceTo(rootPosition) <= radius
    }

    /**
     * Calculates all positions for this spherical region.
     */
    private fun computePositions(): Set<BlockPos> {
        // create box
        val diameter = radius * 2
        val box = Box.of(rootPosition, diameter, diameter, diameter)

        // iterate through all positions and find all that are within range for a sphere
        val boxPositions = iterateBoxBlockPositions(box)
        return boxPositions.filter(::isPositionWithinRadius).toSet()
    }

    private fun isPositionWithinRadius(position: BlockPos): Boolean {
        return position.getSquaredDistance(rootPosition) <= radius
    }

    companion object {
        /**
         * The codec of a spherical region.
         */
        val CODEC: Codec<SphericalRegion> = RecordCodecBuilder.create { instance ->
            instance.group(
                Vec3d.CODEC.fieldOf("root_position").forGetter(SphericalRegion::rootPosition),
                Codec.DOUBLE.fieldOf("radius").forGetter(SphericalRegion::radius)
            ).apply(instance, ::SphericalRegion)
        }
    }
}
