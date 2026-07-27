package com.vertexai.macro.impl.SlayerMacro;

import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

/**
 * LaserEvasionSolver — Handles Voidgloom Seraph T4 Laser Ring Evasion and Jump Boost Physics.
 * 
 * 1. Laser Evasion: Detects expanding laser rings and triggers jump / AOTE teleport to clear the ring.
 * 2. Jump Boost Physics: Calculates jump boost amplifier and height multipliers for pathfinder clearance.
 */
public class LaserEvasionSolver {

    private static final LaserEvasionSolver instance = new LaserEvasionSolver();

    public static LaserEvasionSolver getInstance() {
        return instance;
    }

    private final Minecraft mc = Minecraft.getInstance();
    private long lastJumpTime = 0;

    /**
     * Calculates active Jump Boost height multiplier for Pathfinder obstacle clearance.
     */
    public float getJumpBoostHeightMultiplier() {
        if (mc.player == null) return 1.25f;

        var effect = mc.player.getEffect(MobEffects.JUMP_BOOST);
        if (effect != null) {
            int amplifier = effect.getAmplifier() + 1;
            return 1.25f + (0.1f * amplifier);
        }
        return 1.25f;
    }

    /**
     * Checks if the player is currently under Jump Boost status.
     */
    public boolean hasJumpBoost() {
        return mc.player != null && mc.player.hasEffect(MobEffects.JUMP_BOOST);
    }

    /**
     * Scans for Voidgloom Seraph laser ring beacons and handles jump evasion timing.
     */
    public void tickLaserEvasion() {
        if (mc.player == null || mc.level == null) return;
        
        // Also run Demon Beacon gaze disarm solver
        DemonBeaconSolver.getInstance().tickDemonBeaconSolver();

        if (System.currentTimeMillis() - lastJumpTime < 600) return;

        Entity laserRing = findNearbyLaserRing();
        if (laserRing != null) {
            double distance = mc.player.distanceTo(laserRing);

            // Ring impact window (2.0 to 4.5 blocks away)
            if (distance >= 2.0 && distance <= 4.5) {
                // Only jump if grounded — airborne jump packets flag Grim NoFall
                if (mc.player.onGround()) {
                    mc.player.jumpFromGround();
                    lastJumpTime = System.currentTimeMillis();
                }
            }
        }
    }

    private Entity findNearbyLaserRing() {
        if (mc.level == null || mc.player == null) return null;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ArmorStand armorStand) {
                if (armorStand.getCustomName() != null) {
                    String name = armorStand.getCustomName().getString().toLowerCase();
                    if (name.contains("beacon") || name.contains("ring") || name.contains("laser") || name.contains("voidgloom")) {
                        return armorStand;
                    }
                }
            }
        }
        return null;
    }
}
