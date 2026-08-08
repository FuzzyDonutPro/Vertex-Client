package com.vertexai.macro.features.mining.BlockMiner.states;

import com.vertexai.Vertex;
import com.vertexai.macro.features.mining.BlockMiner.BlockMiner;
import com.vertexai.handler.RotationHandler;
import com.vertexai.util.BlockUtil;
import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.PlayerUtil;
import com.vertexai.util.helper.Clock;
import com.vertexai.util.helper.RotationConfiguration;
import com.vertexai.util.helper.Target;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

/**
 * BreakingState
 * <p>
 * Responsible for driving block destruction.
 * Sends START_DESTROY_BLOCK on entry and ServerboundSwingPacket every tick while maintaining target alignment.
 * Detects server block change (AIR, BEDROCK, or new block state) to complete mining cycle.
 */
public class BreakingState implements BlockMinerState {

    private static final double MAX_MINE_DISTANCE = 4.2;
    private static final int LOOK_AWAY_THRESHOLD_MS = 500;
    private static final int TIMEOUT_TICKS = 300; // 15 seconds

    private final Minecraft mc = Minecraft.getInstance();

    private Clock lookAwayTimer;
    private boolean wasLookingAway = false;
    private int breakAttemptTicks = 0;
    private boolean hasSentStartPacket = false;
    private Vec3 targetPoint;

    @Override
    public void onStart(BlockMiner miner) {
        log("Entering BreakingState");
        breakAttemptTicks = 0;
        hasSentStartPacket = false;
        lookAwayTimer = new Clock();
        wasLookingAway = false;

        BlockPos targetPos = miner.getTargetBlockPos();
        if (targetPos == null) return;

        Direction miningDirection = miner.getMiningDirection();
        if (miningDirection == null) {
            miningDirection = BlockUtil.getClosestVisibleSide(targetPos);
            miner.setMiningDirection(miningDirection);
        }

        KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);

        targetPoint = miner.getTargetPoint();
        if (targetPoint != null && !RotationHandler.getInstance().isEnabled()) {
            RotationHandler.getInstance().queueRotation(
                    new RotationConfiguration(
                            new Target(targetPoint),
                            200,
                            null
                    ).followTarget(true)
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

        Direction miningDirection = miner.getMiningDirection();
        if (miningDirection == null) {
            miningDirection = BlockUtil.getClosestVisibleSide(targetPos);
            miner.setMiningDirection(miningDirection);
        }

        // Reset attack cooldown every tick
        ((com.vertexai.mixin.client.MinecraftAccessor) mc).setAttackCooldown(0);

        // Keep keyAttack false to prevent vanilla startAttack() raycast race
        KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);

        // Precision particle tracking
        if (miner.getTargetParticlePos() != null) {
            RotationHandler.getInstance().easeTo(
                    new RotationConfiguration(new Target(miner.getTargetParticlePos()), 800, null).followTarget(true)
            );
            miner.setTargetParticlePos(null);
        }

        // Distance & reach check
        Vec3 pointToTarget = this.targetPoint != null ? this.targetPoint : Vec3.atCenterOf(targetPos);
        double distance = PlayerUtil.getPlayerEyePos().distanceTo(pointToTarget);

        if (distance > MAX_MINE_DISTANCE) {
            if (Vertex.config().miningMacro.allowPathfinder) {
                if (!com.vertexai.macro.features.navigation.Pathfinder.getInstance().isRunning()) {
                    BlockPos walkableGoal = BlockUtil.getWalkableBlocksAround(targetPos, 2)
                            .stream()
                            .min(Comparator.comparingDouble(b -> b.distSqr(PlayerUtil.getBlockStandingOn())))
                            .orElse(targetPos);
                    com.vertexai.macro.features.navigation.Pathfinder.getInstance().stopAndRequeue(walkableGoal);
                    com.vertexai.macro.features.navigation.Pathfinder.getInstance().start();
                }
            }
            return this;
        } else {
            if (com.vertexai.macro.features.navigation.Pathfinder.getInstance().isRunning()) {
                com.vertexai.macro.features.navigation.Pathfinder.getInstance().stop("Reached mining distance");
            }
        }

        // Shift/sneak handling
        if (!com.vertexai.macro.features.navigation.Pathfinder.getInstance().isRunning()) {
            boolean shouldSneak = Vertex.config().general.sneakWhileMining;
            if (Vertex.config().miningMacro.allowPathfinder && Vertex.config().miningMacro.pathfinderMode == 0) {
                shouldSneak = true;
            }
            KeyBindUtil.setKeyBindState(mc.options.keyShift, shouldSneak);
            KeyBindUtil.setKeyBindState(mc.options.keyUp, false);
        }

        // Reset attack cooldown & destroy delay every tick
        ((com.vertexai.mixin.client.MinecraftAccessor) mc).setAttackCooldown(0);
        ((com.vertexai.mixin.MultiPlayerGameModeAccessor) mc.gameMode).setDestroyDelay(0);

        // Send START_DESTROY_BLOCK once on first tick within reach
        if (!hasSentStartPacket) {
            log("Firing START_DESTROY_BLOCK on " + targetPos + " face " + miningDirection);
            mc.gameMode.startDestroyBlock(targetPos, miningDirection);
            hasSentStartPacket = true;
        } else {
            mc.gameMode.continueDestroyBlock(targetPos, miningDirection);
            if (mc.player != null && mc.getConnection() != null) {
                mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundSwingPacket(
                        net.minecraft.world.InteractionHand.MAIN_HAND
                ));
            }
        }

        // Failsafe 1: Max break duration timeout
        if (++this.breakAttemptTicks > TIMEOUT_TICKS) {
            logError("Mining timeout exceeded (" + TIMEOUT_TICKS + " ticks), restarting state machine");
            if (mc.gameMode != null) {
                mc.gameMode.stopDestroyBlock();
            }
            return new StartingState();
        }

        // Failsafe 2: Look-away threshold (500ms)
        mc.gameRenderer.pick(1.0f);
        boolean isLookingAtTarget = (mc.hitResult instanceof BlockHitResult bhr && targetPos.equals(bhr.getBlockPos()));
        if (!isLookingAtTarget) {
            if (!wasLookingAway) {
                lookAwayTimer.schedule(LOOK_AWAY_THRESHOLD_MS);
                wasLookingAway = true;
            } else if (lookAwayTimer.passed()) {
                log("Camera drifted off target for >" + LOOK_AWAY_THRESHOLD_MS + "ms, selecting new block");
                if (mc.gameMode != null) {
                    mc.gameMode.stopDestroyBlock();
                }
                return new StartingState();
            }
        } else {
            wasLookingAway = false;
        }

        // Server Block Change Detection: Block turned to AIR, BEDROCK, changed block type, or blockChange event fired
        Block currentBlock = mc.level.getBlockState(targetPos).getBlock();
        if (currentBlock == net.minecraft.world.level.block.Blocks.BEDROCK ||
            currentBlock == net.minecraft.world.level.block.Blocks.AIR ||
            !currentBlock.equals(miner.getTargetBlockType()) ||
            miner.isBlockChanged()) {
            log("Block successfully broken at " + targetPos + "!");
            if (mc.gameMode != null) {
                mc.gameMode.stopDestroyBlock();
            }
            return new StartingState();
        }

        return this;
    }

    @Override
    public void onEnd(BlockMiner miner) {
        RotationHandler.getInstance().stop();
        KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);
        if (mc.gameMode != null) {
            mc.gameMode.stopDestroyBlock();
        }
        log("Exiting BreakingState");
    }
}
