package net.mcbrawls.blueprint.region

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.blueprint.region.serialization.SerializableRegion
import net.mcbrawls.blueprint.region.serialization.SerializableRegionTypes
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

/**
 * A region defined by a single point in the world.
 * Useful for cases such as respawn or chest positions.
 */
data class PointRegion(
    /**
     * The position of the point.
     */
    val pointPosition: Vec3d
) : SerializableRegion(SerializableRegionTypes.POINT) {
    override fun getBlockPositions(offset: Vec3d): Set<BlockPos> {
        return setOf(BlockPos.ofFloored(pointPosition.add(offset)))
    }

    /**
     * A predicate to check if an entity is at the point position.
     */
    override fun contains(entity: Entity, offset: Vec3d): Boolean {
        return entity.blockPos.equals(pointPosition.add(offset))
    }

    override fun withOffset(offset: Vec3d): Region {
        return copy(pointPosition = pointPosition.add(offset))
    }

    companion object {
        /**
         * The codec of a cuboid region.
         */
        val CODEC: MapCodec<PointRegion> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Vec3d.CODEC.fieldOf("position").forGetter(PointRegion::pointPosition),
            ).apply(instance, ::PointRegion)
        }
    }
}
