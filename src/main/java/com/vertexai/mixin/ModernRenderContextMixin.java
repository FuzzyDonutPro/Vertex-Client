package com.vertexai.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = io.github.notenoughupdates.moulconfig.platform.ModernRenderContext.class, remap = false)
public class ModernRenderContextMixin {
    /**
     * @author Antigravity
     * @reason Fix MoulConfig crash due to DefaultVertexFormat fields being renamed/removed in 1.21.11
     */
    @Redirect(method = "<clinit>", at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/vertex/DefaultVertexFormat;field_1592:Lcom/mojang/blaze3d/vertex/VertexFormat;"))
    private static com.mojang.blaze3d.vertex.VertexFormat redirectPositionColor() {
        return com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR;
    }

    @Redirect(method = "<clinit>", at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/vertex/DefaultVertexFormat;field_1576:Lcom/mojang/blaze3d/vertex/VertexFormat;"))
    private static com.mojang.blaze3d.vertex.VertexFormat redirectPositionColorTexture() {
        return com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR;
    }
}
