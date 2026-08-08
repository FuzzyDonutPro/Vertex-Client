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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * BreakingState
 * <p>
 * Ultra-strict state:
 * Verifies crosshair is ACTUALLY looking at targetPos before firing keyAttack or dig packets.
 */
public class BreakingState implements BlockMinerState {

    private final Minecraft mc = Minecraft.getInstance();
    private int ticksMining = 0;
    private boolean hasStartedDestroy = false;
    private Vec3 targetPoint;

    @Override
    public void onStart(BlockMiner miner) {
        log("Entering BreakingState");
        ticksMining = 0;
        hasStartedDestroy = false;

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
    }

    @Override
    public BlockMinerState onTick(BlockMiner miner) {
        BlockPos targetPos = miner.getTargetBlockPos();
        if (targetPos == null || mc.level == null || mc.gameMode == null) {
            return new StartingState();
        }

        ticksMining++;

        // Reset attack cooldown & destroy delay every tick
        ((com.vertexai.mixin.client.MinecraftAccessor) mc).setAttackCooldown(0);
        ((com.vertexai.mixin.MultiPlayerGameModeAccessor) mc.gameMode).setDestroyDelay(0);

        // Precision particle tracking
        if (miner.getTargetParticlePos() != null) {
            RotationHandler.getInstance().easeTo(
                    new RotationConfiguration(new Target(miner.getTargetParticlePos()), 600, null).followTarget(true)
            );
            miner.setTargetParticlePos(null);
        }

        // STRICT CHECK: Raycast pick to verify crosshair is ACTUALLY looking at target block
        mc.gameRenderer.pick(1.0f);
        boolean isLookingAtTarget = (mc.hitResult instanceof BlockHitResult bhr
                && targetPos.equals(bhr.getBlockPos()));

        if (!isLookingAtTarget) {
            // Pause mining and release attack key until camera finishes aligning onto target block
            KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);

            if (!RotationHandler.getInstance().isEnabled() && targetPoint != null) {
                RotationHandler.getInstance().queueRotation(
                        new RotationConfiguration(new Target(targetPoint), 150, null).followTarget(true)
                );
                RotationHandler.getInstance().start();
            }

            // If we've been trying to align for > 3 seconds (60 ticks), restart state machine to select new target
            if (ticksMining > 60 && !hasStartedDestroy) {
                logError("Failed to align crosshair on " + targetPos + " within 3s, picking new block");
                return new StartingState();
            }
            return this;
        }

        // Crosshair is 100% CONFIRMED looking at target block! Enable attack key & drive mining
        KeyBindUtil.setKeyBindState(mc.options.keyAttack, true);

        Direction side = miner.getMiningDirection();
        if (side == null) side = Direction.UP;

        if (!hasStartedDestroy) {
            log("Confirmed looking at " + targetPos + ". Firing START_DESTROY_BLOCK on face " + side);
            mc.gameMode.startDestroyBlock(targetPos, side);
            hasStartedDestroy = true;
        }

        mc.gameMode.continueDestroyBlock(targetPos, side);
        if (mc.player != null && mc.getConnection() != null) {
            mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundSwingPacket(
                    net.minecraft.world.InteractionHand.MAIN_HAND
            ));
        }

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
