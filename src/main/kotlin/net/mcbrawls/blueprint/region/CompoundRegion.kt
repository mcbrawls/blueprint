package net.mcbrawls.blueprint.region

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.blueprint.region.serialization.SerializableRegion
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos

/**
 * A region defined of multiple other regions.
 * Useful for regions which cannot be defined through a single format.
 */
data class CompoundRegion(
    /**
     * The regions which compose this compound region.
     */
    val regions: List<SerializableRegion>
) : Region {
    constructor(vararg regions: SerializableRegion) : this(regions.toList())

    override val positions: Set<BlockPos> = regions.flatMap(SerializableRegion::positions).toSet()

    override fun contains(entity: Entity): Boolean {
        return regions.any { region -> region.contains(entity) }
    }

    companion object {
        /**
         * The codec of a compound region.
         */
        val CODEC: Codec<CompoundRegion> = RecordCodecBuilder.create { instance ->
            instance.group(
                SerializableRegion.CODEC.listOf()
                    .fieldOf("regions")
                    .forGetter(CompoundRegion::regions)
            ).apply(instance, ::CompoundRegion)
        }
    }
}
