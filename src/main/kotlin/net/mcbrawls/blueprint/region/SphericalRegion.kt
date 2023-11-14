package net.mcbrawls.blueprint.region

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.blueprint.region.Region.Companion.iterateBoxBlockPositions
import net.mcbrawls.blueprint.region.serialization.SerializableRegion
import net.mcbrawls.blueprint.region.serialization.SerializableRegionTypes
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
    private fun isPositionWithinRadius(offset: Vec3d): (BlockPos) -> Boolean {
        val absolutePosition = rootPosition.add(offset)
        return { position -> position.getSquaredDistance(absolutePosition) <= radius }
    }

    /**
     * Calculates all positions for this spherical region.
     */
    override fun getBlockPositions(offset: Vec3d): Set<BlockPos> {
        // create box
        val diameter = radius * 2
        val box = Box.of(rootPosition.add(offset), diameter, diameter, diameter)

        // iterate through all positions and find all that are within range for a sphere
        val boxPositions = iterateBoxBlockPositions(box)
        val predicate = isPositionWithinRadius(offset)
        return boxPositions.filter(predicate).toSet()
    }

    override fun contains(entity: Entity, offset: Vec3d): Boolean {
        return entity.squaredDistanceTo(rootPosition.add(offset)) <= radius
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
