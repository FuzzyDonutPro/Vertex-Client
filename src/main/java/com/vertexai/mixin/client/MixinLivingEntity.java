package com.vertexai.mixin.client;

import com.vertexai.util.StrafeUtil;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for LivingEntity to override yaw during movement for strafe and swing animation speed.
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    /**
     * Override yaw during jump for strafe.
     */
    @Redirect(method = "jumpFromGround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"))
    private float overrideJumpYaw(LivingEntity self) {
        if (self instanceof LocalPlayer && StrafeUtil.shouldEnable()) {
            return StrafeUtil.yaw;
        }
        return self.getYRot();
    }

    /**
     * Override yaw during travel for strafe.
     */
    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F", ordinal = 0))
    private float overrideTravelYaw(LivingEntity self) {
        if (self instanceof LocalPlayer && StrafeUtil.shouldEnable()) {
            return StrafeUtil.yaw;
        }
        return self.getYRot();
    }

    /**
     * Slow down the swing animation for LocalPlayer if Slow or 1.8 Slow is selected.
     */
    @Inject(method = "getAttackAnim", at = @At("RETURN"), cancellable = true)
    private void onGetAttackAnim(float partialTick, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof LocalPlayer) {
            if (com.vertexai.Vertex.config() != null && com.vertexai.Vertex.config().gui != null) {
                int anim = com.vertexai.Vertex.config().gui.swingAnimation;
                if (anim == 2 || anim == 3) {
                    cir.setReturnValue(cir.getReturnValue() * 0.5f);
                }
            }
        }
    }
}
