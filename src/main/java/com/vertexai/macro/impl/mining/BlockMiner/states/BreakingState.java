package com.vertexai.macro.impl.mining.BlockMiner.states;

import com.vertexai.macro.impl.mining.BlockMiner.BlockMiner;
import com.vertexai.handler.RotationHandler;
import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.helper.Clock;
import com.vertexai.util.helper.RotationConfiguration;
import com.vertexai.util.helper.Target;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/**
 * BreakingState — Adapted directly from ForagingMacro's 100% reliable breaking engine.
 * Rotates smoothly to target block, holds left click via KeyBindUtil, and monitors block state.
 */
public class BreakingState implements BlockMinerState {

    private final Minecraft mc = Minecraft.getInstance();
    private final Clock breakDelay = new Clock();
    private final Clock postBreakTimer = new Clock();
    private boolean blockBroken = false;

    @Override
    public void onStart(BlockMiner miner) {
        log("Aiming and breaking target block...");
        breakDelay.reset();
        postBreakTimer.reset();
        blockBroken = false;

        if (miner.getTargetBlockPos() != null) {
            Vec3 centerVec = Vec3.atCenterOf(miner.getTargetBlockPos());
            RotationHandler.getInstance().easeTo(new RotationConfiguration(
                    new Target(centerVec),
                    80L,
                    null
            ));
        }
    }

    @Override
    public BlockMinerState onTick(BlockMiner miner) {
        if (mc.player == null || mc.level == null || miner.getTargetBlockPos() == null) {
            return new StartingState();
        }

        BlockPos targetPos = miner.getTargetBlockPos();

        // Check if target block has been broken (turned to air or changed block type)
        Block currentBlock = mc.level.getBlockState(targetPos).getBlock();
        if (!currentBlock.equals(miner.getTargetBlockType())) {
            if (!blockBroken) {
                blockBroken = true;
                postBreakTimer.schedule(50L);
            }
            if (postBreakTimer.passed()) {
                log("Target block broken!");
                return new StartingState();
            }
            KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);
            return this;
        }

        // Verify reach limit (4.5 blocks max)
        Vec3 centerVec = Vec3.atCenterOf(targetPos);
        double distanceSq = mc.player.getEyePosition().distanceToSqr(centerVec);
        if (distanceSq > 20.25) { // Out of reach
            log("Target block out of reach, choosing new block...");
            return new StartingState();
        }

        // Check if crosshair is aimed directly at the block
        boolean crosshairOnTarget = mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult blockHit
                && blockHit.getBlockPos().equals(targetPos);

        if (!crosshairOnTarget) {
            if (!RotationHandler.getInstance().isEnabled()) {
                RotationHandler.getInstance().easeTo(new RotationConfiguration(
                        new Target(centerVec),
                        80L,
                        null
                ));
            }
        }

        // Precision particle mining rotation if available
        if (miner.getTargetParticlePos() != null) {
            RotationHandler.getInstance().easeTo(new RotationConfiguration(
                    new Target(miner.getTargetParticlePos()),
                    80L,
                    null
            ).followTarget(true));
            miner.setTargetParticlePos(null);
        }

        // Hold left-click (matching ForagingMacro's 100% reliable mining implementation)
        KeyBindUtil.setKeyBindState(mc.options.keyAttack, true);

        if (com.vertexai.Vertex.config().general.sneakWhileMining) {
            KeyBindUtil.setKeyBindState(mc.options.keyShift, true);
        }

        // Fail-safe timeout (5 seconds max per block)
        if (!breakDelay.isScheduled()) {
            breakDelay.schedule(5000L);
        } else if (breakDelay.passed()) {
            log("Block taking too long to break, choosing new block...");
            return new StartingState();
        }

        return this;
    }

    @Override
    public void onEnd(BlockMiner miner) {
        KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);
        RotationHandler.getInstance().stop();
    }

    @Override
    public void log(String message) {
        com.vertexai.util.Logger.sendLog("[BlockMiner] " + message);
    }
}
