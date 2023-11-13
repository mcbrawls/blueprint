package net.mcbrawls.blueprint.region

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.blueprint.region.serialization.SerializableRegion
import net.mcbrawls.blueprint.region.serialization.SerializableRegionTypes
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos

/**
 * A region defined by a single point in the world.
 * Useful for cases such as respawn or chest positions.
 */
data class PointRegion(
    /**
     * The position of the point.
     */
    val position: BlockPos
) : SerializableRegion(SerializableRegionTypes.POINT) {
    override val positions: Set<BlockPos> = setOf(position)

    /**
     * A predicate to check if an entity is at the point position.
     */
    override fun contains(entity: Entity): Boolean {
        return entity.blockPos.equals(position)
    }

    companion object {
        /**
         * The codec of a cuboid region.
         */
        val CODEC: Codec<PointRegion> = RecordCodecBuilder.create { instance ->
            instance.group(
                BlockPos.CODEC.fieldOf("position").forGetter(PointRegion::position),
            ).apply(instance, ::PointRegion)
        }
    }
}
