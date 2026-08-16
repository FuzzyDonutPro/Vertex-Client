package com.vertexai.feature.impl;

import com.vertexai.Vertex;
import com.vertexai.feature.AbstractFeature;
import com.vertexai.mixin.client.MinecraftAccessor;
import com.vertexai.util.WorldRenderContextWrapper;

import java.util.concurrent.ThreadLocalRandom;

public class AutoClicker extends AbstractFeature {

    public static AutoClicker instance = new AutoClicker();
    public static AutoClicker getInstance() { return instance; }

    private long lastLeftClickTime = 0;
    private long nextLeftDelay = 0;
    private long lastRightClickTime = 0;
    private long nextRightDelay = 0;

    public AutoClicker() {
        this.enabled = true;
    }

    @Override
    public String getName() {
        return "AutoClicker";
    }

    @Override
    public boolean shouldStartAtLaunch() {
        return true;
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public void start() {
        this.enabled = true;
    }

    @Override
    public void stop() {
        // Persistent background feature - do not kill on macro stop
        this.enabled = true;
    }

    private boolean isLeftClickHeld() {
        if (mc.options != null && mc.options.keyAttack.isDown()) return true;
        if (mc.mouseHandler != null) {
            return mc.mouseHandler.isLeftPressed();
        }
        return false;
    }

    private boolean isRightClickHeld() {
        if (mc.options != null && mc.options.keyUse.isDown()) return true;
        if (mc.mouseHandler != null) {
            return mc.mouseHandler.isRightPressed();
        }
        return false;
    }

    @Override
    protected void onTick() {
        executeClicks();
    }

    @Override
    protected void onWorldRender(WorldRenderContextWrapper context) {
        executeClicks();
    }

    private void executeClicks() {
        if (mc.screen != null || mc.player == null || mc.level == null) {
            return;
        }

        if (Vertex.config() == null || Vertex.config().misc == null) return;

        long now = System.currentTimeMillis();

        // Left AutoClicker — active only when holding down attack key / Left Click
        if (Vertex.config().misc.leftClicker && isLeftClickHeld()) {
            int minCps = Math.max(1, Vertex.config().misc.minLeftCps);
            int maxCps = Math.max(minCps, Vertex.config().misc.maxLeftCps);

            if (now - lastLeftClickTime >= nextLeftDelay) {
                lastLeftClickTime = now;
                double targetCps = ThreadLocalRandom.current().nextDouble(minCps, maxCps + 0.999);
                nextLeftDelay = Math.max(10, (long) (1000.0 / targetCps));

                ((MinecraftAccessor) mc).setAttackCooldown(0);
                ((MinecraftAccessor) mc).invokeStartAttack();
            }
        } else {
            lastLeftClickTime = 0;
            nextLeftDelay = 0;
        }

        // Right AutoClicker — active only when holding down use key / Right Click
        if (Vertex.config().misc.rightClicker && isRightClickHeld()) {
            int minCps = Math.max(1, Vertex.config().misc.minRightCps);
            int maxCps = Math.max(minCps, Vertex.config().misc.maxRightCps);

            if (now - lastRightClickTime >= nextRightDelay) {
                lastRightClickTime = now;
                double targetCps = ThreadLocalRandom.current().nextDouble(minCps, maxCps + 0.999);
                nextRightDelay = Math.max(10, (long) (1000.0 / targetCps));

                ((MinecraftAccessor) mc).setItemUseCooldown(0);
                ((MinecraftAccessor) mc).invokeStartUseItem();
            }
        } else {
            lastRightClickTime = 0;
            nextRightDelay = 0;
        }
    }
}
