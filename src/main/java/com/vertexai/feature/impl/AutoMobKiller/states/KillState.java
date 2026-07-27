package com.vertexai.feature.impl.AutoMobKiller.states;

import com.vertexai.feature.impl.AutoMobKiller.AutoMobKiller;
import com.vertexai.feature.impl.Pathfinder;
import com.vertexai.handler.RotationHandler;
import com.vertexai.util.InventoryUtil;
import com.vertexai.util.KeyBindUtil;
import net.minecraft.ChatFormatting;
import com.vertexai.util.helper.Clock;
import com.vertexai.util.helper.RotationConfiguration;
import com.vertexai.util.helper.Target;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.EntityHitResult;

public class KillState implements AutoMobKillerState {

    private static final double MELEE_RANGE_SQ = 9.0;
    private static final long LOST_SIGHT_REPATH_DELAY_MS = 150L;
    private static final long CHASE_REPATH_INTERVAL_MS = 100L;
    private static final long LAST_SEEN_TIMEOUT_MS = 180L;
    private static final long CLOSE_RANGE_STUCK_TIMEOUT_MS = 900L;
    private static final long REAIM_INTERVAL_MS = 240L;
    private static final long REAIM_ROTATION_TIME_MS = 90L;

    private final Minecraft mc = Minecraft.getInstance();
    private final Clock attackDelay = new Clock();
    private final Clock lostSightTimer = new Clock();
    private final Clock lastSeenTimer = new Clock();
    private final Clock closeRangeStuckTimer = new Clock();
    private final Clock reaimTimer = new Clock();
    private final Clock chaseRepathTimer = new Clock();
    private final Clock strafeTimer = new Clock();
    private boolean strafeDirectionLeft = true;
    private final Clock wTapTimer = new Clock();
    private boolean wTapping = false;
    private BlockPos lastChaseTarget = null;

    @Override
    public void onStart(AutoMobKiller mobKiller) {
        log("Entering Kill State");
        attackDelay.reset();
        lostSightTimer.reset();
        lastSeenTimer.reset();
        closeRangeStuckTimer.reset();
        reaimTimer.reset();
        chaseRepathTimer.reset();
        lastChaseTarget = null;
    }

    @Override
    public AutoMobKillerState onTick(AutoMobKiller mobKiller) {
        if (mobKiller.getTargetMob() == null) {
            Pathfinder.getInstance().stop();
            return new FindMobState();
        }

        if (!mobKiller.getTargetMob().isAlive()) {
            Pathfinder.getInstance().stop();
            RotationHandler.getInstance().stop();
            return new FindMobState();
        }

        // Auto-Heal Logic
        if (com.vertexai.Vertex.config().combat.autoHealEnabled && !com.vertexai.Vertex.config().combat.healingItem.isEmpty()) {
            float healthPercent = (mc.player.getHealth() / mc.player.getMaxHealth()) * 100f;
            if (healthPercent < com.vertexai.Vertex.config().combat.autoHealThreshold) {
                if (InventoryUtil.holdItem(com.vertexai.Vertex.config().combat.healingItem)) {
                    if (!attackDelay.isScheduled() || attackDelay.passed()) {
                        KeyBindUtil.rightClick();
                        attackDelay.schedule(150 + (long)(Math.random() * 100)); // Delay after healing
                        log("Auto-Heal triggered! Health: " + String.format("%.1f%%", healthPercent));
                    }
                    // Release movement keys while healing to avoid sprinting past targets
                    KeyBindUtil.setKeyBindState(mc.options.keyUp, false);
                    KeyBindUtil.setKeyBindState(mc.options.keyLeft, false);
                    KeyBindUtil.setKeyBindState(mc.options.keyRight, false);
                    return this; // Skip combat logic for this tick
                } else {
                    com.vertexai.util.Logger.sendWarning("Healing item '" + com.vertexai.Vertex.config().combat.healingItem + "' not found in hotbar!");
                }
            }
        }

        // Dynamic Weapon Swapping
        String mobName = ChatFormatting.stripFormatting(mobKiller.getTargetMob().getName().getString());
        boolean usePickaxe = mobName.contains("Ice Walker") || mobName.contains("Glacite Walker");
        String desiredWeapon = usePickaxe ? mobKiller.getPickaxeWeaponName() : mobKiller.getWeaponName();
        if (desiredWeapon != null && !desiredWeapon.isEmpty()) {
            InventoryUtil.holdItem(desiredWeapon);
        }

        double distanceSq = mc.player.distanceToSqr(mobKiller.getTargetMob());
        boolean inMeleeRange = distanceSq <= MELEE_RANGE_SQ;
        boolean hasLineOfSight = mc.player.hasLineOfSight(mobKiller.getTargetMob());
        double targetYDelta = mobKiller.getTargetMob().getY() - mc.player.getY();
        long lastSeenTimeoutMs = mobKiller.getSlayerProfile() == AutoMobKiller.SlayerProfile.GOBLIN ? 420L : LAST_SEEN_TIMEOUT_MS;
        long closeRangeStuckTimeoutMs = mobKiller.getSlayerProfile() == AutoMobKiller.SlayerProfile.GOBLIN ? 1_250L : CLOSE_RANGE_STUCK_TIMEOUT_MS;

        if (hasLineOfSight) {
            lastSeenTimer.reset();
        } else {
            if (!lastSeenTimer.isScheduled()) {
                lastSeenTimer.schedule(lastSeenTimeoutMs);
            } else if (lastSeenTimer.passed()) {
                mobKiller.blacklistTargetMob();
                Pathfinder.getInstance().stop();
                return new FindMobState();
            }
        }

        if (!hasLineOfSight || !inMeleeRange) {
            if (!lostSightTimer.isScheduled()) {
                lostSightTimer.schedule(LOST_SIGHT_REPATH_DELAY_MS);
            }

            if (lostSightTimer.passed()) {
                if (Pathfinder.getInstance().failed()) {
                    mobKiller.blacklistTargetMob();
                    Pathfinder.getInstance().stop();
                    return new FindMobState();
                }
                chaseTarget(mobKiller);
            }
        } else {
            lostSightTimer.reset();
        }

        if (inMeleeRange && hasLineOfSight) {
            if (!closeRangeStuckTimer.isScheduled()) {
                closeRangeStuckTimer.schedule(closeRangeStuckTimeoutMs);
            } else if (closeRangeStuckTimer.passed()) {
                mobKiller.blacklistTargetMob();
                Pathfinder.getInstance().stop();
                return new FindMobState();
            }
            
            // Advanced Strafing
            if (!strafeTimer.isScheduled() || strafeTimer.passed()) {
                strafeDirectionLeft = Math.random() > 0.5;
                strafeTimer.schedule((long) (400 + Math.random() * 800)); // strafe for 400-1200ms
            }
            KeyBindUtil.setKeyBindState(mc.options.keyLeft, strafeDirectionLeft);
            KeyBindUtil.setKeyBindState(mc.options.keyRight, !strafeDirectionLeft);
            
            // W-Tapping (Sprint resetting)
            if (wTapping && wTapTimer.passed()) {
                KeyBindUtil.setKeyBindState(mc.options.keyUp, true);
                wTapping = false;
            } else if (!wTapping) {
                KeyBindUtil.setKeyBindState(mc.options.keyUp, true); // Keep holding W otherwise
            }
            
        } else {
            closeRangeStuckTimer.reset();
            KeyBindUtil.setKeyBindState(mc.options.keyLeft, false);
            KeyBindUtil.setKeyBindState(mc.options.keyRight, false);
            wTapping = false;
        }

        if (!reaimTimer.isScheduled() || reaimTimer.passed()) {
            RotationHandler.getInstance().easeTo(new RotationConfiguration(
                    new Target(mobKiller.getTargetMob()),
                    REAIM_ROTATION_TIME_MS,
                    null
            ));
            reaimTimer.schedule(REAIM_INTERVAL_MS);
        }

        if (!inMeleeRange || !hasLineOfSight) {
            KeyBindUtil.setKeyBindState(mc.options.keyJump, false);
            return this;
        }

        if (targetYDelta > 2.0 || targetYDelta < -5.0) {
            mobKiller.blacklistTargetMob();
            Pathfinder.getInstance().stop();
            return new FindMobState();
        }

        KeyBindUtil.setKeyBindState(mc.options.keyJump, targetYDelta < -2.0);

        boolean crosshairOnTarget = mc.hitResult instanceof EntityHitResult && ((EntityHitResult) mc.hitResult).getEntity() == mobKiller.getTargetMob();
        if (!crosshairOnTarget) {
            return this;
        }

        if (mc.player.getAttackStrengthScale(0.0F) < 0.92F) {
            return this;
        }

        if (attackDelay.isScheduled() && !attackDelay.passed()) {
            return this;
        }

        KeyBindUtil.leftClick();
        attackDelay.schedule(85 + (long)(Math.random() * 40)); // random delay
        closeRangeStuckTimer.reset();
        
        // Trigger W-Tap sprint reset
        if (inMeleeRange) {
            KeyBindUtil.setKeyBindState(mc.options.keyUp, false);
            wTapping = true;
            wTapTimer.schedule(50 + (long)(Math.random() * 50)); // release W for 50-100ms
        }
        
        return this;
    }

    @Override
    public void onEnd(AutoMobKiller mobKiller) {
        KeyBindUtil.setKeyBindState(mc.options.keyJump, false);
        log("Exiting Kill State");
    }

    private void chaseTarget(AutoMobKiller mobKiller) {
        if (chaseRepathTimer.isScheduled() && !chaseRepathTimer.passed()) {
            return;
        }

        BlockPos chaseTarget = mobKiller.getApproachBlockForTarget(false);
        if (chaseTarget == null) {
            return;
        }

        Pathfinder pathfinder = Pathfinder.getInstance();
        if (!chaseTarget.equals(lastChaseTarget) || !pathfinder.isRunning()) {
            pathfinder.stopAndRequeue(chaseTarget);
            lastChaseTarget = chaseTarget;
        }
        if (!pathfinder.isRunning()) {
            pathfinder.start();
        }
        chaseRepathTimer.schedule(CHASE_REPATH_INTERVAL_MS);
    }

}
