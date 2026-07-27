package com.vertexai.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = io.github.notenoughupdates.moulconfig.platform.ModernMinecraft.class, remap = false)
public class ModernMinecraftMixin {
    /**
     * @author Antigravity
     * @reason Fix MoulConfig crash due to unmapped method_15987
     */
    @Overwrite
    public boolean isKeyboardKeyDown(int keyCode) {
        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(net.minecraft.client.Minecraft.getInstance().getWindow(), keyCode);
    }
}
