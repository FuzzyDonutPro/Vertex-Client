package com.vertexai.macro.impl.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.vertexai.Vertex;
import net.minecraft.client.Minecraft;


public final class SpinRenderController {

    private SpinRenderController() {
    }

    public static void applyAvatarSpin(Object renderState, PoseStack poseStack) { return; } public static void applyAvatarSpin_old(Object renderState, PoseStack poseStack) {
        if (Vertex.config() == null || Vertex.config().render == null) {
            return;
        }
        if (!Vertex.config().render.spin || Minecraft.getInstance().player == null) {
            return;
        }
        if (((net.minecraft.world.entity.Entity)renderState).getId() != Minecraft.getInstance().player.getId()) {
            return;
        }

        float speed = Vertex.config().render.spinSpeed;
        int mode = Vertex.config().render.spinMode;
        float ticks = ((net.minecraft.world.entity.Entity)renderState).tickCount;
        float rot = (ticks * speed * 6.0f) % 360.0f;
        float height = ((net.minecraft.world.entity.Entity)renderState).getBbHeight() > 0.0f ? ((net.minecraft.world.entity.Entity)renderState).getBbHeight() : 1.8f;
        float midHeight = height * 0.5f;

        switch (mode) {
            case 0:
                poseStack.mulPose(Axis.YP.rotationDegrees(-rot));
                break;
            case 1:
                poseStack.mulPose(Axis.YP.rotationDegrees(rot));
                break;
            case 2:
                poseStack.translate(0.0, midHeight, 0.0);
                poseStack.mulPose(Axis.XP.rotationDegrees(rot));
                poseStack.translate(0.0, -midHeight, 0.0);
                break;
            case 3:
                float t = ticks * Math.max(speed, 1.0f);
                poseStack.mulPose(Axis.XP.rotationDegrees((float) (Math.sin(t * 0.28f) * 170.0f)));
                poseStack.mulPose(Axis.YP.rotationDegrees((float) (Math.cos(t * 0.36f) * 170.0f)));
                poseStack.mulPose(Axis.ZP.rotationDegrees((float) (Math.sin(t * 0.44f) * 170.0f)));
                break;
            case 4:
                poseStack.translate(0.0, height + 0.1f, 0.0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
                break;
            case 5:
                float wobble = (float) Math.sin(ticks * 0.4f * Math.max(speed, 1.0f) / 10.0f) * 18.0f;
                poseStack.translate(0.0, midHeight, 0.0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(wobble));
                poseStack.mulPose(Axis.XP.rotationDegrees(wobble * 0.8f));
                poseStack.translate(0.0, -midHeight, 0.0);
                break;
            case 6:
                poseStack.mulPose(Axis.YP.rotationDegrees(rot));
                poseStack.translate(0.0, 0.0, 0.55);
                break;
            case 7:
                float pulse = 1.0f + (float) Math.sin(ticks * 0.22f * Math.max(speed, 1.0f) / 10.0f) * 0.12f;
                poseStack.translate(0.0, midHeight, 0.0);
                poseStack.scale(pulse, pulse, pulse);
                poseStack.translate(0.0, -midHeight, 0.0);
                break;
            default:
                break;
        }
    }
}
