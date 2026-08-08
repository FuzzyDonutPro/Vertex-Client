package com.vertexai.macro.features.misc;

import lombok.Getter;
import com.vertexai.macro.AbstractFeature;
import net.minecraft.world.phys.Vec3;

public class LagDetector extends AbstractFeature {

    public static final LagDetector instance = new LagDetector();
    public static LagDetector getInstance() { return instance; }

    private long lastPacketTime = System.currentTimeMillis();
    private Vec3 lastPacketPosition = new Vec3(0, 0, 0);
    private boolean isLagging = false;

    @Override
    public String getName() {
        return "LagDetector";
    }

    public void onPacketReceive() {
        this.lastPacketTime = System.currentTimeMillis();
        if (mc.player != null) {
            this.lastPacketPosition = mc.player.position();
        }
        if (isLagging) {
            isLagging = false;
            log("Server lag resolved. Connection stable.");
        }
    }

    @Override
    protected void onTick() {
        if (mc.player == null) return;
        long delta = System.currentTimeMillis() - lastPacketTime;
        if (delta > 2500 && !isLagging) {
            isLagging = true;
            warn("Server lag detected (" + delta + "ms since last packet)! Pausing movement...");
        }
    }

    public Vec3 getLastPacketPosition() {
        if (mc.player != null && (lastPacketPosition.x == 0 && lastPacketPosition.y == 0 && lastPacketPosition.z == 0)) {
            return mc.player.position();
        }
        return lastPacketPosition;
    }

    public long getTimeSinceLastTick() {
        return Math.max(0, System.currentTimeMillis() - lastPacketTime);
    }

    public boolean isServerLagging() {
        return isLagging;
    }
}
