package com.vertexai.mixin.client;

import com.vertexai.macro.impl.misc.PerspectiveMod;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MixinMouse {

    @Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
    private void onGrabMouse(CallbackInfo ci) {
        if (com.vertexai.macro.MacroManager.getInstance().isRunning() || com.vertexai.macro.impl.misc.MouseUngrab.getInstance().isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onScroll(long window, double xoffset, double yoffset, CallbackInfo ci) {
        PerspectiveMod mod = PerspectiveMod.getInstance();
        if (mod.isActive()) {
            float delta = (float) (yoffset * 0.5f);
            mod.cameraDistance = Math.max(1.0f, Math.min(25.0f, mod.cameraDistance - delta));
            ci.cancel(); // Prevent hotbar slot scrolling while adjusting camera distance
        }
    }
}
