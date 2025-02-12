package net.mcbrawls.blueprint.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import eu.pb4.polymer.core.api.client.ClientPolymerEntityType;
import eu.pb4.polymer.core.api.client.PolymerClientUtils;
import net.mcbrawls.blueprint.entity.BlueprintEntityTypes;
import net.mcbrawls.blueprint.key.BlueprintKeyBindings;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRendererMixin {
    @Inject(
            method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;renderHitboxes:Z",
                    shift = At.Shift.BEFORE
            )
    )
    private <E extends Entity, S extends EntityRenderState> void onRender(E entity, double x, double y, double z, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertices, int light, EntityRenderer<? super E, S> renderer, CallbackInfo ci, @Local S state) {
        if (BlueprintKeyBindings.INSTANCE.getTOGGLE_ANCHOR_VISUALISATION().isPressed()) {
            ClientPolymerEntityType trueType = PolymerClientUtils.getEntityType(entity);
            if (trueType != null && trueType.registryEntry() == BlueprintEntityTypes.INSTANCE.getANCHOR()) {
                float scale = 1.5f;
                matrices.scale(scale, scale, scale);

                state.nameLabelPos = Vec3d.ZERO;
                renderer.renderLabelIfPresent(state, Text.literal(String.valueOf(entity.getId())), matrices, vertices, light);

                matrices.scale(1 / scale, 1 / scale, 1 / scale);
            }
        }
    }
}
