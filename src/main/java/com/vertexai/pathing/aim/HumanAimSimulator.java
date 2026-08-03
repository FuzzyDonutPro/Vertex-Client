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
        if (loadedProfile != null && !loadedProfile.ticks.isEmpty()) return;

        File gameDir = Minecraft.getInstance().gameDirectory;
        File[] candidateFiles = new File[] {
            new File(gameDir, "config/vertex/aim_profiles/recorded_aim.json"),
            new File(gameDir, "config/vertex/recorded_aim.json"),
            new File(gameDir, "config/vertex/rotations.json"),
            new File(gameDir, "config/vertex/recorded_rotations.json"),
            new File(gameDir, "config/vertex/aim_profile.json")
        };

        for (File candidate : candidateFiles) {
            if (candidate.exists()) {
                RotationProfile prof = RotationProfile.load(candidate);
                if (prof != null && prof.ticks != null && !prof.ticks.isEmpty()) {
                    loadedProfile = prof;
                    com.vertexai.util.Logger.sendLog("[Humanizer] Loaded trained rotation profile from " + candidate.getName() + " (" + prof.ticks.size() + " ticks)");
                    break;
                }
            }
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

                // 4. Scale the sampled delta based on direction and distance ratio
                float yawRatio = Math.abs(dYaw) > 0.01f ? Math.min(1.0f, Math.abs(dYaw) / 30.0f) : 1.0f;
                float pitchRatio = Math.abs(dPitch) > 0.01f ? Math.min(1.0f, Math.abs(dPitch) / 20.0f) : 1.0f;
                
                float appliedYawDelta = Math.max(Math.abs(dYaw) * 0.25f, Math.abs(sample.deltaYaw) * yawRatio) * Math.signum(dYaw);
                float appliedPitchDelta = Math.max(Math.abs(dPitch) * 0.25f, Math.abs(sample.deltaPitch) * pitchRatio) * Math.signum(dPitch);

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
