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

        float dYaw = com.vertexai.util.AngleUtil.getNeededYawChange(currentYaw, targetYaw);
        float dPitch = targetPitch - currentPitch;

        if (loadedProfile == null || loadedProfile.ticks.isEmpty()) {
            // Humanized organic S-curve interpolation
            float absYaw = Math.abs(dYaw);
            float absPitch = Math.abs(dPitch);

            // Smoothstep curve for deceleration near target
            float yawProgress = Math.min(1.0f, absYaw / (isPathfinding ? 45.0f : 25.0f));
            float pitchProgress = Math.min(1.0f, absPitch / 20.0f);
            float yawCurve = yawProgress * yawProgress * (3.0f - 2.0f * yawProgress);
            float pitchCurve = pitchProgress * pitchProgress * (3.0f - 2.0f * pitchProgress);

            float baseStepYaw = isPathfinding ? (0.16f + yawCurve * 0.26f) : (0.14f + yawCurve * 0.28f);
            float baseStepPitch = isPathfinding ? (0.18f + pitchCurve * 0.24f) : (0.15f + pitchCurve * 0.27f);

            // Micro-variance per tick (±8%)
            float varianceYaw = (random.nextFloat() * 0.16f - 0.08f) * baseStepYaw;
            float variancePitch = (random.nextFloat() * 0.16f - 0.08f) * baseStepPitch;

            float stepYaw = Math.max(0.08f, Math.min(0.85f, baseStepYaw + varianceYaw));
            float stepPitch = Math.max(0.08f, Math.min(0.85f, baseStepPitch + variancePitch));

            newYaw = currentYaw + (dYaw * stepYaw);
            newPitch = currentPitch + (dPitch * stepPitch);

            // Subtle pitch breathing effect (simulates natural mouse hold variance)
            double breathing = Math.sin(System.currentTimeMillis() * 0.003) * 0.06;
            newPitch += (float) breathing;
        } else {
            // 1. Calculate remaining distance
            if (Math.abs(dYaw) < 0.3f && Math.abs(dPitch) < 0.3f) {
                newYaw = targetYaw;
                newPitch = targetPitch;
            } else {
                // 2. Sample from recorded organic profile
                RotationProfile.TickData sample = loadedProfile.ticks.get(random.nextInt(loadedProfile.ticks.size()));

                float yawRatio = Math.abs(dYaw) > 0.01f ? Math.min(1.0f, Math.abs(dYaw) / 30.0f) : 1.0f;
                float pitchRatio = Math.abs(dPitch) > 0.01f ? Math.min(1.0f, Math.abs(dPitch) / 20.0f) : 1.0f;
                
                float appliedYawDelta = Math.max(Math.abs(dYaw) * 0.22f, Math.abs(sample.deltaYaw) * yawRatio) * Math.signum(dYaw);
                float appliedPitchDelta = Math.max(Math.abs(dPitch) * 0.22f, Math.abs(sample.deltaPitch) * pitchRatio) * Math.signum(dPitch);

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
        // Note: We removed the global 36.0f downward pitch clamp here because it prevented aiming at low enemies (like Silverfish).
        // The pathing downward pitch cap is now strictly handled at the PathExecutor level, allowing combat aiming to be unrestricted.

        return new float[]{newYaw, newPitch};
    }
}
