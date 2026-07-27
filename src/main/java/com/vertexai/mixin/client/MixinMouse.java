package com.vertexai.mixin.client;

import com.vertexai.feature.impl.PerspectiveMod;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MixinMouse {

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onScroll(long window, double xoffset, double yoffset, CallbackInfo ci) {
        PerspectiveMod mod = PerspectiveMod.getInstance();
        if (mod.isRunning()) {
            float delta = (float) (yoffset * 0.5f);
            mod.cameraDistance = Math.max(1.0f, Math.min(25.0f, mod.cameraDistance - delta));
            ci.cancel(); // Prevent hotbar slot scrolling while adjusting camera distance
        }
    }
}
