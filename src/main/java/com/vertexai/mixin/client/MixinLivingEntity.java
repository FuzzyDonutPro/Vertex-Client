package com.vertexai.mixin.client;

import com.vertexai.util.StrafeUtil;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for LivingEntity to override yaw during movement for strafe and swing animation speed.
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    private float previousStrafeYaw;

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
     * Override yaw during travel for strafe by temporarily setting the entity yaw.
     */
    @Inject(method = "travel", at = @At("HEAD"))
    private void onTravelHead(net.minecraft.world.phys.Vec3 travelVector, CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer && StrafeUtil.shouldEnable()) {
            this.previousStrafeYaw = ((LivingEntity)(Object)this).getYRot();
            ((LivingEntity)(Object)this).setYRot(StrafeUtil.yaw);
        }
    }

    @Inject(method = "travel", at = @At("RETURN"))
    private void onTravelReturn(net.minecraft.world.phys.Vec3 travelVector, CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer && StrafeUtil.shouldEnable()) {
            ((LivingEntity)(Object)this).setYRot(this.previousStrafeYaw);
        }
    }

    /**
     * Scale the swing animation speed for LocalPlayer based on the speed slider.
     */
    @Inject(method = "getAttackAnim", at = @At("RETURN"), cancellable = true)
    private void onGetAttackAnim(float partialTick, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof LocalPlayer) {
            if (com.vertexai.Vertex.config() != null && com.vertexai.Vertex.config().animations != null) {
                float speed = com.vertexai.Vertex.config().animations.swingSpeed;
                if (speed != 1.0f) {
                    cir.setReturnValue(Math.min(cir.getReturnValue() * speed, 1.0f));
                }
            }
        }
    }
}
