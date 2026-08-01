package com.vertexai.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.vertexai.Vertex;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import com.mojang.blaze3d.vertex.VertexConsumer;

@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer {

    @Inject(method = "applyItemArmTransform", at = @At("HEAD"), cancellable = true)
    private void onApplyItemArmTransform(PoseStack poseStack, HumanoidArm humanoidArm, float swingProgress, CallbackInfo ci) {
        if (Vertex.config() == null || Vertex.config().animations == null) return;

        // Apply custom global rotations, scale, and translations FIRST so they affect the entire arm
        float pitch = Vertex.config().animations.itemPitch;
        float yaw = Vertex.config().animations.itemYaw;
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
        if (pitch != 0.0f) {
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        }
        if (yaw != 0.0f) {
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        }
        // Vanilla does the rest!
    }
}
