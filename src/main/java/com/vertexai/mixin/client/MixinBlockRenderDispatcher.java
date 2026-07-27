package com.vertexai.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vertexai.Vertex;
import com.vertexai.macro.MacroManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderDispatcher.class)
public class MixinBlockRenderDispatcher {

    @Inject(method = "renderBatched", at = @At("HEAD"), cancellable = true)
    private void Vertex$onRenderBatched(BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack poseStack, VertexConsumer buffer, boolean checkSides, RandomSource random, CallbackInfo ci) {
        var config = Vertex.config();
        if (config != null && config.misc.noRenderMode && MacroManager.getInstance().isRunning()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && pos != null) {
                int dx = Math.abs(pos.getX() - mc.player.getBlockX());
                int dz = Math.abs(pos.getZ() - mc.player.getBlockZ());
                int dy = Math.abs(pos.getY() - mc.player.getBlockY());

                // Only render terrain blocks within 5x5 area around player (radius 2 horizontally, 3 vertically)
                if (dx > 2 || dz > 2 || dy > 3) {
                    ci.cancel();
                }
            }
        }
    }
}
