package com.vertexai.macro.features.mining.BlockMiner.states;

import com.vertexai.macro.features.mining.BlockMiner.BlockMiner;
import com.vertexai.handler.RotationHandler;
import com.vertexai.util.BlockUtil;
import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.helper.RotationConfiguration;
import com.vertexai.util.helper.Target;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * BreakingState
 * <p>
 * Clean state from scratch:
 * 1. Locks camera smoothly onto target block.
 * 2. Holds keyAttack = true while zeroing destroyDelay.
 * 3. Monitors block status until it turns to AIR/BEDROCK or changes state, then transitions back to StartingState.
 */
public class BreakingState implements BlockMinerState {

    private final Minecraft mc = Minecraft.getInstance();
    private int ticksMining = 0;
    private Vec3 targetPoint;

    @Override
    public void onStart(BlockMiner miner) {
        log("Entering BreakingState");
        ticksMining = 0;

        BlockPos targetPos = miner.getTargetBlockPos();
        if (targetPos == null) return;

        Direction miningDirection = miner.getMiningDirection();
        if (miningDirection == null) {
            miningDirection = BlockUtil.getClosestVisibleSide(targetPos);
            miner.setMiningDirection(miningDirection);
        }

        targetPoint = miner.getTargetPoint();
        if (targetPoint == null) {
            targetPoint = Vec3.atCenterOf(targetPos);
            miner.setTargetPoint(targetPoint);
        }

        // Keep camera locked on target block face
        if (!RotationHandler.getInstance().isEnabled()) {
            RotationHandler.getInstance().queueRotation(
                    new RotationConfiguration(new Target(targetPoint), 150, null).followTarget(true)
            );
            RotationHandler.getInstance().start();
        }

        // Start attack on target pos
        if (mc.gameMode != null && miningDirection != null) {
            mc.gameMode.startDestroyBlock(targetPos, miningDirection);
        }
    }

    @Override
    public BlockMinerState onTick(BlockMiner miner) {
        BlockPos targetPos = miner.getTargetBlockPos();
        if (targetPos == null || mc.level == null || mc.gameMode == null) {
            return new StartingState();
        }

        ticksMining++;

        // Reset attack cooldown & destroy delay every tick for instant re-attack
        ((com.vertexai.mixin.client.MinecraftAccessor) mc).setAttackCooldown(0);
        ((com.vertexai.mixin.MultiPlayerGameModeAccessor) mc.gameMode).setDestroyDelay(0);

        // Hold left-click attack key
        KeyBindUtil.setKeyBindState(mc.options.keyAttack, true);

        // Precision particle tracking
        if (miner.getTargetParticlePos() != null) {
            RotationHandler.getInstance().easeTo(
                    new RotationConfiguration(new Target(miner.getTargetParticlePos()), 600, null).followTarget(true)
            );
            miner.setTargetParticlePos(null);
        }

        // Continuously drive block destruction
        Direction side = miner.getMiningDirection();
        if (side == null) side = Direction.UP;
        mc.gameMode.continueDestroyBlock(targetPos, side);

        // Safety timeout (15 seconds max per block)
        if (ticksMining > 300) {
            logError("Mining timeout reached (15s), moving to next block");
            return new StartingState();
        }

        // Check if block has been broken / changed
        Block currentBlock = mc.level.getBlockState(targetPos).getBlock();
        boolean isBroken = (currentBlock == Blocks.AIR ||
                            currentBlock == Blocks.BEDROCK ||
                            !currentBlock.equals(miner.getTargetBlockType()) ||
                            miner.isBlockChanged());

        if (isBroken) {
            log("Block successfully broken at " + targetPos);
            return new StartingState();
        }

        return this;
    }

    @Override
    public void onEnd(BlockMiner miner) {
        log("Exiting BreakingState");
        KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);
        if (mc.gameMode != null) {
            mc.gameMode.stopDestroyBlock();
        }
    }
}
