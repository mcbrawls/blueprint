package net.mcbrawls.blueprint.region.serialization

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import net.mcbrawls.blueprint.region.CompoundRegion
import net.mcbrawls.blueprint.region.Region
import net.minecraft.util.math.Vec3d

/**
 * A region which can be serialized or deserialized by codecs.
 */
abstract class SerializableRegion(
    /**
     * The serialized type of this region.
     */
    val type: Type
) : Region {
    /**
     * Creates a compound of this region with the given offset.
     * @return a compound region
     */
    fun withOffset(offset: Vec3d): CompoundRegion {
        return CompoundRegion.ofRegionsOffset(offset, this)
    }

    /**
     * A type of serializable region.
     */
    data class Type(
        /**
         * The codec of this serializable region type.
         */
        val codec: MapCodec<out SerializableRegion>
    )

    companion object {
        /**
         * The codec for a serializable region, defined by its type.
         */
        val CODEC: Codec<SerializableRegion> = SerializableRegionTypes.REGISTRY.codec
            .dispatch(SerializableRegion::type, Type::codec)
    }
}
