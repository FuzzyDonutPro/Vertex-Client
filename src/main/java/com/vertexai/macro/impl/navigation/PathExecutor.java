package com.vertexai.macro.impl.navigation;

import kotlin.Pair;
import lombok.Getter;
import lombok.Setter;
import com.vertexai.Vertex;
import com.vertexai.handler.RotationHandler;
import com.vertexai.pathfinder.calculate.Path;
import com.vertexai.pathfinder.helper.BlockStateAccessor;
import com.vertexai.pathfinder.movement.CalculationContext;
import com.vertexai.pathfinder.movement.MovementHelper;
import com.vertexai.util.*;
import com.vertexai.util.helper.Angle;
import com.vertexai.util.helper.Clock;
import com.vertexai.util.helper.RotationConfiguration;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
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
    private final Deque<Path> pathQueue = new LinkedList<>();
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

    private boolean interpolated = true;
    private float interpolYawDiff = 0f;

    private State state = State.STARTING_PATH;

    private boolean allowSprint = true;
    private boolean allowInterpolation = false;

    public Deque<Path> getPathQueue() { return pathQueue; }
    public boolean isEnabled() { return enabled; }
    public String getStopReason() { return stopReason; }
    public State getState() { return state; }
    public boolean isAllowSprint() { return allowSprint; }
    public void setAllowSprint(boolean allowSprint) { this.allowSprint = allowSprint; }
    public boolean isAllowInterpolation() { return allowInterpolation; }
    public void setAllowInterpolation(boolean allowInterpolation) { this.allowInterpolation = allowInterpolation; }

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

        if (lastPath != null && !lastPath.getGoal().isAtGoal(start.getX(), start.getY(), start.getZ())) {
            log("Disjoint path segment detected (last goal " + lastPath.getGoal() + " vs new start " + start + "). Resetting queue to new segment.");
            this.pathQueue.clear();
            this.curr = null;
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
        this.interpolated = true;
        this.segmentTimeout.reset();
        this.jumpDelay.reset();
        this.stuckTimer.reset();
        this.stuckRecoveryWindow.reset();
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
                if (this.state == State.STARTING_PATH || Pathfinder.getInstance().isPathfinding()) {
                    return true;
                }
                log("Reached final destination");
                this.succeeded = true;
                this.failed = false;
                this.stop("Reached destination");
                return false;
            }
        }

        if (this.curr == null || this.target == this.blockPath.size()) {
            log("Path traversed");
            if (this.pathQueue.isEmpty()) {
                if (this.state == State.STARTING_PATH || Pathfinder.getInstance().isPathfinding()) {
                    return true;
                }
                log("Reached final destination");
                this.succeeded = true;
                this.failed = false;
                this.stop("Reached destination");
                return false;
            }
            this.succeeded = true;
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

        BlockPos target = this.blockPath.get(this.target);
        Vec3 playerPosVec = mc.player.position();
        
        // --- FUTURE NODE SKIPPING (Catch-up) ---
        // If the player got bumped and accidentally skipped a node but landed on a future node,
        // instantly advance the target to that future node so it doesn't run backwards.
        boolean advanced = false;
        for (int i = this.blockPath.size() - 1; i > this.target; i--) {
            BlockPos futureNode = this.blockPath.get(i);
            double hDist = Math.hypot(playerPosVec.x - futureNode.getX() - 0.5, playerPosVec.z - futureNode.getZ() - 0.5);
            double vDist = Math.abs(mc.player.getY() - futureNode.getY());
            
            if (hDist <= NODE_REACHED_HORIZONTAL_DIST && vDist <= NODE_REACHED_VERTICAL_TOLERANCE) {
                if (nodeSwitchDelay.passed()) {
                    this.previous = i;
                    this.target = i + 1; // We reached node i, so target the next one
                    nodeSwitchDelay.schedule(50 + random.nextInt(70));
                    log("skipped missed nodes and caught up to target index: " + this.target);
                    advanced = true;
                    break;
                }
            }
        }
        
        if (!advanced) {
            double horizontalDistToCurrent = Math.hypot(playerPosVec.x - target.getX() - 0.5, playerPosVec.z - target.getZ() - 0.5);
            double verticalDistToCurrent = Math.abs(mc.player.getY() - target.getY());
            boolean closeToCurrentNode = horizontalDistToCurrent <= NODE_REACHED_HORIZONTAL_DIST
                    && verticalDistToCurrent <= NODE_REACHED_VERTICAL_TOLERANCE;

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
                    // If progress is >= 1.0, we have crossed the orthogonal plane of the target node!
                    if (progress >= 1.0 && verticalDistToCurrent <= NODE_REACHED_VERTICAL_TOLERANCE) {
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
            }
        }

        if (advanced) {
            if (this.target >= this.blockPath.size()) {
                 return true; // Let the next tick handle path completion via the preemptive block
            }
            target = this.blockPath.get(this.target);
        }

        boolean onGround = mc.player.onGround();

        int targetX = target.getX();
        int targetZ = target.getZ();
        double horizontalDistToTarget = Math.hypot(mc.player.getX() - targetX - 0.5, mc.player.getZ() - targetZ - 0.5);

        // Look directly at next node
        Vec3 nodeCenter = new Vec3(targetX + 0.5, target.getY() + 0.5, targetZ + 0.5);
        Vec3 eyePos = PlayerUtil.getPlayerEyePos();
        Angle targetAngle = AngleUtil.getRotation(eyePos, nodeCenter);
        float yaw = targetAngle.getYaw();

        // Scale down vertical movement by 60% (keep 40%) unless going uphill
        double deltaY = nodeCenter.y - eyePos.y;
        float pitch = targetAngle.getPitch();
        if (deltaY <= 0) {
            pitch = pitch * 0.40f;
        }

        float rawDiff = Math.abs(AngleUtil.get360RotationYaw() - AngleUtil.get360RotationYaw(yaw));
        float yawDiff = rawDiff > 180 ? 360 - rawDiff : rawDiff;
        
        if (yawDiff >= 30 && onGround) {
            this.stuckTimer.reset();
        }

        if (this.interpolYawDiff == 0) {
            this.interpolYawDiff = yaw - AngleUtil.get360RotationYaw();
        }
        // Disable StrafeUtil for realistic client-side movement
        StrafeUtil.enabled = false;

        // Smoothly rotate camera toward target node
        if (yawDiff > 3 && !RotationHandler.getInstance().isEnabled()) {
            float rotYaw = yaw + (float) (random.nextGaussian() * ROTATION_HUMAN_ERROR_FACTOR);
            float time = Vertex.config().debug.useFixedRotation ? Vertex.config().debug.fixedRotationTime : Math.max(220, (long) (360 - horizontalDistToTarget * Vertex.config().debug.rotationMultiplier));

            RotationHandler.getInstance().easeTo(
                    new RotationConfiguration(
                            new Angle(rotYaw, pitch),
                            (long) time, null
                    )
            );
        }

        // Calculate which WASD keys to press based on current player rotation (not target direction)
        // This makes movement purely client-side and realistic
        Vec3 targetVec = new Vec3(targetX + 0.5, mc.player.getY(), targetZ + 0.5);
        
        List<KeyMapping> neededKeys = new ArrayList<>();
        // KeyBindUtil calculates strafing vectors correctly relative to camera angle,
        // so we never need to stop walking just because we are turning our head.
        neededKeys.addAll(KeyBindUtil.getNeededKeyPresses(mc.player.position(), targetVec));
        
        List<KeyMapping> keyBindings = new ArrayList<>(neededKeys);

        // Preserve attack/use item state
        if (mc.options.keyUse.isDown()) {
            keyBindings.add(mc.options.keyUse);
        }
        if (mc.options.keyAttack.isDown()) {
            keyBindings.add(mc.options.keyAttack);
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

        // Handle sprinting - sprint whenever moving forward along path
        boolean shouldSprint = this.allowSprint && yawDiff < 45.0f;
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
        this.blockPath.addAll(this.curr.getSmoothedPath());
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
        return !this.enabled || this.state == State.END;
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

        // Block directly ahead at feet level (py + 1)
        boolean feetBlocked = !MovementHelper.INSTANCE.canWalkThrough(bsa, landingX, py + 1, landingZ, bsa.get(landingX, py + 1, landingZ));

        // Valid landing surface at py + 1 (1-block step up)
        boolean validLanding = MovementHelper.INSTANCE.canStandOn(bsa, landingX, py + 1, landingZ, bsa.get(landingX, py + 1, landingZ));

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
