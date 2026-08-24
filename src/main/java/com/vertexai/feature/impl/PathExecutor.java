package com.vertexai.feature.impl;

import kotlin.Pair;
import lombok.Getter;
import lombok.Setter;
import com.vertexai.Vertex;
import com.vertexai.handler.RotationHandler;
import com.vertexai.pathfinder.calculate.Path;
import com.vertexai.pathfinder.helper.BlockStateAccessor;
import com.vertexai.pathfinder.movement.CalculationContext;
import com.vertexai.pathfinder.movement.MovementHelper;
import com.vertexai.pathfinder.util.RaycastPathPlanner;
import com.vertexai.util.*;
import com.vertexai.util.helper.Angle;
import com.vertexai.util.helper.Clock;
import com.vertexai.util.helper.RotationConfiguration;
import com.vertexai.util.helper.Target;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.*;
import java.util.List;

public class PathExecutor {

    // Constants for anti-cheat/human-like behavior
    private static final double FORWARD_KEY_RELEASE_PROBABILITY = 0.02;
    private static final int MAX_YAW_DIFF_FOR_SPRINT = 40;
    private static final int JUMP_DELAY_MIN = 80;
    private static final int JUMP_DELAY_RANDOM = 120;
    private static final double ROTATION_HUMAN_ERROR_FACTOR = 1.2;
    private static final double COLLISION_CHECK_DISTANCE = 0.7;
    private static final int SEGMENT_TIMEOUT_MS = 30000; // 30 seconds per path segment
    private static final double STEP_UP_NO_JUMP_RISE = 0.6;
    private static final double MIN_FORWARD_RISE = -0.05;
    private static final double MAX_JUMPABLE_RISE = 1.25;
    private static final double MAX_JUMP_PROBE_DISTANCE = 2.2;
    private static final double NODE_REACHED_HORIZONTAL_DIST = 0.7;
    private static final double NODE_REACHED_VERTICAL_TOLERANCE = 1.35;
    private static final double SEGMENT_PROGRESS_SWITCH_THRESHOLD = 0.65;
    private static final double STUCK_SPEED_THRESHOLD = 0.05;
    private static final int STUCK_DETECTION_MS = 350;
    private static final int STUCK_RECOVERY_TIMEOUT_MS = 400;
    private static PathExecutor instance;
    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Path> pathQueue = new java.util.concurrent.ConcurrentLinkedDeque<>();
    private final Map<Long, List<Long>> map = new HashMap<>();
    private final List<BlockPos> blockPath = new ArrayList<>();
    private final Clock stuckTimer = new Clock();
    private final Clock stuckRecoveryWindow = new Clock();
    private final Clock nodeSwitchDelay = new Clock();
    private final Clock jumpDelay = new Clock();
    private final Clock segmentTimeout = new Clock();
    private final List<Runnable> onFinishCallbacks = new ArrayList<>();
    private final List<Runnable> onFailCallbacks = new ArrayList<>();
    private final Random random = new Random();
    private final Clock dynamicPitch = new Clock();
    private final Clock rotationCooldown = new Clock();
    private boolean enabled = false;
    private String stopReason = "Not started";
    private Path prev;
    private Path curr;
    private boolean failed = false;
    private boolean succeeded = false;
    private boolean pastTarget = false;
    private boolean attemptedStuckRecovery = false;
    private boolean pendingStuckRecoveryJump = false;
    private double lastPitch = 10 + (15 - 10) * random.nextDouble();
    private int target = 0;
    private int previous = -1;
    private long nodeChangeTime = 0;
    private BlockPos lastLookTargetNode = null;

    private boolean interpolated = true;
    private float interpolYawDiff = 0f;

    private State state = State.STARTING_PATH;

    private boolean allowSprint = true;
    private boolean allowInterpolation = false;
    private boolean allowNodeLook = true;

    public Deque<Path> getPathQueue() { return pathQueue; }
    public boolean isEnabled() { return enabled; }
    public String getStopReason() { return stopReason; }
    public State getState() { return state; }
    public boolean isAllowSprint() { return allowSprint; }
    public void setAllowSprint(boolean allowSprint) { this.allowSprint = allowSprint; }
    public boolean isAllowInterpolation() { return allowInterpolation; }
    public void setAllowInterpolation(boolean allowInterpolation) { this.allowInterpolation = allowInterpolation; }
    public boolean isAllowNodeLook() { return allowNodeLook; }
    public void setAllowNodeLook(boolean allowNodeLook) { this.allowNodeLook = allowNodeLook; }

    public boolean isCombatTargetNear() {
        if (com.vertexai.feature.impl.AutoMobKiller.AutoMobKiller.getInstance().isRunning()) {
            net.minecraft.world.entity.LivingEntity target = com.vertexai.feature.impl.AutoMobKiller.AutoMobKiller.getInstance().getTargetMob();
            if (target != null && target.isAlive() && mc.player != null && mc.player.distanceToSqr(target) <= 256.0) {
                return true;
            }
        }
        return false;
    }

    public static PathExecutor getInstance() {
        if (instance == null) {
            instance = new PathExecutor();
        }
        return instance;
    }

    public void onFinish(Runnable callback) {
        onFinishCallbacks.add(callback);
    }

    public void onFail(Runnable callback) {
        onFailCallbacks.add(callback);
    }

    public void queuePath(Path path) {
        if (path.getPath().isEmpty()) {
            this.stopReason = "Rejected empty path segment";
            error("Path is empty");
            failed = true;
            return;
        }

        BlockPos start = path.getStart();
        Path lastPath = (this.curr != null) ? this.curr : this.pathQueue.peekLast();

        if (lastPath != null && !lastPath.getEnd().equals(start)) {
            this.stopReason = "Rejected disjoint path segment";
            error("This path segment does not start at last path's end. LastPathEnd: " + lastPath.getEnd() + ", ThisPathStart: " + start);
            failed = true;
            return;
        }

        this.pathQueue.offer(path);
    }

    public void start() {
        this.state = State.STARTING_PATH;
        this.enabled = true;
        this.succeeded = false;
        this.failed = false;
        this.stopReason = "Running";
    }

    public boolean isRunning() {
        return this.enabled;
    }

    public boolean succeeded() {
        return this.succeeded;
    }

    public void stop() {
        stop(this.stopReason);
    }

    public void stop(String reason) {
        if (reason != null && !reason.trim().isEmpty()) {
            this.stopReason = reason.trim();
        }

        this.enabled = false;
        this.pathQueue.clear();
        this.blockPath.clear();
        this.map.clear();
        this.curr = null;
        this.prev = null;
        this.target = 0;
        this.previous = -1;
        this.pastTarget = false;
        this.state = State.END;
        this.interpolYawDiff = 0f;
        this.allowSprint = true;
        this.allowInterpolation = false;
        this.nodeChangeTime = 0;
        this.lastLookTargetNode = null;
        this.interpolated = true;
        this.segmentTimeout.reset();
        this.jumpDelay.reset();
        this.stuckTimer.reset();
        this.stuckRecoveryWindow.reset();
        this.rotationCooldown.reset();
        this.attemptedStuckRecovery = false;
        this.pendingStuckRecoveryJump = false;
        StrafeUtil.enabled = false;
        RotationHandler.getInstance().stop();
        KeyBindUtil.releaseAllExcept();
        log("stopped. reason: " + this.stopReason);

        // Execute callbacks
        if (this.succeeded) {
            onFinishCallbacks.forEach(Runnable::run);
        } else if (this.failed) {
            onFailCallbacks.forEach(Runnable::run);
        }
        onFinishCallbacks.clear();
        onFailCallbacks.clear();
    }

    public void clearQueue() {
        this.pathQueue.clear();
        this.curr = null;
        this.succeeded = true;
        this.failed = false;
        this.interpolated = false;
        this.target = 0;
        this.previous = -1;
    }

    public void clearQueuedPaths() {
        this.pathQueue.clear();
        this.curr = null;
        this.target = 0;
        this.previous = -1;
        this.interpolated = false;
        this.succeeded = false;
        this.failed = false;
    }

    public boolean onTick() {
        if (!enabled) {
            return false;
        }

        // --- PREEMPTIVE ARRIVAL CHECK ---
        // If the player physically arrives at the final destination block, instantly stop and succeed.
        // This prevents the bot from doing weird micro-adjustments or following unnecessary trailing nodes
        // after it has already achieved the ultimate goal.
        BlockPos finalDest = null;
        if (this.pathQueue.isEmpty() && this.curr != null) {
            finalDest = this.curr.getEnd();
        } else if (!this.pathQueue.isEmpty()) {
            finalDest = this.pathQueue.peekLast().getEnd();
        }
        
        if (finalDest != null) {
            double hDist = Math.hypot(mc.player.getX() - finalDest.getX() - 0.5, mc.player.getZ() - finalDest.getZ() - 0.5);
            double vDist = Math.abs(mc.player.getY() - finalDest.getY());
            if (hDist <= NODE_REACHED_HORIZONTAL_DIST && vDist <= NODE_REACHED_VERTICAL_TOLERANCE) {
                log("Physically arrived at final destination preemptively!");
                this.succeeded = true;
                this.failed = false;
                this.stop("Reached destination");
                return false;
            }
        }
        // --------------------------------

        double horizontalSpeed = Math.hypot(mc.player.getDeltaMovement().x, mc.player.getDeltaMovement().z);
        if (this.attemptedStuckRecovery && this.stuckRecoveryWindow.isScheduled() && this.stuckRecoveryWindow.passed()) {
            if (horizontalSpeed < STUCK_SPEED_THRESHOLD) {
                this.failed = true;
                this.succeeded = false;
                this.stop("Stuck recovery jump failed near " + PlayerUtil.getBlockStandingOn() + " (target index " + this.target + ")");
                return false;
            }
            this.attemptedStuckRecovery = false;
            this.pendingStuckRecoveryJump = false;
            this.stuckRecoveryWindow.reset();
        }

        if (this.stuckTimer.isScheduled() && this.stuckTimer.passed()) {
            if (!this.attemptedStuckRecovery) {
                this.attemptedStuckRecovery = true;
                this.pendingStuckRecoveryJump = true;
                this.stuckRecoveryWindow.schedule(STUCK_RECOVERY_TIMEOUT_MS);
                this.stuckTimer.reset();
                log("Was stuck for a second. Attempting one recovery jump.");
            } else {
                log("Was Stuck For a Second.");
                this.failed = true;
                this.succeeded = false;
                this.stop("Stuck for " + STUCK_DETECTION_MS + "ms near " + PlayerUtil.getBlockStandingOn() + " (target index " + this.target + ")");
                return false;
            }
        }

        if (this.segmentTimeout.isScheduled() && this.segmentTimeout.passed()) {
            error("Path segment timed out after " + SEGMENT_TIMEOUT_MS + "ms");
            this.failed = true;
            this.succeeded = false;
            this.stop("Path segment timeout after " + SEGMENT_TIMEOUT_MS + "ms (target index " + this.target + ")");
        }

        BlockPos playerPos = PlayerUtil.getBlockStandingOn();
        if (this.curr != null) {
            // Removed flawed blockHashes skipping logic that caused the bot to skip path nodes and walk into walls

            if (horizontalSpeed < STUCK_SPEED_THRESHOLD) {
                if (!this.stuckTimer.isScheduled()) {
                    this.stuckTimer.schedule(STUCK_DETECTION_MS);
                }
            } else {
                this.stuckTimer.reset();
                this.attemptedStuckRecovery = false;
                this.pendingStuckRecoveryJump = false;
                this.stuckRecoveryWindow.reset();
            }
        } else {
            if (this.stuckTimer.isScheduled()) {
                this.stuckTimer.reset();
            }
            this.attemptedStuckRecovery = false;
            this.pendingStuckRecoveryJump = false;
            this.stuckRecoveryWindow.reset();
            if (this.pathQueue.isEmpty()) {
                return true;
            }
        }

        if (this.curr == null || this.target == this.blockPath.size()) {
            if (this.pathQueue.isEmpty()) {
                if (this.curr != null) {
                    log("Reached final destination");
                    this.succeeded = true;
                    this.failed = false;
                    this.stop("Reached destination");
                    return false;
                }
                return true; // Awaiting path calculation from A* worker
            }
            log("Path traversed, loading next segment from queue");
            this.succeeded = false;
            this.failed = false;
            this.prev = this.curr;
            this.target = 1;
            this.previous = 0;
            loadPath(this.pathQueue.poll());
            if (this.target == this.blockPath.size()) {
                return true;
            }
            log("loaded new path target: " + this.target + ", prev: " + this.previous);
        }

        Vec3 playerPosVec = mc.player.position();
        
        // --- REAL-TIME NEAREST NODE TRACKING (Floor-Bounded) ---
        // Locate the nearest node along the immediate path window on the current Y-floor
        int searchStart = Math.max(0, this.target - 1);
        int searchEnd = Math.min(this.target + 3, this.blockPath.size());
        int nearestIdx = this.target;
        double minDistanceSq = Double.MAX_VALUE;
        for (int i = searchStart; i < searchEnd; i++) {
            BlockPos node = this.blockPath.get(i);
            if (Math.abs(mc.player.getY() - node.getY()) <= 0.6) {
                double distSq = playerPosVec.distanceToSqr(node.getX() + 0.5, node.getY(), node.getZ() + 0.5);
                if (distSq < minDistanceSq) {
                    minDistanceSq = distSq;
                    nearestIdx = i;
                }
            }
        }
        
        if (nearestIdx > this.target) {
            this.previous = this.target;
            this.target = nearestIdx;
        }

        // --- FUTURE NODE SKIPPING (Catch-up, Same-Floor Only) ---
        boolean advanced = false;
        for (int i = Math.min(this.target + 2, this.blockPath.size() - 1); i > this.target; i--) {
            BlockPos futureNode = this.blockPath.get(i);
            double hDist = Math.hypot(playerPosVec.x - futureNode.getX() - 0.5, playerPosVec.z - futureNode.getZ() - 0.5);
            double vDist = Math.abs(mc.player.getY() - futureNode.getY());
            
            if (hDist <= 0.55 && vDist <= 0.5) {
                if (nodeSwitchDelay.passed()) {
                    this.previous = i;
                    this.target = Math.min(i + 1, this.blockPath.size() - 1);
                    nodeSwitchDelay.schedule(50 + random.nextInt(70));
                    log("caught up to target index on same floor: " + this.target);
                    advanced = true;
                    break;
                }
            }
        }
        
        BlockPos target = this.blockPath.get(Math.min(this.target, this.blockPath.size() - 1));
        if (!advanced) {
            double horizontalDistToCurrent = Math.hypot(playerPosVec.x - target.getX() - 0.5, playerPosVec.z - target.getZ() - 0.5);
            double verticalDistToCurrent = Math.abs(mc.player.getY() - target.getY());

            boolean isVerticalTransition = target.getY() != playerPos.getY() 
                    || (this.previous >= 0 && this.previous < this.blockPath.size() && this.blockPath.get(this.previous).getY() != target.getY());
            
            boolean isTurnTransition = false;
            if (this.target < this.blockPath.size() - 1 && this.previous >= 0 && this.previous < this.blockPath.size()) {
                BlockPos prev = this.blockPath.get(this.previous);
                BlockPos next = this.blockPath.get(this.target + 1);
                int d1x = target.getX() - prev.getX();
                int d1z = target.getZ() - prev.getZ();
                int d2x = next.getX() - target.getX();
                int d2z = next.getZ() - target.getZ();
                if (d1x != d2x || d1z != d2z) {
                    isTurnTransition = true;
                }
            }

            double reqH = (isVerticalTransition || isTurnTransition) ? 0.50 : NODE_REACHED_HORIZONTAL_DIST;
            double reqV = isVerticalTransition ? 0.45 : NODE_REACHED_VERTICAL_TOLERANCE;

            boolean closeToCurrentNode = horizontalDistToCurrent <= reqH && verticalDistToCurrent <= reqV;

            boolean overshot = false;
            BlockPos prevTarget = (this.previous >= 0 && this.previous < this.blockPath.size()) ? this.blockPath.get(this.previous) : null;
            if (prevTarget != null) {
                Vec3 prevCenter = new Vec3(prevTarget.getX() + 0.5, 0.0, prevTarget.getZ() + 0.5);
                Vec3 currCenter = new Vec3(target.getX() + 0.5, 0.0, target.getZ() + 0.5);
                Vec3 v = currCenter.subtract(prevCenter);
                double vLenSqr = v.x * v.x + v.z * v.z;
                if (vLenSqr > 1.0E-6) {
                    Vec3 p = new Vec3(playerPosVec.x, 0.0, playerPosVec.z).subtract(prevCenter);
                    
                    // Cross-track error (2D cross product magnitude / length of AB)
                    double crossProduct = p.x * v.z - p.z * v.x;
                    double perpendicularDist = Math.abs(crossProduct) / Math.sqrt(vLenSqr);
                    
                    if (perpendicularDist > 5.0) {
                        this.failed = true;
                        this.succeeded = false;
                        this.stop("Off course: deviated " + String.format("%.2f", perpendicularDist) + " blocks from path");
                        return false;
                    }

                    double progress = (p.x * v.x + p.z * v.z) / vLenSqr;
                    // If progress is >= 1.0, crossed the orthogonal plane of the target node!
                    if (progress >= 1.0 && verticalDistToCurrent <= reqV) {
                        overshot = true;
                    }
                }
            }

            if (this.target < this.blockPath.size() - 1) {
                if ((closeToCurrentNode || overshot) && nodeSwitchDelay.passed()) {
                    this.previous = this.target;
                    this.target++;
                    nodeSwitchDelay.schedule(50 + random.nextInt(70));
                    log("advanced to next target (" + (overshot && !closeToCurrentNode ? "overshot" : "close") + ")");
                    advanced = true;
                }
            } else if (this.target == this.blockPath.size() - 1) {
                if (closeToCurrentNode || overshot) {
                    log("Reached final destination node");
                    this.succeeded = true;
                    this.failed = false;
                    if (this.pathQueue.isEmpty()) {
                        this.stop("Reached destination");
                        return false;
                    } else {
                        this.prev = this.curr;
                        this.target = 1;
                        this.previous = 0;
                        loadPath(this.pathQueue.poll());
                        return true;
                    }
                }
            }
        }

        if (advanced) {
            if (this.target >= this.blockPath.size()) {
                 return true; // Let the next tick handle path completion via the preemptive block
            }
            target = this.blockPath.get(this.target);
        }

        // --- LOOK AT TARGET (Raycast line-of-sight planning & continuous rotation coordination) ---
        int raycastLookIndex = RaycastPathPlanner.findFurthestVisibleNodeIndex(playerPosVec, this.blockPath, this.target, 8);
        
        // Find if there is an elevation change (stairs/hill) ahead within 6 blocks
        for (int i = this.target; i <= Math.min(this.target + 6, this.blockPath.size() - 1); i++) {
            BlockPos node = this.blockPath.get(i);
            if (node.getY() != playerPos.getY()) {
                raycastLookIndex = i;
                break;
            }
        }
        BlockPos lookTargetNode = this.blockPath.get(Math.min(raycastLookIndex, this.blockPath.size() - 1));

        boolean onGround = mc.player.onGround();

        int targetX = lookTargetNode.getX();
        int targetZ = lookTargetNode.getZ();
        double horizontalDistToTarget = Math.hypot(mc.player.getX() - targetX - 0.5, mc.player.getZ() - targetZ - 0.5);
        float yaw = AngleUtil.getRotationYaw360(mc.player.position(), new Vec3(targetX + 0.5, 0.0, targetZ + 0.5));
        float rawDiff = Math.abs(AngleUtil.get360RotationYaw() - yaw);
        float yawDiff = rawDiff > 180 ? 360 - rawDiff : rawDiff;
        
        // If we are intentionally standing still to wait for the camera to rotate, reset the stuck timer
        if (yawDiff >= 25 && onGround) {
            this.stuckTimer.reset();
        }

        if (this.interpolYawDiff == 0) {
            this.interpolYawDiff = yaw - AngleUtil.get360RotationYaw();
        }
        // Disable StrafeUtil for realistic client-side movement
        StrafeUtil.enabled = false;

        boolean inWater = mc.player.isInWater() || mc.player.isUnderWater();
        boolean ascendingInWater = inWater && (target.getY() >= playerPos.getY() || (this.target < this.blockPath.size() - 1 && this.blockPath.get(this.target + 1).getY() >= playerPos.getY()));

        // Calculate natural human pitch (looks up on stairs/hills, down on drops, up in water)
        float targetPitch;
        if (ascendingInWater) {
            targetPitch = -30.0f; // Look up toward surface to swim up
        } else {
            double dy = (lookTargetNode.getY() + 0.6) - mc.player.getEyeY();
            double dxz = Math.max(1.0, horizontalDistToTarget);
            float calculatedPitch = (float) -Math.toDegrees(Math.atan2(dy, dxz));
            // Clamp pitch to comfortable human viewing range (-24° up on stairs to +12° down on drops)
            targetPitch = Mth.clamp(calculatedPitch, -24.0f, 12.0f);
        }

        // Rotate player to face look target with a 300ms cooldown before adjusting mouse angle again
        boolean cooldownPassed = !this.rotationCooldown.isScheduled() || this.rotationCooldown.passed();
        boolean sharpTurn = yawDiff > 25.0f;
        boolean needPitchAdjustment = Math.abs(mc.player.getXRot() - targetPitch) > 7.0f;

        if (this.allowNodeLook && !isCombatTargetNear()) {
            if (cooldownPassed && (yawDiff > 2.5f || needPitchAdjustment || sharpTurn || ascendingInWater)) {
                this.rotationCooldown.schedule(300);
                float time = Vertex.config().debug.useFixedRotation 
                        ? Vertex.config().debug.fixedRotationTime 
                        : Math.max(100, Math.min(240, (long) (yawDiff * 2.0f)));

                RotationHandler.getInstance().easeTo(
                        new RotationConfiguration(
                                new Target(new Angle(yaw, targetPitch)),
                                (long) time,
                                RotationConfiguration.RotationType.CLIENT,
                                null
                        )
                );
            }
        }

        // Coordinate target movement vector:
        // Use immediate node for tight navigation if turning or stairs, or raycast vector if clear
        Vec3 immediateTargetVec = new Vec3(target.getX() + 0.5, mc.player.getY(), target.getZ() + 0.5);
        Vec3 raycastTargetVec = new Vec3(lookTargetNode.getX() + 0.5, mc.player.getY(), lookTargetNode.getZ() + 0.5);
        boolean isElevationChange = target.getY() != playerPos.getY();
        Vec3 targetVec = (yawDiff < 25.0f && !isElevationChange) ? raycastTargetVec : immediateTargetVec;

        List<KeyMapping> neededKeys = new ArrayList<>();
        // If turning sharply on stairs/corners, wait briefly for rotation to align
        if (!(yawDiff > 50.0f && isElevationChange)) {
            neededKeys.addAll(KeyBindUtil.getNeededKeyPresses(mc.player.position(), targetVec));
        }
        
        List<KeyMapping> keyBindings = new ArrayList<>(neededKeys);

        // Preserve attack/use item state
        if (mc.options.keyUse.isDown()) {
            keyBindings.add(mc.options.keyUse);
        }
        if (mc.options.keyAttack.isDown()) {
            keyBindings.add(mc.options.keyAttack);
        }

        // Water ascending & swimming up
        if (ascendingInWater) {
            keyBindings.add(mc.options.keyJump);
            keyBindings.add(mc.options.keyUp);
        }

        // Jump only when path requires a one-block step-up and landing space is valid.
        boolean shouldJump = shouldJumpOneBlock(playerPos, target, horizontalDistToTarget);
        boolean recoveryJumping = false;
        if (shouldJump && onGround && (!this.jumpDelay.isScheduled() || this.jumpDelay.passed())) {
            keyBindings.add(mc.options.keyJump);
            this.jumpDelay.schedule(JUMP_DELAY_MIN + random.nextInt(JUMP_DELAY_RANDOM));
            this.state = State.JUMPING;
        }
        if (this.pendingStuckRecoveryJump && onGround && (!this.jumpDelay.isScheduled() || this.jumpDelay.passed())) {
            keyBindings.add(mc.options.keyJump);
            this.jumpDelay.schedule(JUMP_DELAY_MIN + random.nextInt(JUMP_DELAY_RANDOM));
            this.pendingStuckRecoveryJump = false;
            recoveryJumping = true;
            this.state = State.JUMPING;
            log("Issued stuck recovery jump.");
        }

        // Apply all the calculated key presses
        KeyBindUtil.holdThese(keyBindings.toArray(new KeyMapping[0]));

        // Handle sprinting - walk strictly on elevation changes and sharp turns
        boolean shouldSprint = this.allowSprint && yawDiff < 25.0f && !isElevationChange;
        KeyBindUtil.setKeyBindState(mc.options.keySprint, shouldSprint);
        if (shouldSprint && mc.player != null && mc.options.keyUp.isDown()) {
            mc.player.setSprinting(true);
        }

        return mc.player.position().distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5) < 100;
    }

    public void loadPath(Path path) {
        this.blockPath.clear();
        this.map.clear();

        this.curr = path;
        List<BlockPos> smoothed = this.curr.getSmoothedPath();

        // Raycast-guided path node optimization
        if (mc.level != null && smoothed.size() > 2) {
            List<BlockPos> raycastOptimized = new ArrayList<>();
            raycastOptimized.add(smoothed.get(0));
            int currIdx = 0;
            while (currIdx < smoothed.size() - 1) {
                int furthestReachable = currIdx + 1;
                for (int nextIdx = Math.min(currIdx + 8, smoothed.size() - 1); nextIdx > currIdx + 1; nextIdx--) {
                    BlockPos p1 = smoothed.get(currIdx);
                    BlockPos p2 = smoothed.get(nextIdx);
                    if (RaycastPathPlanner.hasLineOfSight(
                            new Vec3(p1.getX() + 0.5, p1.getY(), p1.getZ() + 0.5),
                            new Vec3(p2.getX() + 0.5, p2.getY(), p2.getZ() + 0.5),
                            mc.level)) {
                        furthestReachable = nextIdx;
                        break;
                    }
                }
                raycastOptimized.add(smoothed.get(furthestReachable));
                currIdx = furthestReachable;
            }
            this.blockPath.addAll(raycastOptimized);
        } else {
            this.blockPath.addAll(smoothed);
        }

        for (int i = 0; i < this.blockPath.size(); i++) {
            BlockPos pos = this.blockPath.get(i);
            this.map.computeIfAbsent(this.pack(pos.getX(), pos.getZ()), k -> new ArrayList<>()).add(this.pack(pos.getY(), i));
        }
    }

    public void onRender() {
        if (this.target != -1 && this.target < this.blockPath.size()) {
            BlockPos playerPos = PlayerUtil.getBlockStandingOn();
            BlockPos target = this.blockPath.get(this.target);
            int targetX = target.getX();
            int targetZ = target.getZ();
            Vec3 playerPosVec = mc.player.position();
            float yaw = AngleUtil.getRotationYaw360(playerPosVec, new Vec3(targetX + 0.5, 0.0, targetZ + 0.5));
            Vec3 pos = new Vec3(playerPosVec.x, playerPos.getY() + 0.5, playerPosVec.z);
            Vec3 vec4Rot = AngleUtil.getVectorForRotation(yaw);
            // Keep marker just above the player's feet; this is a delta, not an absolute Y.
            Vec3 newV = pos.add(vec4Rot.x, +1, vec4Rot.z);
            RenderUtil.drawBlock(new BlockPos((int) newV.x, (int) newV.y, (int) newV.z), // BlockPos.ofFloored check?
                    new Color(255, 0, 0, 255));
            RenderUtil.drawBlock(playerPos, new Color(255, 255, 0, 100));
        }
    }

    public Path getPreviousPath() {
        return this.prev;
    }

    public Path getCurrentPath() {
        return this.curr;
    }

    public boolean failed() {
        return !this.enabled && this.failed;
    }

    public boolean ended() {
        return !this.enabled && this.succeeded;
    }

    private boolean shouldJumpOneBlock(BlockPos playerPos, BlockPos targetPos, double horizontalDistToTarget) {
        if (mc.player == null || mc.level == null) {
            return false;
        }

        // 1. Immediate Horizontal Collision against a step-up (player is pressing against a 1-block step-up)
        if (mc.player.horizontalCollision && mc.player.onGround()) {
            BlockStateAccessor bsa = new BlockStateAccessor(mc.level);
            int px = playerPos.getX();
            int py = playerPos.getY();
            int pz = playerPos.getZ();
            if (MovementHelper.INSTANCE.canWalkThrough(bsa, px, py + 3, pz, bsa.get(px, py + 3, pz))) {
                return true;
            }
        }

        // 2. Target node is elevated above player standing Y
        if (targetPos.getY() > playerPos.getY() && horizontalDistToTarget <= 2.5) {
            return shouldJumpTowardTargetLive(playerPos, targetPos, horizontalDistToTarget);
        }

        // 3. Probe next target node if approaching transition to an elevated node
        if (this.target < this.blockPath.size() - 1) {
            BlockPos nextTarget = this.blockPath.get(this.target + 1);
            double horizontalDistToNext = Math.hypot(mc.player.getX() - nextTarget.getX() - 0.5, mc.player.getZ() - nextTarget.getZ() - 0.5);
            if (nextTarget.getY() > playerPos.getY() && horizontalDistToNext <= 2.5) {
                return shouldJumpTowardTargetLive(playerPos, nextTarget, horizontalDistToNext);
            }
        }

        return false;
    }

    private boolean shouldJumpTowardTargetLive(BlockPos playerPos, BlockPos desiredTarget, double horizontalDist) {
        if (horizontalDist > MAX_JUMP_PROBE_DISTANCE) {
            return false;
        }

        // Calculate exact vertical rise in world coordinates
        double playerSurfaceY = mc.player.getY();
        double targetSurfaceY = desiredTarget.getY() + com.vertexai.pathing.PartialBlockHelper.getStandingHeightOffset(mc.level, desiredTarget);
        double deltaY = targetSurfaceY - playerSurfaceY;

        // Check if destination is too high for vanilla jump (max 1.25 blocks rise)
        // If jumping from a slab (0.5) to a full block (2.0), deltaY is 1.5 blocks -> impossible in vanilla!
        if (deltaY > 1.25 || deltaY < -0.2) {
            return false;
        }

        int stepX = Integer.compare(desiredTarget.getX(), playerPos.getX());
        int stepZ = Integer.compare(desiredTarget.getZ(), playerPos.getZ());
        if (stepX == 0 && stepZ == 0) {
            return false;
        }

        BlockStateAccessor bsa = new BlockStateAccessor(mc.level);
        int px = playerPos.getX();
        int py = playerPos.getY();
        int pz = playerPos.getZ();

        int landingX = px + stepX;
        int landingZ = pz + stepZ;

        var landingFeetState = bsa.get(landingX, py, landingZ);
        var landingStepUpState = bsa.get(landingX, py + 1, landingZ);

        // If stepping onto normal stairs or bottom slabs with <= 0.6 height rise, vanilla Minecraft handles it smoothly without jumping
        if (deltaY <= 0.6) {
            if (landingFeetState.getBlock() instanceof net.minecraft.world.level.block.StairBlock) {
                if (landingFeetState.getValue(net.minecraft.world.level.block.StairBlock.HALF) == net.minecraft.world.level.block.state.properties.Half.BOTTOM) {
                    return false; // Auto-step smoothly
                }
            }
            if (landingFeetState.getBlock() instanceof net.minecraft.world.level.block.SlabBlock) {
                if (landingFeetState.getValue(net.minecraft.world.level.block.SlabBlock.TYPE) == net.minecraft.world.level.block.state.properties.SlabType.BOTTOM) {
                    return false; // Auto-step smoothly
                }
            }
        }

        // Block directly ahead at feet level (py + 1)
        boolean feetBlocked = !MovementHelper.INSTANCE.canWalkThrough(bsa, landingX, py + 1, landingZ, landingStepUpState);

        // Valid landing surface at py + 1 (1-block step up)
        boolean validLanding = MovementHelper.INSTANCE.canStandOn(bsa, landingX, py + 1, landingZ, landingStepUpState);

        // Player headroom (py + 3)
        boolean hasHeadroom = MovementHelper.INSTANCE.canWalkThrough(bsa, px, py + 3, pz, bsa.get(px, py + 3, pz))
                && MovementHelper.INSTANCE.canWalkThrough(bsa, landingX, py + 3, landingZ, bsa.get(landingX, py + 3, landingZ));

        return (feetBlocked || validLanding) && hasHeadroom;
    }

    private long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public Pair<Integer, Integer> unpack(long packed) {
        return new Pair<>((int) (packed >> 32), (int) packed);
    }

    void log(String message) {
        Logger.sendLog(getMessage(message));
    }

    void send(String message) {
        Logger.sendMessage(getMessage(message));
    }

    void error(String message) {
        Logger.sendError(getMessage(message));
    }

    void note(String message) {
        Logger.sendNote(getMessage(message));
    }

    String getMessage(String message) {
        return "[PathExecutor] " + message;
    }

    // Deprecated? Anti-cheat method not fully implemented in old code?
    private boolean shouldAvoidForwardMovement(float yaw, BlockPos playerPos) {
        Vec3 eye = PlayerUtil.getPlayerEyePos();
        Vec3 forward = AngleUtil.getVectorForRotation(yaw);
        Vec3 probe = eye.add(forward.x * COLLISION_CHECK_DISTANCE, 0, forward.z * COLLISION_CHECK_DISTANCE);
        HitResult hit = RaytracingUtil.fastRayTrace(mc.player, eye, probe, Collections.emptyList());
        if (hit != null && (hit.getType() == HitResult.Type.BLOCK || hit.getType() == HitResult.Type.ENTITY)) {
            return true;
        }

        BlockPos ahead = BlockPos.containing(
                mc.player.getX() + forward.x * COLLISION_CHECK_DISTANCE,
                playerPos.getY(),
                mc.player.getZ() + forward.z * COLLISION_CHECK_DISTANCE
        );
        return !BlockUtil.canStandOn(ahead);
    }

    enum State {
        STARTING_PATH, TRAVERSING, JUMPING, WAITING, END
    }
}
