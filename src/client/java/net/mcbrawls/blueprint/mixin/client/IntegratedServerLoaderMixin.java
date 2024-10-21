package net.mcbrawls.blueprint.mixin.client;

import net.mcbrawls.blueprint.resource.BlueprintManager;
import net.minecraft.server.integrated.IntegratedServerLoader;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IntegratedServerLoader.class)
public class IntegratedServerLoaderMixin {
	@Inject(method = "createSession", at = @At("RETURN"))
	private void onCreateSession(String levelName, CallbackInfoReturnable<LevelStorage.Session> cir) {
		BlueprintManager.INSTANCE.onSessionChange(cir.getReturnValue());
	}
}
