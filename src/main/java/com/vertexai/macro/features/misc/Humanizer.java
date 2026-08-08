package com.vertexai.macro.features.misc;

import lombok.Getter;
import com.vertexai.macro.AbstractFeature;
import com.vertexai.handler.RotationHandler;
import com.vertexai.macro.MacroManager;

import java.util.Random;

/**
 * Humanizer â€” Applies organic camera rotation noise (drift & micro-jitter)
 * during active macro execution to bypass anticheat rotation heuristics,
 * safely paused near walls & active pathfinding.
 */
public class Humanizer extends AbstractFeature {

    @Getter
    public static final Humanizer instance = new Humanizer();

    private final Random random = new Random();
    private long lastJitterTime = 0;

    public Humanizer() {
        this.enabled = true;
    }

    @Override
    public String getName() {
        return "Humanizer";
    }

    @Override
    protected void onTick() {
        if (mc.player == null || mc.level == null) return;
        if (!MacroManager.getInstance().isRunning()) return;

        // Safety 1: Do not interrupt active precision smooth rotations
        if (RotationHandler.getInstance().isEnabled()) return;

        // Safety 2: Do not apply jitter if colliding with walls or near obstacles
        if (mc.player.horizontalCollision) return;

        // Apply ultra-subtle micro-noise (Â±0.04Â° yaw, Â±0.03Â° pitch)
        long now = System.currentTimeMillis();
        if (now - lastJitterTime > 250 + random.nextInt(150)) {
            lastJitterTime = now;
            float yawJitter = (random.nextFloat() - 0.5f) * 0.08f;  // Â±0.04Â° micro drift
            float pitchJitter = (random.nextFloat() - 0.5f) * 0.06f; // Â±0.03Â° micro drift

            mc.player.setYRot(mc.player.getYRot() + yawJitter);
            mc.player.setXRot(mc.player.getXRot() + pitchJitter);
        }
    }
}
