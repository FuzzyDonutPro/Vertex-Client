package com.vertexai.feature.impl.AutoMobKiller.states;

import com.vertexai.Vertex;
import com.vertexai.feature.impl.AutoMobKiller.AutoMobKiller;
import com.vertexai.feature.impl.Pathfinder;
import com.vertexai.handler.RotationHandler;
import com.vertexai.util.helper.Clock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

public class PathfindingState implements AutoMobKillerState {

    private static final double ENTER_KILL_RANGE_SQ = 12.25; // 3.5 blocks
    private static final double TARGET_DRIFT_REPATH_THRESHOLD_SQ = 6.25; // 2.5 blocks
    private static final int MAX_REPATH_ATTEMPTS = 4;
    private static final long PATHING_TIMEOUT_MS = 10_000L;
    private static final long REPATH_DELAY_MS = 180L;

    private final Minecraft mc = Minecraft.getInstance();
    private final Clock timeout = new Clock();
    private final Clock repathDelay = new Clock();
    private int pathAttempts = 0;
    private BlockPos lastQueuedTarget = null;

    @Override
    public void onStart(AutoMobKiller mobKiller) {
        log("Entering pathfinding state");
        pathAttempts = 0;
        timeout.reset();
        timeout.schedule(PATHING_TIMEOUT_MS);
        repathDelay.reset();
        lastQueuedTarget = null;

        Pathfinder.getInstance().setSprintState(Vertex.config().commission.dwarvenCommission.mobKillerSprint);
        Pathfinder.getInstance().setInterpolationState(Vertex.config().commission.dwarvenCommission.mobKillerInterpolate);
        queuePathToTarget(mobKiller, true);
    }

    @Override
    public AutoMobKillerState onTick(AutoMobKiller mobKiller) {
        LivingEntity target = mobKiller.getTargetMob();
        if (target == null || !target.isAlive() || mc.player == null) {
            Pathfinder.getInstance().stop();
            RotationHandler.getInstance().stop();
            return new FindMobState();
        }

        if (isInKillRange(mobKiller)) {
            Pathfinder.getInstance().stop();
            return new KillState();
        }

        // Repath if target mob moved significantly
        if (target.position().distanceToSqr(mobKiller.getTargetMobOriginalPos()) > TARGET_DRIFT_REPATH_THRESHOLD_SQ) {
            if (++pathAttempts > MAX_REPATH_ATTEMPTS) {
                log("Target mob moved away too many times. Re-choosing mob.");
                mobKiller.blacklistTargetMob();
                Pathfinder.getInstance().stop();
                return new FindMobState();
            }
            mobKiller.setTargetMobOriginalPos(target.position());
            queuePathToTarget(mobKiller, true);
            return this;
        }

        // Ensure Pathfinder engine is active
        if (!Pathfinder.getInstance().isRunning()) {
            if (!repathDelay.isScheduled() || repathDelay.passed()) {
                if (++pathAttempts > MAX_REPATH_ATTEMPTS) {
                    log("Pathfinding stopped too many times. Re-choosing mob.");
                    mobKiller.blacklistTargetMob();
                    Pathfinder.getInstance().stop();
                    return new FindMobState();
                }
                queuePathToTarget(mobKiller, false);
            }
        }

        if (timeout.passed()) {
            log("Pathfinding timeout. Re-choosing target mob.");
            mobKiller.blacklistTargetMob();
            Pathfinder.getInstance().stop();
            return new FindMobState();
        }

        return this;
    }

    @Override
    public void onEnd(AutoMobKiller mobKiller) {
        // Pathfinder cleanup handled by transition
    }

    private boolean isInKillRange(AutoMobKiller mobKiller) {
        if (mc.player == null || mobKiller.getTargetMob() == null) {
            return false;
        }
        return mc.player.distanceToSqr(mobKiller.getTargetMob()) <= ENTER_KILL_RANGE_SQ;
    }

    private void queuePathToTarget(AutoMobKiller mobKiller, boolean forceRefreshApproachTarget) {
        Pathfinder pathfinder = Pathfinder.getInstance();
        BlockPos approachTarget = mobKiller.getApproachBlockForTarget(forceRefreshApproachTarget);
        if (approachTarget == null) {
            return;
        }

        boolean sameQueuedTarget = approachTarget.equals(lastQueuedTarget);
        if (!sameQueuedTarget || !pathfinder.isRunning()) {
            pathfinder.stopAndRequeue(approachTarget);
            lastQueuedTarget = approachTarget;
        }
        if (!pathfinder.isRunning()) {
            pathfinder.start();
        }
        repathDelay.schedule(REPATH_DELAY_MS);
    }
}
