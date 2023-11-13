package net.mcbrawls.blueprint.serialization;

import com.mojang.serialization.Codec;
import net.mcbrawls.blueprint.region.Region;
import net.mcbrawls.blueprint.region.serialization.SerializableRegionTypes;

public abstract class SerializableRegion implements Region {
    public final Type type;

    public SerializableRegion(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public static final Codec<SerializableRegion> CODEC = SerializableRegionTypes.INSTANCE.getREGISTRY().getCodec().dispatch("type", SerializableRegion::getType, Type::codec);

    public record Type(
            Codec<? extends SerializableRegion> codec
    ) {}
}
