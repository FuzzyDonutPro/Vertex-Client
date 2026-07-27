package com.vertexai.feature.impl;

import lombok.Getter;
import com.vertexai.Vertex;
import com.vertexai.feature.AbstractFeature;
import com.vertexai.mixin.MultiPlayerGameModeAccessor;
import net.minecraft.core.BlockPos;

/**
 * FastBreak — Removes client-side block breaking delay with configurable 1.0x to 2.0x tick speed slider
 * and anti-ghost block desync safeguards for Gemstone & Titanium mining.
 */
public class FastBreak extends AbstractFeature {

    @Getter
    private static final FastBreak instance = new FastBreak();

    @Override
    public String getName() {
        return "Fast Break";
    }

    @Override
    public void onTick() {
        if (!isRunning() || mc.gameMode == null || mc.level == null) return;

        float speedMultiplier = 1.5f;
        if (Vertex.config() != null && Vertex.config().misc != null) {
            speedMultiplier = Vertex.config().misc.fastBreakSpeed;
        }

        // Calculate target delay ticks based on speed multiplier (1.0x = normal 5 ticks, 2.0x = 0 ticks)
        int targetDelay = (int) Math.max(0, Math.round(5.0f * (2.0f - speedMultiplier)));

        // Anti-Ghost Block Safeguard: Cap min delay to 1 tick on high-hardness gemstone blocks
        if (mc.hitResult != null && mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
            BlockPos targetPos = blockHit.getBlockPos();
            String blockName = mc.level.getBlockState(targetPos).getBlock().toString().toLowerCase();
            if (blockName.contains("gemstone") || blockName.contains("prismarine") || blockName.contains("obsidian")) {
                targetDelay = Math.max(1, targetDelay);
            }
        }

        ((MultiPlayerGameModeAccessor) mc.gameMode).setDestroyDelay(targetDelay);
    }

    public boolean isRunning() {
        return this.enabled && mc.player != null && mc.gameMode != null;
    }
}
