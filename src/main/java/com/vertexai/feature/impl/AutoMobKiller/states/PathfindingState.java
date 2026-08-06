package com.vertexai.feature.impl.AutoMobKiller.states;

import com.vertexai.Vertex;
import com.vertexai.feature.impl.AutoMobKiller.AutoMobKiller;
import com.vertexai.feature.impl.Pathfinder;
import com.vertexai.handler.RotationHandler;
import com.vertexai.util.helper.Clock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import java.util.List;

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

        // Auto Rogue Sword Speed Boost
        if (com.vertexai.Vertex.config().combat.autoRogueSword) {
            if (!mobKiller.getRogueTimer().isScheduled() || mobKiller.getRogueTimer().passed()) {
                int rogueSlot = com.vertexai.util.InventoryUtil.getHotbarSlotOfItem("Rogue");
                if (rogueSlot != -1) {
                    int mana = com.vertexai.util.ManaTracker.getCurrentMana();
                    if (mana >= 50) {
                        log("Attempting to use Rogue Sword from slot " + rogueSlot + " (Mana: " + mana + ")");
                        if (com.vertexai.util.UseItemAbility.useItemAbility("Rogue", rogueSlot, 150)) {
                            mobKiller.getRogueTimer().schedule(35000L);
                            log("Auto Rogue Sword speed boost! Mana: " + mana);
                        } else {
                            log("UseItemAbility failed for Rogue Sword");
                        }
                    } else {
                        log("Mana too low for Rogue Sword: " + mana);
                    }
                } else {
                    log("Rogue Sword not found in hotbar");
                }
            }
        }

        // Dynamic nearest target check: Switch if a mob is significantly closer
        List<LivingEntity> mobs = com.vertexai.util.EntityUtil.getEntities(mobKiller.getMobsToKill(), mobKiller.getBlacklistedMobs());
        double currentDistSq = mc.player.distanceToSqr(target);
        for (LivingEntity m : mobs) {
            if (m != null && m.isAlive() && m != target) {
                double mDistSq = mc.player.distanceToSqr(m);
                if (mDistSq + 16.0 < currentDistSq || (mDistSq <= 25.0 && mDistSq < currentDistSq - 4.0)) {
                    mobKiller.updateTargetMob(m);
                    target = m;
                    mobKiller.setTargetMobOriginalPos(m.position());
                    queuePathToTarget(mobKiller, true);
                    break;
                }
            }
        }

        // Dynamic live tracking: Only repath if the mob moves significantly (>= 2.2 blocks) from last queued target position
        BlockPos approachTarget = mobKiller.getApproachBlockForTarget(false);
        if (approachTarget != null) {
            if (lastQueuedTarget == null || lastQueuedTarget.distSqr(approachTarget) >= 5.0) {
                mobKiller.setTargetMobOriginalPos(target.position());
                queuePathToTarget(mobKiller, true);
            }
        }

        // Auto-reroute if Pathfinder engine cancels or fails a route
        if (Pathfinder.getInstance().failed()) {
            log("Pathfinder cancelled/failed route. Rerouting automatically...");
            lastQueuedTarget = null;
            if (++pathAttempts > 3) {
                log("Target unreachable after multiple route attempts. Re-choosing nearest mob.");
                mobKiller.blacklistTargetMob();
                Pathfinder.getInstance().stop();
                return new FindMobState();
            }
            queuePathToTarget(mobKiller, true);
        }

        // Ensure Pathfinder engine is active
        if (!Pathfinder.getInstance().isRunning() && !Pathfinder.getInstance().failed()) {
            if (!repathDelay.isScheduled() || repathDelay.passed()) {
                queuePathToTarget(mobKiller, false);
            }
        }

        // Only apply direct fallback drive if Pathfinder is not actively navigating
        if (!Pathfinder.getInstance().isRunning()) {
            RotationHandler.getInstance().easeTo(new com.vertexai.util.helper.RotationConfiguration(
                    new com.vertexai.util.helper.Target(target),
                    100L,
                    null
            ));
            com.vertexai.util.KeyBindUtil.setKeyBindState(mc.options.keyUp, true);
            if (mc.player.horizontalCollision && mc.player.onGround()) {
                com.vertexai.util.KeyBindUtil.setKeyBindState(mc.options.keyJump, true);
            } else {
                com.vertexai.util.KeyBindUtil.setKeyBindState(mc.options.keyJump, false);
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
        return mc.player.distanceToSqr(mobKiller.getTargetMob()) <= ENTER_KILL_RANGE_SQ
                && mc.player.hasLineOfSight(mobKiller.getTargetMob());
    }

    private void queuePathToTarget(AutoMobKiller mobKiller, boolean forceRefreshApproachTarget) {
        Pathfinder pathfinder = Pathfinder.getInstance();
        BlockPos approachTarget = mobKiller.getApproachBlockForTarget(forceRefreshApproachTarget);
        if (approachTarget == null) {
            return;
        }

        boolean sameQueuedTarget = approachTarget.equals(lastQueuedTarget);
        if (!sameQueuedTarget || !pathfinder.isRunning() || pathfinder.failed()) {
            pathfinder.stopAndRequeue(approachTarget);
            lastQueuedTarget = approachTarget;
            pathfinder.start();
        } else if (!pathfinder.isRunning()) {
            pathfinder.start();
        }
        repathDelay.schedule(REPATH_DELAY_MS);
    }
}
