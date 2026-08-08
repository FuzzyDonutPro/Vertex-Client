package com.vertexai.mixin.client;

import com.vertexai.macro.features.misc.PerspectiveMod;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void onTurn(double yRot, double xRot, CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer) {
            PerspectiveMod mod = PerspectiveMod.getInstance();
            if (mod.isRunning()) {
                mod.freeLookYaw += (float) yRot * 0.15f;
                mod.freeLookPitch += (float) xRot * 0.15f;
                mod.freeLookPitch = Math.max(-90.0F, Math.min(90.0F, mod.freeLookPitch));
                ci.cancel();
            }
        }
    }

    @Inject(method = "getViewYRot", at = @At("HEAD"), cancellable = true)
    private void onGetViewYRot(float partialTicks, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof LocalPlayer) {
            PerspectiveMod mod = PerspectiveMod.getInstance();
            if (mod.isRunning()) {
                cir.setReturnValue(mod.freeLookYaw);
            }
        }
    }

    @Inject(method = "getViewXRot", at = @At("HEAD"), cancellable = true)
    private void onGetViewXRot(float partialTicks, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof LocalPlayer) {
            PerspectiveMod mod = PerspectiveMod.getInstance();
            if (mod.isRunning()) {
                cir.setReturnValue(mod.freeLookPitch);
            }
        }
    }
}
