package com.vertexai.mixin.client;

import com.vertexai.feature.impl.PerspectiveMod;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class MixinCamera {

    @Shadow protected abstract void setRotation(float yRot, float xRot);
    @Shadow protected abstract void move(float distanceToMove, float y, float z);

    @Inject(method = "setup", at = @At("TAIL"))
    private void onSetupCamera(Level level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        PerspectiveMod mod = PerspectiveMod.getInstance();
        if (mod.isRunning()) {
            this.setRotation(mod.freeLookYaw, mod.freeLookPitch);
            float extraDistance = mod.cameraDistance - 4.0f;
            if (extraDistance != 0.0f) {
                this.move(-extraDistance, 0.0f, 0.0f);
            }
        }
    }
}
