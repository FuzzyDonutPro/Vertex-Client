package com.vertexai.pathfinder.calculate;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * FluidAndFlyingPathfinder — High-precision 3D pathfinder with accurate Minecraft physics
 * friction coefficients for Air, Water, Lava, and Ice traversal.
 */
public class FluidAndFlyingPathfinder {

    private static final FluidAndFlyingPathfinder instance = new FluidAndFlyingPathfinder();

    public static FluidAndFlyingPathfinder getInstance() {
        return instance;
    }

    // Accurate Minecraft Physics Friction Values
    public static final float AIR_FRICTION = 0.91f;       // Air Drag
    public static final float FLYING_FRICTION = 0.98f;    // Flying Deceleration Factor
    public static final float WATER_FRICTION = 0.80f;     // Water Drag
    public static final float LAVA_FRICTION = 0.50f;      // Lava Drag Factor
    public static final float ICE_FRICTION = 0.98f;       // Ice Surface Friction
    public static final float GROUND_FRICTION = 0.60f;    // Default Block Surface Friction

    private final Minecraft mc = Minecraft.getInstance();

    /**
     * Calculates the friction coefficient for the player's current environment state.
     */
    public float getCurrentEnvironmentFriction() {
        Player player = mc.player;
        if (player == null) return GROUND_FRICTION;

        if (player.isInWater()) {
            return WATER_FRICTION;
        } else if (player.isInLava()) {
            return LAVA_FRICTION;
        } else if (player.getAbilities().flying) {
            return FLYING_FRICTION;
        } else if (!player.onGround()) {
            return AIR_FRICTION;
        } else {
            BlockPos posUnder = player.blockPosition().below();
            if (mc.level != null) {
                var blockState = mc.level.getBlockState(posUnder);
                if (blockState.getBlock().toString().contains("ice")) {
                    return ICE_FRICTION;
                }
            }
            return GROUND_FRICTION;
        }
    }

    /**
     * Predicts stopping position given current velocity and environmental friction.
     */
    public Vec3 predictStoppingPosition(Vec3 currentPos, Vec3 velocity, int ticks) {
        float friction = getCurrentEnvironmentFriction();
        double vx = velocity.x;
        double vy = velocity.y;
        double vz = velocity.z;

        double px = currentPos.x;
        double py = currentPos.y;
        double pz = currentPos.z;

        for (int i = 0; i < ticks; i++) {
            px += vx;
            py += vy;
            pz += vz;

            vx *= friction;
            vy *= friction;
            vz *= friction;

            if (Math.abs(vx) < 0.005 && Math.abs(vy) < 0.005 && Math.abs(vz) < 0.005) {
                break;
            }
        }
        return new Vec3(px, py, pz);
    }

    /**
     * Calculates 3D smooth waypoints through air/water medium to destination.
     */
    public List<Vec3> calculate3DWaypoints(Vec3 start, Vec3 target, double stepSize) {
        List<Vec3> waypoints = new ArrayList<>();
        double distance = start.distanceTo(target);
        if (distance <= stepSize) {
            waypoints.add(target);
            return waypoints;
        }

        int steps = (int) Math.ceil(distance / stepSize);
        Vec3 direction = target.subtract(start).normalize();

        for (int i = 1; i <= steps; i++) {
            double currentDist = Math.min(i * stepSize, distance);
            Vec3 point = start.add(direction.scale(currentDist));
            waypoints.add(point);
        }
        return waypoints;
    }

    /**
     * Returns the maximum jump height clearance for the pathfinder (incorporating active Jump Boost status).
     */
    public float getMaxJumpClearanceHeight() {
        return com.vertexai.macro.impl.SlayerMacro.LaserEvasionSolver.getInstance().getJumpBoostHeightMultiplier();
    }
}
