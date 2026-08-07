package com.vertexai.feature.impl.BlockMiner.states;

import com.vertexai.Vertex;
import com.vertexai.feature.impl.BlockMiner.BlockMiner;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * BreakingState
 * <p>
 * State responsible for breaking the selected target block.
 * Handles player rotation, movement, and mining mechanics.
 * Will attempt to move towards the block if too far away.
 */
public class BreakingState implements BlockMinerState {

    /**
     * Minimum distance required between the player and block to trigger walking behavior.
     */
    private static final double MIN_WALK_DISTANCE = 0.2;
    /**
     * The maximum allowed distance for the player to attempt to mine a block.
     * If the block is further than this, the player will walk towards it.
     */
    private static final double MAX_MINE_DISTANCE = 3;
    /**
     * Number of ticks after which a failsafe is triggered if mining takes too long.
     */
    private static final int FAILSAFE_TICKS = 40;
    /**
     * Time in milliseconds the player can look away from the block before switching targets.
     */
    private static final int LOOK_AWAY_THRESHOLD_MS = 500;
    /**
     * Reference to the Minecraft client instance.
     */
    private final Minecraft mc = Minecraft.getInstance();
    /**
     * Random number generator for introducing slight variability (e.g., in targeting or movement).
     */
    private final Random random = new Random();
    /**
     * Timer used to track how long the player has been looking away from the target block.
     */
    private Clock lookAwayTimer;

    /**
     * Flag indicating whether the player was looking away from the block in the previous tick.
     */
    private boolean wasLookingAway = false;

    /**
     * Number of ticks the player has been attempting to break the current block.
     */
    private int breakAttemptTime;

    /**
     * Estimated number of ticks required to break the current block.
     */
    private int miningTime;

    /**
     * The exact point on the block being targeted for mining.
     */
    private Vec3 targetPoint;

    /**
     * The block position that the player is walking toward if not in range to mine.
     */
    private Vec3 walkingDestinationBlock;

    /**
     * Indicates whether the player is currently walking toward the target block.
     */
    private boolean isWalking;

    /**
     * Latches to true once the player looks at the target block, preventing attack key flickering.
     */
    private boolean hasStartedMining = false;

    /**
     * Tracks if startDestroyBlock has been called for the current target block in 1.21.11.
     */
    private boolean hasSentStartPacket = false;


    @Override
    public void onStart(BlockMiner miner) {
        log("Entering Breaking State");
        breakAttemptTime = 0;
        isWalking = false;
        hasStartedMining = false;
        hasSentStartPacket = false;

        lookAwayTimer = new Clock();
        wasLookingAway = false;

        miningTime = BlockUtil.getMiningTime(
                mc.level.getBlockState(miner.getTargetBlockPos()),
                miner.getMiningSpeed()
        );

        // Setup rotation to look at the block
        RotationHandler.getInstance().stop();
        initializeRotation(miner);
    }

    @Override
    public BlockMinerState onTick(BlockMiner miner) {
        // Handle key presses for mining
        handleKeybinds(miner);

        // Handle Pathfinder navigation if target block is out of mining reach (> 3 blocks) and pathfinder is allowed
        double miningDistance = this.targetPoint != null ? PlayerUtil.getPlayerEyePos().distanceTo(this.targetPoint) : 999;
        if (miningDistance > MAX_MINE_DISTANCE && com.vertexai.Vertex.config().miningMacro.allowPathfinder) {
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
                com.vertexai.feature.impl.Pathfinder.getInstance().stop("Reached mining distance or pathfinder disabled");
            }
        }

        // Handle precision mining
        if (Vertex.config().general.precisionMiner && miner.getTargetParticlePos() != null) {
            RotationHandler.getInstance().easeTo(new RotationConfiguration(new Target(miner.getTargetParticlePos()), 80L, null));
            miner.setTargetParticlePos(null);
        } else {
            miner.setTargetParticlePos(null);
        }

        // Safety mechanism: if we've been trying to break for too long, reset
        if (++this.breakAttemptTime > this.miningTime + FAILSAFE_TICKS) {
            logError("Stuck while mining, return to starting state");
            if (mc.gameMode != null) {
                mc.gameMode.stopDestroyBlock();
            }
            if (com.vertexai.feature.impl.Pathfinder.getInstance().isRunning()) {
                com.vertexai.feature.impl.Pathfinder.getInstance().stop("Stuck while mining");
            }
            return new StartingState();
        }

        // After breaking a block or if target block turns to Bedrock/Air, halt attack and pick new block
        Block detectedBlockType = mc.level.getBlockState(miner.getTargetBlockPos()).getBlock();
        if (detectedBlockType == net.minecraft.world.level.block.Blocks.BEDROCK ||
            detectedBlockType == net.minecraft.world.level.block.Blocks.AIR ||
            !detectedBlockType.equals(miner.getTargetBlockType())) {
            KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);
            if (mc.gameMode != null) {
                mc.gameMode.stopDestroyBlock();
            }
            if (com.vertexai.feature.impl.Pathfinder.getInstance().isRunning()) {
                com.vertexai.feature.impl.Pathfinder.getInstance().stop("Target block broken or converted to bedrock");
            }
            return new StartingState();
        }

        // Drive continuous mining via gameMode.startDestroyBlock / continueDestroyBlock
        BlockPos currentLookingAt = BlockUtil.getBlockLookingAt();
        boolean isLookingAtTarget = miner.getTargetBlockPos() != null && miner.getTargetBlockPos().equals(currentLookingAt);

        if (mc.gameMode != null && miner.getTargetBlockPos() != null) {
            BlockPos targetPos = miner.getTargetBlockPos();
            net.minecraft.core.Direction direction = (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bhr) ? bhr.getDirection() : BlockUtil.getClosestVisibleSide(targetPos);
            if (direction == null) direction = net.minecraft.core.Direction.UP;

            if (isLookingAtTarget) {
                if (!hasSentStartPacket) {
                    mc.gameMode.startDestroyBlock(targetPos, direction);
                    hasSentStartPacket = true;
                } else {
                    mc.gameMode.continueDestroyBlock(targetPos, direction);
                }
            } else {
                if (hasSentStartPacket) {
                    mc.gameMode.stopDestroyBlock();
                    hasSentStartPacket = false;
                }
            }
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
        if (com.vertexai.feature.impl.Pathfinder.getInstance().isRunning()) {
            com.vertexai.feature.impl.Pathfinder.getInstance().stop("Exiting Breaking State");
        }
        log("Exiting Breaking State");
    }

    /**
     * Handles key bindings for mining.
     * Sets attack key to continuously mine ONLY when crosshair is locked onto target block,
     * and holds it down to prevent mining progress resets.
     */
    private void handleKeybinds(BlockMiner miner) {
        BlockPos currentLookingAt = BlockUtil.getBlockLookingAt();
        boolean isLookingAtTarget = miner.getTargetBlockPos() != null && miner.getTargetBlockPos().equals(currentLookingAt);

        if (isLookingAtTarget) {
            hasStartedMining = true;
        }

        KeyBindUtil.setKeyBindState(mc.options.keyAttack, hasStartedMining);

        if (!com.vertexai.feature.impl.Pathfinder.getInstance().isRunning()) {
            boolean shouldSneak = Vertex.config().general.sneakWhileMining;
            // Minimal mode: force sneak for sub-block precision while standing at edges
            if (Vertex.config().miningMacro.allowPathfinder && Vertex.config().miningMacro.pathfinderMode == 0) {
                shouldSneak = true;
            }
            KeyBindUtil.setKeyBindState(mc.options.keyShift, shouldSneak);
            KeyBindUtil.setKeyBindState(mc.options.keyUp, false);
        }
    }

    /**
     * Sets up rotation to look at the target block.
     * Also determines if the player needs to walk toward the block.
     *
     * @param miner The BlockMiner instance
     */
    private void initializeRotation(BlockMiner miner) {
        // Get best points to look at on the block
        List<Vec3> points = BlockUtil.bestPointsOnBestSide(miner.getTargetBlockPos());

        // Handle case where no valid points are found
        if (points.isEmpty()) {
            logError("Cannot find points to look at. Returning to STARTING state.");
            miner.setError(BlockMiner.BlockMinerError.NO_POINTS_FOUND);
            miner.stop();
            return;
        }

        // Select first point as target
        this.targetPoint = points.get(0);

        // Configure rotation to look at target
        RotationHandler.getInstance().stop();
        RotationHandler.getInstance().queueRotation(
                new RotationConfiguration(
                        new Target(targetPoint),
                        Vertex.config().getRandomRotationTime(),
                        null
                )
        );

        // Sometimes randomly choose a different point on the block (for variety)
        if (random.nextBoolean() && Vertex.config().general.randomizedRotations) {
            int halfwayMark = points.size() / 2;
            this.targetPoint = points.get(random.nextInt(halfwayMark) + halfwayMark - 1);

            RotationHandler.getInstance().queueRotation(
                    new RotationConfiguration(
                            new Target(targetPoint),
                            Vertex.config().getRandomRotationTime() * 2L,
                            null
                    )
            );
        }

        RotationHandler.getInstance().start();

        // Determine if the player needs to walk toward block (too far away)
        if (this.targetPoint != null && PlayerUtil.getPlayerEyePos().distanceTo(this.targetPoint) > MAX_MINE_DISTANCE) {
            isWalking = true;
            Vec3 vec = AngleUtil.getVectorForRotation(AngleUtil.getRotationYaw(this.targetPoint));

            // Find walkable block closest to target
            if (mc.level.isEmptyBlock(new BlockPos((int) (mc.player.position().x + vec.x), (int) (mc.player.position().y + vec.y), (int) (mc.player.position().z + vec.z)))) {
                // Note: vec.add in 1.8.9 returns a new Vec3. In Fabric Vec3.add returns a new Vec3.
                // However, BlockPos constructor taking Vec3 is not standard in vanilla.
                // I adjusted the BlockPos construction above to be safe using coords.

                this.walkingDestinationBlock = BlockUtil.getWalkableBlocksAround(PlayerUtil.getBlockStandingOn(), 3)
                        .stream()
                        .min(Comparator.comparingDouble(miner.getTargetBlockPos()::distSqr))
                        .map(b -> new Vec3(b.getX() + 0.5, b.getY(), b.getZ() + 0.5))
                        .orElse(null);
            }
        }
    }
}
