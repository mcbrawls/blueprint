package net.mcbrawls.blueprint.mixin;

import dev.andante.codex.CodexKt;
import net.mcbrawls.blueprint.BlueprintMod;
import net.mcbrawls.blueprint.player.BlueprintPlayerAccessor;
import net.mcbrawls.blueprint.player.BlueprintPlayerData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A mixin to manage blueprint player data on the server player.
 */
@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin implements BlueprintPlayerAccessor {
    @Unique
    private static final String BLUEPRINT_KEY = BlueprintMod.MOD_ID;

    @Unique
    @Nullable
    private BlueprintPlayerData blueprintPlayerData = null;

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void copyBlueprintData(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        // copy old blueprint data to new player
        this.blueprintPlayerData = BlueprintPlayerData.Companion.getBlueprintData(oldPlayer);
    }

	@Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
	private void writeBlueprintNbt(NbtCompound nbt, CallbackInfo info) {
        if (this.blueprintPlayerData != null) {
            // write blueprint data to nbt
            NbtElement blueprintNbt = CodexKt.encodeQuick(BlueprintPlayerData.Companion.getCODEC(), NbtOps.INSTANCE, this.blueprintPlayerData);
            nbt.put(BLUEPRINT_KEY, blueprintNbt);
        }
	}

	@Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
	private void readBlueprintNbt(NbtCompound nbt, CallbackInfo info) {
        // read blueprint data from nbt
        NbtElement blueprintNbt = nbt.getCompound(BLUEPRINT_KEY);
        this.blueprintPlayerData = CodexKt.decodeQuick(BlueprintPlayerData.Companion.getCODEC(), NbtOps.INSTANCE, blueprintNbt);
	}

    @Unique
    @Override
    public @Nullable BlueprintPlayerData getBlueprintPlayerData() {
        return this.blueprintPlayerData;
    }

    @Unique
    @Override
    public void setBlueprintPlayerData(@Nullable BlueprintPlayerData data) {
        this.blueprintPlayerData = data;
    }
}
