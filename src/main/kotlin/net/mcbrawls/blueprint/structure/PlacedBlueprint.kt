package net.mcbrawls.blueprint.structure

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.blueprint.BlueprintMod.logger
import net.mcbrawls.blueprint.anchor.Anchor
import net.mcbrawls.blueprint.region.CompoundRegion
import net.mcbrawls.blueprint.region.EmptyRegion
import net.mcbrawls.blueprint.region.PointRegion
import net.mcbrawls.blueprint.region.Region
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.util.function.Consumer

/**
 * A blueprint after it has been placed in the world.
 */
data class PlacedBlueprint(
    /**
     * The blueprint used in placement.
     */
    val blueprint: Blueprint,

    /**
     * Where the blueprint was placed in the world.
     */
    val position: BlockPos,
) {
    val offset: Vec3d = Vec3d.of(position)

    /**
     * The centre of the placed blueprint.
     */
    val center: BlockPos = position.add(blueprint.center)

    /**
     * All positions of this blueprint.
     */
    val positions: Set<BlockPos> by lazy {
        buildSet {
            val size = blueprint.size
            val sizeX = size.x
            val sizeY = size.y
            val sizeZ = size.z
            for (x in 0 until sizeX) {
                for (y in 0 until sizeY) {
                    for (z in 0 until sizeZ) {
                        add(position.add(x, y, z))
                    }
                }
            }
        }
    }

    /**
     * Places the source blueprint again.
     * @return this placed blueprint
     */
    fun place(world: ServerWorld): PlacedBlueprint {
        blueprint.place(world, position)
        return this
    }

    /**
     * Retrieves a single region from the source blueprint.
     * @return an offset region
     */
    fun getRegion(key: String): Region? {
        val region = blueprint.regions[key] ?: return null

        // return offset compound region
        return region.withOffset(offset)
    }

    /**
     * Retrieves all given regions from the source blueprint as a compound region.
     * @return an offset region
     */
    fun getRegionsCombined(vararg keys: String): Region {
        val nullableRegions = keys.associateWith { key -> blueprint.regions[key] }
        val regions = nullableRegions.values.filterNotNull()

        // verify regions
        if (regions.size != nullableRegions.size) {
            val invalidKeys = nullableRegions.filter { it.value == null }.keys
            logger.warn("Tried to access blueprint regions but were not present: $invalidKeys")
        }

        if (regions.isEmpty()) {
            return EmptyRegion
        }

        // return offset compound region
        return CompoundRegion.ofRegionsOffset(offset, *regions.toTypedArray())
    }

    /**
     * Gets the position of a point region.
     */
    fun getPointRegionPos(id: String): Vec3d {
        val region = getRegion(id) ?: throw IllegalArgumentException("Not a valid region: $id")
        val pointRegion = region as? PointRegion ?: throw IllegalArgumentException("Not a point region: $id")
        return pointRegion.pointPosition
    }

    /**
     * Gets all anchors of a given id.
     */
    fun getAnchors(id: String): List<Anchor> {
        return blueprint.anchors
            .filter { (testedId, _) -> testedId == id }
            .map { (_, anchor) -> getAnchorOffset(anchor) }
    }

    /**
     * Gets a given anchor offset for this placed blueprint.
     */
    fun getAnchorOffset(anchor: Anchor): Anchor {
        return Anchor(anchor.position.add(offset), anchor.rotation, anchor.data)
    }

    /**
     * Gets an anchor position from an anchor id assumed to be unique.
     * @return the placed offset position of the given anchor
     */
    fun getUniqueAnchor(id: String): Anchor? {
        return getAnchors(id).firstOrNull()
    }

    /**
     * Performs an action for every position in this placed blueprint.
     */
    fun forEachPosition(action: Consumer<BlockPos>) {
        return positions.forEach(action)
    }

    /**
     * Clears the blueprint from the world.
     */
    fun clear(world: ServerWorld) {
        forEachPosition { pos ->
           world.setBlockState(pos, Blocks.AIR.defaultState, Block.NOTIFY_LISTENERS or Block.FORCE_STATE or Block.NO_REDRAW)
        }
    }

    companion object {
        /**
         * The codec for a placed blueprint.
         */
        val CODEC: Codec<PlacedBlueprint> = RecordCodecBuilder.create { instance ->
            instance.group(
                Blueprint.CODEC.fieldOf("source_blueprint").forGetter(PlacedBlueprint::blueprint),
                BlockPos.CODEC.fieldOf("placed_position").forGetter(PlacedBlueprint::position)
            ).apply(instance, ::PlacedBlueprint)
        }
    }
}
