package net.mcbrawls.blueprint.mixin.client;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRendererMixin {
    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/EntityRenderState;DDDLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/render/entity/state/EntityRenderState;hitbox:Lnet/minecraft/client/render/entity/state/EntityHitboxAndView;",
                    shift = At.Shift.BEFORE
            )
    )
    private <S extends EntityRenderState> void onRender(S state, double x, double y, double z, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, EntityRenderer<?, S> renderer, CallbackInfo ci) {
        /*if (BlueprintKeyBindings.INSTANCE.getTOGGLE_ANCHOR_VISUALISATION().isPressed()) {
            ClientPolymerEntityType trueType = PolymerClientUtils.getEntityType(entity);
            if (trueType != null && trueType.registryEntry() == BlueprintEntityTypes.INSTANCE.getANCHOR()) {
                float scale = 1.5f;
                matrices.scale(scale, scale, scale);

                state.nameLabelPos = Vec3d.ZERO;
                renderer.renderLabelIfPresent(state, Text.literal(String.valueOf(entity.getId())), matrices, vertices, light);

                matrices.scale(1 / scale, 1 / scale, 1 / scale);
            }
        } TODO */
    }
}
