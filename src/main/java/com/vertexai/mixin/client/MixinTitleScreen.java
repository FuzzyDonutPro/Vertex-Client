package com.vertexai.mixin.client;

import com.vertexai.gui.particle.PlexusRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinTitleScreen {
    @Unique
    private PlexusRenderer plexusRenderer;
    
    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (plexusRenderer == null) {
            plexusRenderer = new PlexusRenderer();
        }
        TitleScreen screen = (TitleScreen) (Object) this;
        plexusRenderer.init(screen.width, screen.height);
        
        try {
            for (java.lang.reflect.Field field : TitleScreen.class.getDeclaredFields()) {
                if (field.getType().getSimpleName().contains("Panorama") || field.getType().getSimpleName().contains("CubeMap")) {
                    field.setAccessible(true);
                    field.set(this, null);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        if (plexusRenderer != null) {
            plexusRenderer.tick();
        }
    }
    
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void onRender(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (plexusRenderer != null) {
            plexusRenderer.render(context, delta);
        }
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void onRenderBackground(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ci.cancel();
    }
}
