package com.vertexai.macro.features.mining.BlockMiner.states;

import com.vertexai.Vertex;
import com.vertexai.macro.features.mining.BlockMiner.BlockMiner;
import com.vertexai.handler.RotationHandler;
import com.vertexai.util.AngleUtil;
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
import java.util.List;
import java.util.Random;

/**
 * BreakingState
 * <p>
 * Step 4 of 4-step mining lifecycle: Legit Mine.
 * Combines VeinForge precision targeting, look-away failsafe, and 1.21.1 packet-safe block destruction.
 */
public class BreakingState implements BlockMinerState {

    private static final double MIN_WALK_DISTANCE = 0.2;
    private static final double MAX_MINE_DISTANCE = 3.0;
    private static final int LOOK_AWAY_THRESHOLD_MS = 500;

    private final Minecraft mc = Minecraft.getInstance();
    private final Random random = new Random();

    private Clock lookAwayTimer;
    private boolean wasLookingAway = false;
    private int breakAttemptTime;
    private boolean hasStartedDestroy;
    private Vec3 targetPoint;

    @Override
    public void onStart(BlockMiner miner) {
        log("Entering Breaking State");
        breakAttemptTime = 0;
        hasStartedDestroy = false;
        lookAwayTimer = new Clock();
        wasLookingAway = false;

        BlockPos targetPos = miner.getTargetBlockPos();
        if (targetPos == null) return;

        Direction miningDirection = miner.getMiningDirection();
        if (miningDirection == null) {
            miningDirection = BlockUtil.getClosestVisibleSide(targetPos);
            miner.setMiningDirection(miningDirection);
        }

        // Ensure keyAttack stays false so vanilla Minecraft.tick() startAttack() does not race
        KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);

        // Setup initial rotation to look at the block
        initializeRotation(miner);
    }

    @Override
    public BlockMinerState onTick(BlockMiner miner) {
        if (miner.getTargetBlockPos() == null || mc.level == null || mc.gameMode == null) {
            return new StartingState();
        }

        BlockPos targetPos = miner.getTargetBlockPos();
        Direction miningDirection = miner.getMiningDirection();
        if (miningDirection == null) {
            miningDirection = BlockUtil.getClosestVisibleSide(targetPos);
            miner.setMiningDirection(miningDirection);
        }

        // Reset attack cooldown every tick
        ((com.vertexai.mixin.client.MinecraftAccessor) mc).setAttackCooldown(0);

        // Ensure keyAttack stays false to prevent vanilla startAttack() racing
        KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);

        // Handle precision particle mining if precision mode triggered a crit particle
        if (miner.getTargetParticlePos() != null) {
            RotationHandler.getInstance().easeTo(
                    new RotationConfiguration(new Target(miner.getTargetParticlePos()), 800, null).followTarget(true)
            );
            miner.setTargetParticlePos(null);
        }

        // Calculate eye distance to target point
        Vec3 pointToTarget = this.targetPoint != null ? this.targetPoint : Vec3.atCenterOf(targetPos);
        double miningDistance = PlayerUtil.getPlayerEyePos().distanceTo(pointToTarget);

        // 1. Gate dig calls on reach: Player must be within MAX_MINE_DISTANCE (3.0 blocks)
        if (miningDistance > MAX_MINE_DISTANCE) {
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

        // Handle shift/sneak requirements
        if (!com.vertexai.macro.features.navigation.Pathfinder.getInstance().isRunning()) {
            boolean shouldSneak = Vertex.config().general.sneakWhileMining;
            if (Vertex.config().miningMacro.allowPathfinder && Vertex.config().miningMacro.pathfinderMode == 0) {
                shouldSneak = true;
            }
            KeyBindUtil.setKeyBindState(mc.options.keyShift, shouldSneak);
            KeyBindUtil.setKeyBindState(mc.options.keyUp, false);
        }

        // 2. Gate startDestroyBlock on rotation convergence & reach
        if (!hasStartedDestroy) {
            mc.gameRenderer.pick(1.0f);
            boolean confirmedTarget = (mc.hitResult instanceof BlockHitResult bhr && bhr.getBlockPos().equals(targetPos));
            boolean rotationFinished = !RotationHandler.getInstance().isEnabled();

            if (confirmedTarget || rotationFinished || breakAttemptTime >= 5) {
                log("Rotation aligned (" + String.format("%.2f", miningDistance) + "m). Firing startDestroyBlock on " + targetPos + " face " + miningDirection);
                mc.gameMode.startDestroyBlock(targetPos, miningDirection);
                hasStartedDestroy = true;
            }
        } else {
            // Drive block destruction manually once started
            mc.gameMode.continueDestroyBlock(targetPos, miningDirection);

            // Send explicit swing packet to server & trigger local animation
            if (mc.player != null && mc.getConnection() != null) {
                mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundSwingPacket(
                        net.minecraft.world.InteractionHand.MAIN_HAND
                ));
            }
        }

        // Safety mechanism 1: if we've been trying to break for too long (30s), reset
        if (++this.breakAttemptTime > 600) {
            logError("Stuck while mining, returning to StartingState");
            if (mc.gameMode != null) {
                mc.gameMode.stopDestroyBlock();
            }
            return new StartingState();
        }

        // Safety mechanism 2: look-away threshold check (500ms window)
        mc.gameRenderer.pick(1.0f);
        boolean isLookingAtTarget = (mc.hitResult instanceof BlockHitResult bhr && targetPos.equals(bhr.getBlockPos()));
        if (!isLookingAtTarget) {
            if (!wasLookingAway) {
                lookAwayTimer.schedule(LOOK_AWAY_THRESHOLD_MS);
                wasLookingAway = true;
            } else if (lookAwayTimer.passed()) {
                log("Player looked away from target block for too long, choosing new block");
                if (mc.gameMode != null) {
                    mc.gameMode.stopDestroyBlock();
                }
                return new StartingState();
            }
        } else {
            wasLookingAway = false;
        }

        // Detect block broken: block turns to Bedrock, Air, changes block type, or blockChanged event fired
        Block currentBlock = mc.level.getBlockState(targetPos).getBlock();
        if (currentBlock == net.minecraft.world.level.block.Blocks.BEDROCK ||
            currentBlock == net.minecraft.world.level.block.Blocks.AIR ||
            !currentBlock.equals(miner.getTargetBlockType()) ||
            miner.isBlockChanged()) {
            log("Block successfully broken at " + targetPos);
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
        log("Exiting Breaking State");
    }

    private void initializeRotation(BlockMiner miner) {
        List<Vec3> points = BlockUtil.bestPointsOnBestSide(miner.getTargetBlockPos());

        if (points.isEmpty()) {
            logError("Cannot find points to look at. Returning to STARTING state.");
            miner.setError(BlockMiner.BlockMinerError.NO_POINTS_FOUND);
            miner.stop();
            return;
        }

        this.targetPoint = points.get(0);
        miner.setTargetPoint(this.targetPoint);

        RotationHandler.getInstance().stop();
        RotationHandler.getInstance().queueRotation(
                new RotationConfiguration(
                        new Target(targetPoint),
                        Vertex.config().getRandomRotationTime(),
                        null
                ).followTarget(true)
        );

        if (random.nextBoolean() && Vertex.config().general.randomizedRotations) {
            int halfwayMark = points.size() / 2;
            if (halfwayMark > 0) {
                this.targetPoint = points.get(random.nextInt(halfwayMark) + halfwayMark - 1);
                miner.setTargetPoint(this.targetPoint);

                RotationHandler.getInstance().queueRotation(
                        new RotationConfiguration(
                                new Target(targetPoint),
                                Vertex.config().getRandomRotationTime() * 2L,
                                null
                        ).followTarget(true)
                );
            }
        }

        RotationHandler.getInstance().start();
    }
}
