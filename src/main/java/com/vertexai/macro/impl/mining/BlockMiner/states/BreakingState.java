package com.vertexai.macro.impl.mining.BlockMiner.states;

import com.vertexai.macro.impl.mining.BlockMiner.BlockMiner;
import com.vertexai.handler.BlockBreakingEngine;
import com.vertexai.util.KeyBindUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * BreakingState — Refactored to use central BlockBreakingEngine.
 */
public class BreakingState implements BlockMinerState {

    private final Minecraft mc = Minecraft.getInstance();

    @Override
    public void onStart(BlockMiner miner) {
        log("Aiming and breaking target block...");
        if (miner.getTargetBlockPos() != null) {
            BlockBreakingEngine.getInstance().breakBlock(miner.getTargetBlockPos());
        }
    }

    @Override
    public BlockMinerState onTick(BlockMiner miner) {
        if (mc.player == null || mc.level == null || miner.getTargetBlockPos() == null) {
            BlockBreakingEngine.getInstance().stopBreaking();
            return new StartingState();
        }

        BlockPos targetPos = miner.getTargetBlockPos();

        // Verify reach limit (strict 3.0 blocks max = 9.0 sq blocks)
        Vec3 centerVec = Vec3.atCenterOf(targetPos);
        if (mc.player.getEyePosition().distanceToSqr(centerVec) > 9.0) {
            log("Target block out of 3.0-block reach, choosing new block...");
            BlockBreakingEngine.getInstance().stopBreaking();
            return new StartingState();
        }

        // Delegate breaking to centralized BlockBreakingEngine
        boolean stillMining = BlockBreakingEngine.getInstance().breakBlock(targetPos);
        if (!stillMining) {
            log("Target block broken!");
            return new StartingState();
        }

        if (com.vertexai.Vertex.config().general.sneakWhileMining) {
            KeyBindUtil.setKeyBindState(mc.options.keyShift, true);
        }

        // Fail-safe timeout (5 seconds max per block)
        if (BlockBreakingEngine.getInstance().getBreakDurationMs() > 5000L) {
            log("Block taking too long to break, choosing new block...");
            BlockBreakingEngine.getInstance().stopBreaking();
            return new StartingState();
        }

        return this;
    }

    @Override
    public void onEnd(BlockMiner miner) {
        BlockBreakingEngine.getInstance().stopBreaking();
    }

    @Override
    public void log(String message) {
        com.vertexai.util.Logger.sendLog("[BlockMiner] " + message);
    }
}
