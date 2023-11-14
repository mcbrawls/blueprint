package net.mcbrawls.blueprint.region

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.andante.codex.SetCodec.Companion.setOf
import dev.andante.codex.nullableFieldOf
import net.mcbrawls.blueprint.region.serialization.SerializableRegion
import net.mcbrawls.blueprint.structure.Blueprint
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

/**
 * A region defined of multiple other regions.
 * Useful for regions which cannot be defined through a single format.
 */
data class CompoundRegion(
    /**
     * The regions which compose this compound region.
     */
    val regions: Set<SerializableRegion>,

    /**
     * The global offset for this compound region.
     */
    val globalOffset: Vec3d? = null
) : Region {
    /**
     * A secondary vararg constructor.
     */
    constructor(
        vararg regions: SerializableRegion,
        globalOffset: Vec3d? = null
    ) : this(regions.toSet(), globalOffset)

    private fun getAbsoluteOffset(offset: Vec3d): Vec3d {
        return if (globalOffset != null) {
            offset.add(globalOffset)
        } else {
            offset
        }
    }

    override fun getBlockPositions(offset: Vec3d): Set<BlockPos> {
        val absoluteOffset = getAbsoluteOffset(offset)
        val mapped = regions.flatMap { region -> region.getBlockPositions(absoluteOffset) }
        return mapped.toSet()
    }

    override fun contains(entity: Entity, offset: Vec3d): Boolean {
        val absoluteOffset = getAbsoluteOffset(offset)
        return regions.any { region -> region.contains(entity, absoluteOffset) }
    }

    companion object {
        /**
         * The codec of a compound region.
         */
        val CODEC: Codec<CompoundRegion> = RecordCodecBuilder.create { instance ->
            instance.group(
                SerializableRegion.CODEC.setOf()
                    .fieldOf("regions")
                    .forGetter(CompoundRegion::regions),
                Vec3d.CODEC
                    .nullableFieldOf("offset")
                    .forGetter(CompoundRegion::globalOffset)
            ).apply(instance, ::CompoundRegion)
        }

        /**
         * Creates a compound region from the given regions of a blueprint.
         * @return a compound region
         */
        fun of(blueprint: Blueprint, vararg keys: String): CompoundRegion {
            val regions = keys.mapNotNull(blueprint.regions::get)
            return CompoundRegion(regions.toSet())
        }
    }
}
