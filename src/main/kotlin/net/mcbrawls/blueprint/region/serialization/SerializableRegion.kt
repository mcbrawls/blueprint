package net.mcbrawls.blueprint.region.serialization

import com.mojang.serialization.Codec
import net.mcbrawls.blueprint.region.Region

/**
 * A region which can be serialized or deserialized by codecs.
 */
abstract class SerializableRegion(
    /**
     * The serialized type of this region.
     */
    val type: SerializableRegionType
) : Region {
    companion object {
        /**
         * The codec for a serializable region, defined by its type.
         */
        val CODEC: Codec<SerializableRegion> = SerializableRegionTypes.REGISTRY.getCodec()
            .dispatch("type", SerializableRegion::type, SerializableRegionType::codec)
    }
}
