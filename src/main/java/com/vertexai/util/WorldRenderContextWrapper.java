package com.vertexai.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;

public class WorldRenderContextWrapper {
    private final Object context;

    public WorldRenderContextWrapper(Object context) {
        this.context = context;
    }

    public PoseStack matrixStack() {
        if (context != null) {
            try {
                java.lang.reflect.Method m = context.getClass().getMethod("poseStack");
                Object res = m.invoke(context);
                if (res instanceof PoseStack) {
                    return (PoseStack) res;
                }
            } catch (Throwable ignored) {}
            try {
                java.lang.reflect.Method m = context.getClass().getMethod("matrices");
                Object res = m.invoke(context);
                if (res instanceof PoseStack) {
                    return (PoseStack) res;
                }
            } catch (Throwable ignored) {}
        }
        return new PoseStack();
    }

    public MultiBufferSource.BufferSource consumers() {
        if (context != null) {
            try {
                java.lang.reflect.Method m = context.getClass().getMethod("bufferSource");
                Object res = m.invoke(context);
                if (res instanceof MultiBufferSource.BufferSource) {
                    return (MultiBufferSource.BufferSource) res;
                }
            } catch (Throwable ignored) {}
            try {
                java.lang.reflect.Method m = context.getClass().getMethod("consumers");
                Object res = m.invoke(context);
                if (res instanceof MultiBufferSource.BufferSource) {
                    return (MultiBufferSource.BufferSource) res;
                }
            } catch (Throwable ignored) {}
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.renderBuffers() != null) {
            return mc.renderBuffers().bufferSource();
        }
        return null;
    }

    public Camera camera() {
        return Minecraft.getInstance().gameRenderer.getMainCamera();
    }
}
