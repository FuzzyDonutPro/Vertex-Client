package com.vertexai.macro.impl.SlayerMacro;

import com.vertexai.handler.RotationHandler;
import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.helper.Clock;
import com.vertexai.util.helper.RotationConfiguration;
import com.vertexai.util.helper.Target;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * DemonBeaconSolver — Detects Voidgloom Seraph Demon Beacons, auto-aims camera at the beacon,
 * and interacts (right-clicks) with the beacon to disarm it before detonation.
 * Includes Line-of-Sight raytrace validation to prevent clicking through solid obstacles.
 */
public class DemonBeaconSolver {

    private static final DemonBeaconSolver instance = new DemonBeaconSolver();

    public static DemonBeaconSolver getInstance() {
        return instance;
    }

    private final Minecraft mc = Minecraft.getInstance();
    private final Clock interactClock = new Clock();

    /**
     * Ticks Demon Beacon detection, aiming, and right-click interaction disarm.
     */
    public void tickDemonBeaconSolver() {
        if (mc.player == null || mc.level == null) return;
        if (interactClock.isScheduled() && !interactClock.passed()) return;

        Entity activeBeacon = findActiveDemonBeacon();
        if (activeBeacon != null) {
            Vec3 beaconPos = activeBeacon.position().add(0, activeBeacon.getEyeHeight() * 0.5, 0);

            // 1. Ease camera directly onto the Demon Beacon
            RotationHandler.getInstance().easeTo(new RotationConfiguration(new Target(beaconPos), 90, null));

            // 2. Line of Sight & Reach Verification (3.0 blocks max reach)
            if (mc.player.distanceTo(activeBeacon) <= 3.0f && hasLineOfSight(beaconPos)) {
                if (mc.gameMode != null) {
                    mc.gameMode.interact(mc.player, activeBeacon, new net.minecraft.world.phys.EntityHitResult(activeBeacon), net.minecraft.world.InteractionHand.MAIN_HAND);
                }
                KeyBindUtil.rightClick();
                interactClock.schedule(200);
            }
        }
    }

    private boolean hasLineOfSight(Vec3 targetPos) {
        if (mc.player == null || mc.level == null) return false;
        Vec3 eyePos = mc.player.getEyePosition(1.0f);
        var hitResult = mc.level.clip(new ClipContext(eyePos, targetPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        return hitResult.getType() == HitResult.Type.MISS || hitResult.getLocation().distanceTo(targetPos) <= 0.5;
    }

    private Entity findActiveDemonBeacon() {
        if (mc.level == null || mc.player == null) return null;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ArmorStand armorStand) {
                if (armorStand.getCustomName() != null) {
                    String name = armorStand.getCustomName().getString().toLowerCase();
                    if (name.contains("beacon") || name.contains("demon") || name.contains("throw") || name.contains("siphon")) {
                        return armorStand;
                    }
                }
            }
        }
        return null;
    }
}
