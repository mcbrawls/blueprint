package net.mcbrawls.blueprint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.mcbrawls.blueprint.region.serialization.SerializableRegionTypes;
import net.mcbrawls.blueprint.serialization.SerializableRegion;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class CompoundRegion extends SerializableRegion {
    public static final Codec<CompoundRegion> CODEC = RecordCodecBuilder.create(
            instance -> {
                return instance.group(
                        SerializableRegion.CODEC.listOf()
                                .fieldOf("regions")
                                .forGetter(CompoundRegion::getRegions)
                ).apply(instance, CompoundRegion::new);
            }
    );

    private final List<SerializableRegion> regions;

    public CompoundRegion(List<SerializableRegion> regions) {
        super(SerializableRegionTypes.INSTANCE.getCOMPOUND());
        this.regions = regions;
    }

    public List<SerializableRegion> getRegions() {
        return regions;
    }

    @NotNull
    @Override
    public Set<BlockPos> getPositions() {
        return null;
    }

    @Override
    public boolean contains(@NotNull Entity entity) {
        return false;
    }

    @Override
    public void forEachPosition(@NotNull Function1<? super BlockPos, Unit> action) {

    }
}
