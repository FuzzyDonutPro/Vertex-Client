package com.vertexai.feature.impl;

import lombok.Getter;
import com.vertexai.feature.AbstractFeature;
import com.vertexai.handler.RotationHandler;
import com.vertexai.macro.MacroManager;

import java.util.Random;

/**
 * Humanizer — Applies organic camera rotation noise (drift & micro-jitter)
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

        // Apply ultra-subtle micro-noise (±0.04° yaw, ±0.03° pitch)
        long now = System.currentTimeMillis();
        if (now - lastJitterTime > 250 + random.nextInt(150)) {
            lastJitterTime = now;
            float yawJitter = (random.nextFloat() - 0.5f) * 0.08f;  // ±0.04° micro drift
            float pitchJitter = (random.nextFloat() - 0.5f) * 0.06f; // ±0.03° micro drift

            mc.player.setYRot(mc.player.getYRot() + yawJitter);
            mc.player.setXRot(mc.player.getXRot() + pitchJitter);
        }
    }
}
