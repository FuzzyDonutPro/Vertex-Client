package com.vertexai.pathing.aim;

import net.minecraft.client.Minecraft;
import java.io.File;
import java.util.Random;

/**
 * HumanAimSimulator — Provides humanized aim smoothing and anti-cheat bypass jitter.
 */
public class HumanAimSimulator {

    private static RotationProfile loadedProfile;
    private static final Random random = new Random();

    public static void loadProfile() {
        File dir = new File(Minecraft.getInstance().gameDirectory, "config/vertex/aim_profiles");
        File file = new File(dir, "recorded_aim.json");
        if (file.exists()) {
            loadedProfile = RotationProfile.load(file);
        }
    }

    public static float[] getNextAngle(float currentYaw, float currentPitch, float targetYaw, float targetPitch) {
        return getNextAngle(currentYaw, currentPitch, targetYaw, targetPitch, false);
    }

    public static float[] getNextAnglePathfinding(float currentYaw, float currentPitch, float targetYaw, float targetPitch) {
        return getNextAngle(currentYaw, currentPitch, targetYaw, targetPitch, true);
    }

    public static float[] getNextAngle(float currentYaw, float currentPitch, float targetYaw, float targetPitch, boolean isPathfinding) {
        float newYaw;
        float newPitch;

        if (loadedProfile == null || loadedProfile.ticks.isEmpty()) {
            // Fallback to basic algorithmic aim if no profile is loaded
            float dYaw = com.vertexai.util.AngleUtil.getNeededYawChange(currentYaw, targetYaw);
            float dPitch = targetPitch - currentPitch;

            // Smooth interpolation with ±10% random variance (breaks fixed exponential decay pattern)
            float baseStep = isPathfinding ? 0.3f : 0.2f;
            float variance = (random.nextFloat() * 0.2f - 0.1f) * baseStep; // ±10%
            float step = baseStep + variance;
            newYaw = currentYaw + (dYaw * step);
            newPitch = currentPitch + (dPitch * step);
        } else {
            // 1. Calculate remaining distance
            float dYaw = com.vertexai.util.AngleUtil.getNeededYawChange(currentYaw, targetYaw);
            float dPitch = targetPitch - currentPitch;

            // 2. If we are very close, snap to target
            if (Math.abs(dYaw) < 0.5f && Math.abs(dPitch) < 0.5f) {
                newYaw = targetYaw;
                newPitch = targetPitch;
            } else {
                // 3. Sample a random tick from the recorded organic profile
                RotationProfile.TickData sample = loadedProfile.ticks.get(random.nextInt(loadedProfile.ticks.size()));

                // 4. Scale the sampled delta based on direction
                float appliedYawDelta = Math.abs(sample.deltaYaw) * Math.signum(dYaw);
                float appliedPitchDelta = Math.abs(sample.deltaPitch) * Math.signum(dPitch);

                // Cap movement to prevent overshooting
                if (Math.abs(appliedYawDelta) > Math.abs(dYaw)) appliedYawDelta = dYaw;
                if (Math.abs(appliedPitchDelta) > Math.abs(dPitch)) appliedPitchDelta = dPitch;

                newYaw = currentYaw + appliedYawDelta;
                newPitch = currentPitch + appliedPitchDelta;
            }
        }

        // === Grim Baritone Check Bypass ===
        // Grim Baritone check flags: deltaXRot == 0 AND 0 < deltaPitch < 1 (sub-degree pitch) for 8+ ticks.
        // Solution: Inject micro-noise into yaw if yaw is static while pitch is adjusting.
        float finalDeltaYaw = newYaw - currentYaw;
        float finalDeltaPitch = Math.abs(newPitch - currentPitch);

        if (finalDeltaYaw == 0f && finalDeltaPitch > 0f && finalDeltaPitch < 1.0f) {
            // Only jitter ~30% of eligible ticks — intermittent noise is far harder to pattern-match
            if (random.nextFloat() < 0.30f) {
                float maxJitter = isPathfinding ? 0.008f : 0.03f;
                float jitter = (random.nextFloat() * (maxJitter - 0.001f) + 0.001f) * (random.nextBoolean() ? 1f : -1f);
                newYaw += jitter;
            }
        }

        // === BadPacketsD Bypass ===
        // Clamp pitch to strictly [-90, 90]
        newPitch = Math.max(-90.0f, Math.min(90.0f, newPitch));

        return new float[]{newYaw, newPitch};
    }
}
