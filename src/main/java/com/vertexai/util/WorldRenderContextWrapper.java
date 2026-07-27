package com.vertexai.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;

public class WorldRenderContextWrapper {
    private final WorldRenderContext context;

    public WorldRenderContextWrapper(WorldRenderContext context) {
        this.context = context;
    }

    public PoseStack matrixStack() {
        return context.matrices();
    }

    public MultiBufferSource.BufferSource consumers() {
        return (MultiBufferSource.BufferSource) context.consumers();
    }

    public Camera camera() {
        return Minecraft.getInstance().gameRenderer.getMainCamera();
    }
}
