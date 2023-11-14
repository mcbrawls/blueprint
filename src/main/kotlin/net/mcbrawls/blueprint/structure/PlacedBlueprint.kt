package net.mcbrawls.blueprint.structure

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.blueprint.BlueprintMod.logger
import net.mcbrawls.blueprint.region.CompoundRegion
import net.mcbrawls.blueprint.region.EmptyRegion
import net.mcbrawls.blueprint.region.Region
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

/**
 * A blueprint after it has been placed in the world.
 */
data class PlacedBlueprint(
    /**
     * The blueprint used in placement.
     */
    val sourceBlueprint: Blueprint,

    /**
     * Where the blueprint was placed in the world.
     */
    val placedPosition: BlockPos
) {
    val offset: Vec3d = Vec3d.of(placedPosition)

    /**
     * Places the source blueprint again.
     * @return this placed blueprint
     */
    fun place(world: ServerWorld): PlacedBlueprint {
        sourceBlueprint.place(world, placedPosition)
        return this
    }

    /**
     * Retrieves a single region from the source blueprint.
     * @return an offset region
     */
    fun getRegion(key: String): Region {
        val region = sourceBlueprint.regions[key]

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
        val nullableRegions = keys.associateWith { key -> sourceBlueprint.regions[key] }
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

    companion object {
        /**
         * The codec for a placed blueprint.
         */
        val CODEC: Codec<PlacedBlueprint> = RecordCodecBuilder.create { instance ->
            instance.group(
                Blueprint.CODEC.fieldOf("source_blueprint").forGetter(PlacedBlueprint::sourceBlueprint),
                BlockPos.CODEC.fieldOf("placed_position").forGetter(PlacedBlueprint::placedPosition)
            ).apply(instance, ::PlacedBlueprint)
        }
    }
}
