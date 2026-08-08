package com.vertexai.feature.impl.BlockMiner.states;

import com.vertexai.Vertex;
import com.vertexai.feature.impl.BlockMiner.BlockMiner;
import com.vertexai.handler.RotationHandler;
import com.vertexai.util.BlockUtil;
import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.PlayerUtil;
import com.vertexai.util.helper.RotationConfiguration;
import com.vertexai.util.helper.Target;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * BreakingState
 * <p>
 * Completely rewritten legit player mining implementation.
 * 1. Target Selection -> 2. Smooth Aim (rotate camera) -> 3. Arrive & Lock -> 4. Mine (hold left click until broken).
 */
public class BreakingState implements BlockMinerState {

    private static final double MAX_MINE_DISTANCE = 3.5;
    private final Minecraft mc = Minecraft.getInstance();

    private int breakAttemptTime;
    private int miningTime;
    private Vec3 targetPoint;
    private boolean hasStartedMining = false;
    private boolean hasSentStartPacket = false;
    private net.minecraft.core.Direction miningDirection = null;

    @Override
    public void onStart(BlockMiner miner) {
        log("Entering Breaking State");
        breakAttemptTime = 0;
        hasStartedMining = false;
        hasSentStartPacket = false;
        miningDirection = null;

        miningTime = BlockUtil.getMiningTime(
                mc.level.getBlockState(miner.getTargetBlockPos()),
                miner.getMiningSpeed()
        );

        // Setup smooth camera rotation toward the center of the best visible block side
        RotationHandler.getInstance().stop();
        initializeRotation(miner);
    }

    @Override
    public BlockMinerState onTick(BlockMiner miner) {
        if (miner.getTargetBlockPos() == null || mc.level == null) {
            return new StartingState();
        }

        // Handle key presses for mining (aim -> arrive -> hold left click)
        handleKeybinds(miner);

        // Handle Pathfinder navigation if target block is out of mining reach
        double miningDistance = this.targetPoint != null ? PlayerUtil.getPlayerEyePos().distanceTo(this.targetPoint) : 999;
        if (miningDistance > MAX_MINE_DISTANCE && Vertex.config().miningMacro.allowPathfinder) {
            if (!com.vertexai.feature.impl.Pathfinder.getInstance().isRunning()) {
                BlockPos targetPos = miner.getTargetBlockPos();
                BlockPos walkableGoal = BlockUtil.getWalkableBlocksAround(targetPos, 2)
                        .stream()
                        .min(Comparator.comparingDouble(b -> b.distSqr(PlayerUtil.getBlockStandingOn())))
                        .orElse(targetPos);
                com.vertexai.feature.impl.Pathfinder.getInstance().stopAndRequeue(walkableGoal);
                com.vertexai.feature.impl.Pathfinder.getInstance().start();
            }
        } else {
            if (com.vertexai.feature.impl.Pathfinder.getInstance().isRunning()) {
                com.vertexai.feature.impl.Pathfinder.getInstance().stop("Reached mining distance");
            }
        }

        // Safety timeout: if block has been actively mined for far too long, reset
        if (hasStartedMining && ++this.breakAttemptTime > Math.max(200, this.miningTime * 4)) {
            logError("Stuck while mining, return to starting state");
            if (mc.gameMode != null) {
                mc.gameMode.stopDestroyBlock();
            }
            KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);
            return new StartingState();
        }

        // Detect block broken: block turns to Bedrock, Air, or changes from target block type
        Block currentBlock = mc.level.getBlockState(miner.getTargetBlockPos()).getBlock();
        if (currentBlock == net.minecraft.world.level.block.Blocks.BEDROCK ||
            currentBlock == net.minecraft.world.level.block.Blocks.AIR ||
            !currentBlock.equals(miner.getTargetBlockType())) {
            KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);
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
        hasSentStartPacket = false;
        hasStartedMining = false;
        miningDirection = null;
        log("Exiting Breaking State");
    }

    /**
     * Handles key bindings for mining.
     * Operates purely like a legit human player:
     * 1. Turn camera to target (keyAttack released)
     * 2. Once crosshair arrives on block: press and hold keyAttack
     * 3. Continuously drive block destruction with locked direction face until block breaks
     */
    private void handleKeybinds(BlockMiner miner) {
        if (mc.gameMode == null || mc.player == null) return;

        // Reset attack cooldown so mining isn't delayed by weapon miss timers
        ((com.vertexai.mixin.client.MinecraftAccessor) mc).setAttackCooldown(0);

        BlockPos targetPos = miner.getTargetBlockPos();
        if (targetPos == null) return;

        // Pick raycast hit result with current camera angles
        mc.gameRenderer.pick(1.0f);

        boolean arrivedOnTarget = (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bhr && bhr.getBlockPos().equals(targetPos));
        boolean rotationFinished = !RotationHandler.getInstance().isEnabled();

        // 1. AIM & ARRIVE: Wait until camera rotation finishes or crosshair arrives on block before clicking
        if (!hasSentStartPacket) {
            if (!arrivedOnTarget && !rotationFinished) {
                // Camera still turning toward block — keep left click released (legit human behavior)
                KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);
                return;
            }

            // Camera arrived! Lock initial direction face and start destruction
            net.minecraft.world.phys.BlockHitResult bhr = (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult b) ? b : null;
            miningDirection = (bhr != null && bhr.getBlockPos().equals(targetPos)) ? bhr.getDirection() : BlockUtil.getClosestVisibleSide(targetPos);
            if (miningDirection == null) miningDirection = net.minecraft.core.Direction.UP;

            mc.gameMode.startDestroyBlock(targetPos, miningDirection);
            hasSentStartPacket = true;
            hasStartedMining = true;
        }

        // 2. LEGIT HOLD & MINE: Hold left click and continue destruction with locked face
        KeyBindUtil.setKeyBindState(mc.options.keyAttack, true);
        mc.gameMode.continueDestroyBlock(targetPos, miningDirection != null ? miningDirection : net.minecraft.core.Direction.UP);
        mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);

        // Handle shift/sneak requirements
        if (!com.vertexai.feature.impl.Pathfinder.getInstance().isRunning()) {
            boolean shouldSneak = Vertex.config().general.sneakWhileMining;
            if (Vertex.config().miningMacro.allowPathfinder && Vertex.config().miningMacro.pathfinderMode == 0) {
                shouldSneak = true;
            }
            KeyBindUtil.setKeyBindState(mc.options.keyShift, shouldSneak);
            KeyBindUtil.setKeyBindState(mc.options.keyUp, false);
        }
    }

    /**
     * Sets up smooth humanized rotation to look at the target block center.
     */
    private void initializeRotation(BlockMiner miner) {
        List<Vec3> points = BlockUtil.bestPointsOnBestSide(miner.getTargetBlockPos());

        if (points.isEmpty()) {
            logError("Cannot find points to look at. Returning to STARTING state.");
            miner.setError(BlockMiner.BlockMinerError.NO_POINTS_FOUND);
            miner.stop();
            return;
        }

        this.targetPoint = points.get(0);

        RotationHandler.getInstance().stop();
        RotationHandler.getInstance().queueRotation(
                new RotationConfiguration(
                        new Target(targetPoint),
                        Vertex.config().getRandomRotationTime(),
                        null
                )
        );
        RotationHandler.getInstance().start();
    }
}
