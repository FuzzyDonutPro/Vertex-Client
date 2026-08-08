package com.vertexai.macro.features.mining.BlockMiner.states;

import com.vertexai.macro.features.mining.BlockMiner.BlockMiner;
import com.vertexai.util.BlockUtil;
import com.vertexai.util.KeyBindUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;

/**
 * ArriveState
 * <p>
 * Step 3 of 4-step mining lifecycle: Arrive Verification.
 * Verifies that the crosshair raycast is confirmed on target block face before starting destruction.
 * Captures and locks the targeted Direction face ONCE.
 */
public class ArriveState implements BlockMinerState {

    private final Minecraft mc = Minecraft.getInstance();
    private int verifyTicks = 0;

    @Override
    public void onStart(BlockMiner miner) {
        log("Entering Arrive State");
        verifyTicks = 0;
        KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);

        BlockPos targetPos = miner.getTargetBlockPos();
        if (targetPos == null) return;

        // Pick raycast hit result
        mc.gameRenderer.pick(1.0f);

        // Lock targeted Direction face ONCE
        BlockHitResult bhr = (mc.hitResult instanceof BlockHitResult b) ? b : null;
        Direction face = (bhr != null && bhr.getBlockPos().equals(targetPos))
                ? bhr.getDirection()
                : BlockUtil.getClosestVisibleSide(targetPos);

        if (face == null) face = Direction.UP;
        miner.setMiningDirection(face);
        log("Arrival verified. Direction face locked to: " + face);
    }

    @Override
    public BlockMinerState onTick(BlockMiner miner) {
        if (miner.getTargetBlockPos() == null || mc.level == null) {
            return new StartingState();
        }

        KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);
        verifyTicks++;

        mc.gameRenderer.pick(1.0f);
        boolean confirmedTarget = (mc.hitResult instanceof BlockHitResult bhr && bhr.getBlockPos().equals(miner.getTargetBlockPos()));

        // If raycast confirmed on target block (or verified max 3 ticks), transition to BreakingState
        if (confirmedTarget || verifyTicks >= 3) {
            log("Arrive verification confirmed, transitioning to BreakingState");
            return new BreakingState();
        }

        return this;
    }

    @Override
    public void onEnd(BlockMiner miner) {
        log("Exiting Arrive State");
    }
}
