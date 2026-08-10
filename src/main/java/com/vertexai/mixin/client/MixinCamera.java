package com.vertexai.mixin.client;

import com.vertexai.feature.impl.PerspectiveMod;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class MixinCamera {

    @Shadow protected abstract void setPosition(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yRot, float xRot);
    @Shadow protected abstract void move(float distanceToMove, float y, float z);

    @Inject(method = "setup", at = @At("TAIL"))
    private void onSetupCamera(Level level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        PerspectiveMod mod = PerspectiveMod.getInstance();
        if (mod.isRunning()) {
            double x = Mth.lerp(partialTick, entity.xo, entity.getX());
            double y = Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getEyeHeight();
            double z = Mth.lerp(partialTick, entity.zo, entity.getZ());
            
            this.setPosition(x, y, z);
            this.setRotation(mod.freeLookYaw, mod.freeLookPitch);
            this.move(-mod.cameraDistance, 0.0f, 0.0f);
        }
    }

    @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
    private void onGetMaxZoom(float desiredCameraDistance, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> cir) {
        PerspectiveMod mod = PerspectiveMod.getInstance();
        if (mod.isRunning()) {
            cir.setReturnValue(desiredCameraDistance);
        }
    }
}
