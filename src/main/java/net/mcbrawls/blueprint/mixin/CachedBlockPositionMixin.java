package net.mcbrawls.blueprint.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.pattern.CachedBlockPosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CachedBlockPosition.class)
public class CachedBlockPositionMixin {
    // fix crashes when getBlockState happens to return null
    @ModifyReturnValue(method = "getBlockState", at = @At("RETURN"))
    private BlockState onGetBlockState(BlockState original) {
        return original == null ? Blocks.AIR.getDefaultState() : original;
    }
}
