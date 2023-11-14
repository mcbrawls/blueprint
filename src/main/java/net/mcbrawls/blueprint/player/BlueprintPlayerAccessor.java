package net.mcbrawls.blueprint.player;

import org.jetbrains.annotations.Nullable;

public interface BlueprintPlayerAccessor {
    default @Nullable BlueprintPlayerData getBlueprintPlayerData() {
        throw new AssertionError();
    }

    default void setBlueprintPlayerData(@Nullable BlueprintPlayerData data) {
        throw new AssertionError();
    }
}
