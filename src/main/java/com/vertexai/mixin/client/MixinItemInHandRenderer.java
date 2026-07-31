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
        if (Vertex.config() == null || Vertex.config().animations == null) return;

        int anim = Vertex.config().animations.swingAnimation; // 0=Normal, 1=Slow, 2=Block Hit

        // Apply custom scale and translations FIRST so they are in global (screen) space
        float scale = Vertex.config().animations.itemScale;
        float posX = Vertex.config().animations.itemPosX;
        float posY = Vertex.config().animations.itemPosY;
        float posZ = Vertex.config().animations.itemPosZ;

        if (posX != 0.0f || posY != 0.0f || posZ != 0.0f) {
            poseStack.translate(posX, posY, posZ);
        }
        if (scale != 1.0f) {
            poseStack.scale(scale, scale, scale);
        }

        if (anim == 2) { // Block Hit
            int i = humanoidArm == HumanoidArm.RIGHT ? 1 : -1;

            poseStack.translate(i * 0.56F, -0.52F, -0.71999997F);
            if (humanoidArm == HumanoidArm.LEFT) {
                poseStack.translate(0.0F, 0.0F, -0.15F);
            }
            poseStack.translate(i * -0.14142136f, 0.08f, 0.14142136f);

            // Static rest orientation
            poseStack.mulPose(Axis.XP.rotationDegrees(-102.25f));
            poseStack.mulPose(Axis.YP.rotationDegrees(i * 13.365f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(i * 78.05f));

            // Smoothly move into the 1.8 block position during the swing
            float eased = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
            poseStack.translate(i * -0.5F * eased, 0.2F * eased, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(i * 30.0F * eased));
            poseStack.mulPose(Axis.XP.rotationDegrees(-80.0F * eased));
            poseStack.mulPose(Axis.YP.rotationDegrees(i * 60.0F * eased));

            ci.cancel();
        }
        // For Normal (0) and Slow (1), we don't cancel, so vanilla does the rest.
        // We already applied the global translations/scales above.
    }
}
