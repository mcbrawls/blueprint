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
     * Creates a box of this cuboid region.
     */
    fun createBox(offset: Vec3d = Vec3d.ZERO): Box {
        val offsetPosition = rootPosition.add(offset)

        val rootBox = Box.from(offsetPosition)

        val endPosition = offsetPosition.add(size)
        val endBox = Box.from(endPosition)

        return rootBox.intersection(endBox)
    }

    /**
     * Calculates all positions for this cuboid region.
     */
    override fun getBlockPositions(offset: Vec3d): Set<BlockPos> {
        val box = createBox(offset)
        return iterateBoxBlockPositions(box).toSet()
    }

    /**
     * A predicate to check if an entity is within the cuboid bounding box.
     */
    override fun contains(entity: Entity, offset: Vec3d): Boolean {
        val box = createBox(offset)
        return entity.boundingBox.intersects(box)
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
