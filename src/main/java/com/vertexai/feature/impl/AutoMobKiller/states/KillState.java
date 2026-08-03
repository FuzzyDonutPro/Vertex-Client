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
import net.minecraft.world.entity.LivingEntity;
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
    private final Clock rogueTimer = new Clock();
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

        // Auto Rogue Sword Speed Boost
        if (com.vertexai.Vertex.config().combat.autoRogueSword) {
            if (!rogueTimer.isScheduled() || rogueTimer.passed()) {
                int rogueSlot = InventoryUtil.getHotbarSlotOfItem("Rogue");
                if (rogueSlot != -1) {
                    int mana = com.vertexai.util.ManaTracker.getCurrentMana();
                    if (mana >= 50) {
                        InventoryUtil.holdItem("Rogue");
                        KeyBindUtil.rightClick();
                        rogueTimer.schedule(3000L);
                        log("Auto Rogue Sword speed boost! Mana: " + mana);
                    }
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

        // Smooth rotation through RotationHandler anti-cheat humanized engine
        RotationHandler.getInstance().easeTo(new RotationConfiguration(
                new com.vertexai.util.helper.Target(mobKiller.getTargetMob()),
                90L,
                null
        ));

        // Crosshair Raycast Target Lock: If crosshair touches ANY valid mob, lock onto it instantly
        if (mc.hitResult instanceof EntityHitResult hitResult && hitResult.getEntity() instanceof LivingEntity hitLiving) {
            if (hitLiving.isAlive() && mc.player.distanceToSqr(hitLiving) <= MELEE_RANGE_SQ) {
                String hitName = ChatFormatting.stripFormatting(hitLiving.getName().getString().toLowerCase(java.util.Locale.ROOT));
                boolean isValidTarget = mobKiller.getMobsToKill().stream().anyMatch(t -> hitName.contains(t.toLowerCase(java.util.Locale.ROOT)));
                if (isValidTarget && !mobKiller.getBlacklistedMobs().contains(hitLiving)) {
                    mobKiller.setTargetMob(hitLiving);
                }
            }
        }

        // Always push forward to close distance if not directly touching
        if (distanceSq > 2.25) {
            KeyBindUtil.setKeyBindState(mc.options.keyUp, true);
        }

        // Hop over 1-block obstacles when colliding
        if (mc.player.horizontalCollision && mc.player.onGround()) {
            KeyBindUtil.setKeyBindState(mc.options.keyJump, true);
        }

        // Strictly enforce 3.0-block vanilla reach limit (3.0^2 = 9.0)
        if (distanceSq > 9.0) {
            return this;
        }

        // Require crosshair/aim alignment on target mob (vanilla reach 3.0 blocks max)
        boolean aimAlignedOnMob = (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living && living.isAlive())
                || (hasLineOfSight && Math.abs(com.vertexai.util.AngleUtil.getNeededYawChange(mc.player.getYRot(), com.vertexai.util.AngleUtil.getRotationYaw(mobKiller.getTargetMob().getEyePosition(1.0f)))) < 25.0f);

        if (!aimAlignedOnMob) {
            return this;
        }

        if (attackDelay.isScheduled() && !attackDelay.passed()) {
            return this;
        }

        // Fast & smooth humanized click dispatch (~10-12 CPS)
        KeyBindUtil.leftClick();
        attackDelay.schedule(70 + (long)(Math.random() * 35));
        closeRangeStuckTimer.reset();
        
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
