package net.mcbrawls.blueprint.mixin;

import net.mcbrawls.blueprint.resource.BlueprintManager;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelStorage.class)
public class LevelStorageMixin {
    @Inject(method = "createSession", at = @At("RETURN"))
    private void onCreateSession(String directoryName, CallbackInfoReturnable<LevelStorage.Session> cir) {
        BlueprintManager.INSTANCE.onSessionChange(cir.getReturnValue());
    }

    @Inject(method = "createSessionWithoutSymlinkCheck", at = @At("RETURN"))
    private void onCreateSessionWithoutSymlinkCheck(String directoryName, CallbackInfoReturnable<LevelStorage.Session> cir) {
        BlueprintManager.INSTANCE.onSessionChange(cir.getReturnValue());
    }
}
