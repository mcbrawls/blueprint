package net.mcbrawls.blueprint.structure

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.blueprint.BlueprintMod.logger
import net.mcbrawls.blueprint.region.CompoundRegion
import net.mcbrawls.blueprint.region.EmptyRegion
import net.mcbrawls.blueprint.region.Region
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
    val position: BlockPos
) {
    val offset: Vec3d = Vec3d.of(position)

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
    fun getRegion(key: String): Region {
        val region = blueprint.regions[key]

        // verify region
        if (region == null) {
            logger.warn("Tried to access blueprint region but was not present: $key")
            return EmptyRegion
        }

        // return offset compound region
        return CompoundRegion.ofRegionsOffset(offset, region)
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
           world.setBlockState(pos, Blocks.AIR.defaultState)
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
