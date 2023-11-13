package net.mcbrawls.blueprint.region.serialization

import com.mojang.serialization.Codec

/**
 * A type of serializable region.
 */
data class SerializableRegionType(
    /**
     * The codec of this serializable region type.
     */
    val codec: Codec<out SerializableRegion>
)
