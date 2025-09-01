package net.mcbrawls.blueprint.region

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.blueprint.command.BlueprintEditorCommand.maxVec
import net.mcbrawls.blueprint.command.BlueprintEditorCommand.minVec
import net.mcbrawls.blueprint.region.Region.Companion.createBlockPositionSequence
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
        val (minPos, maxPos) = createMinMaxVectors(offset)
        return Box(minPos, maxPos)
    }

    private fun createMinMaxVectors(offset: Vec3d): Pair<Vec3d, Vec3d> {
        val offsetPosition = rootPosition.add(offset)
        val endPosition = offsetPosition.add(size)
        val minPos = minVec(offsetPosition, endPosition)
        val maxPos = maxVec(offsetPosition, endPosition)
        return Pair(minPos, maxPos)
    }

    /**
     * Calculates all positions for this cuboid region.
     */
    override fun getBlockPositions(offset: Vec3d): Iterable<BlockPos> {
        val (minPos, maxPos) = createMinMaxVectors(offset)
        return createBlockPositionSequence(minPos, maxPos).asIterable()
    }

    /**
     * A predicate to check if an entity is within the cuboid bounding box.
     */
    override fun contains(entity: Entity, offset: Vec3d): Boolean {
        val box = createBox(offset)
        return entity.boundingBox.intersects(box)
    }

    override fun withOffset(offset: Vec3d): Region {
        return copy(rootPosition = rootPosition.add(offset))
    }

    companion object {
        /**
         * The codec of a cuboid region.
         */
        val CODEC: MapCodec<CuboidRegion> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Vec3d.CODEC.fieldOf("root_position").forGetter(CuboidRegion::rootPosition),
                Vec3d.CODEC.fieldOf("size").forGetter(CuboidRegion::size)
            ).apply(instance, ::CuboidRegion)
        }
    }
}
