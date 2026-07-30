package com.vertexai.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.vertexai.Vertex;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer {

    @Inject(method = "applyItemArmTransform", at = @At("HEAD"), cancellable = true)
    private void onApplyItemArmTransform(PoseStack poseStack, HumanoidArm humanoidArm, float swingProgress, CallbackInfo ci) {
        if (Vertex.config() == null || Vertex.config().gui == null) return;

        int anim = Vertex.config().gui.swingAnimation; // 0=Normal, 1=1.8, 2=Slow, 3=1.8 Slow, 4=Pitch

        if (anim == 1 || anim == 3) { // 1.8 or 1.8 Slow
            int i = humanoidArm == HumanoidArm.RIGHT ? 1 : -1;

            // Revert to 1.8 pivot
            poseStack.translate(i * 0.56F, -0.52F, -0.71999997F);
            poseStack.translate(i * -0.14142136f, 0.08f, 0.14142136f);

            // Swing math
            float f = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
            float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);

            poseStack.mulPose(Axis.YP.rotationDegrees(i * -(60.0F * g)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(i * (20.0F * f)));
            poseStack.mulPose(Axis.XP.rotationDegrees(-(80.0F * g)));

            poseStack.mulPose(Axis.XP.rotationDegrees(-102.25f));
            poseStack.mulPose(Axis.YP.rotationDegrees(i * 13.365f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(i * 78.05f));

            applyCustomTransforms(poseStack);
            ci.cancel();
        } else if (anim == 4) { // Pitch
            int i = humanoidArm == HumanoidArm.RIGHT ? 1 : -1;

            // Standard item arm position
            poseStack.translate(i * 0.56F, -0.52F, -0.71999997F);
            poseStack.translate(i * -0.14142136f, 0.08f, 0.14142136f);

            // Smooth pitch interpolation: 5 degrees -> -100 degrees using eased swing progress
            float eased = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
            float pitchDegrees = Mth.lerp(eased, 5.0F, -100.0F);

            poseStack.mulPose(Axis.XP.rotationDegrees(pitchDegrees));

            // Standard rest orientation
            poseStack.mulPose(Axis.XP.rotationDegrees(-102.25f));
            poseStack.mulPose(Axis.YP.rotationDegrees(i * 13.365f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(i * 78.05f));

            applyCustomTransforms(poseStack);
            ci.cancel();
        }
    }

    @Inject(method = "applyItemArmTransform", at = @At("RETURN"))
    private void onApplyItemArmTransformReturn(PoseStack poseStack, HumanoidArm humanoidArm, float swingProgress, CallbackInfo ci) {
        if (Vertex.config() == null || Vertex.config().gui == null) return;

        int anim = Vertex.config().gui.swingAnimation;
        // For Normal (0) and Slow (2), we don't cancel HEAD, so apply transforms at RETURN
        if (anim == 0 || anim == 2) {
            applyCustomTransforms(poseStack);
        }
    }

    private void applyCustomTransforms(PoseStack poseStack) {
        float scale = Vertex.config().gui.itemScale;
        float posX = Vertex.config().gui.itemPosX;
        float posY = Vertex.config().gui.itemPosY;
        float posZ = Vertex.config().gui.itemPosZ;

        if (posX != 0.0f || posY != 0.0f || posZ != 0.0f) {
            poseStack.translate(posX, posY, posZ);
        }
        if (scale != 1.0f) {
            poseStack.scale(scale, scale, scale);
        }
    }
}
