package com.vertexai.macro.impl.misc;

import lombok.Getter;
import com.vertexai.Vertex;
import com.vertexai.macro.AbstractFeature;
import com.vertexai.mixin.client.MinecraftAccessor;
import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.helper.Clock;
import java.util.concurrent.ThreadLocalRandom;

public class AutoClicker extends AbstractFeature {

    public static AutoClicker instance = new AutoClicker();
    public static AutoClicker getInstance() { return instance; }

    private final Clock leftClock = new Clock();
    private final Clock rightClock = new Clock();

    @Override
    public String getName() {
        return "AutoClicker";
    }

    @Override
    public boolean shouldStartAtLaunch() {
        return true;
    }

    @Override
    public void start() {
        this.enabled = true;
    }

    @Override
    protected void onTick() {
        if (mc.screen != null || mc.player == null) {
            return;
        }

        // Left AutoClicker â€” active only when holding down attack key (Left Click)
        if (Vertex.config().misc.leftClicker && mc.options.keyAttack.isDown()) {
            // Reset Minecraft's internal 4-tick (5 CPS) attack delay (missTime)
            ((MinecraftAccessor) mc).setAttackCooldown(0);

            int minCps = Math.max(1, Vertex.config().misc.minLeftCps);
            int maxCps = Math.max(minCps, Vertex.config().misc.maxLeftCps);

            if (!leftClock.isScheduled() || leftClock.passed()) {
                KeyBindUtil.leftClick();
                ((MinecraftAccessor) mc).setAttackCooldown(0);

                double targetCps = ThreadLocalRandom.current().nextDouble(minCps, maxCps + 0.999);
                long delay = Math.max(10, (long) (1000.0 / targetCps));
                leftClock.schedule(delay);
            }
        } else {
            leftClock.reset();
        }

        // Right AutoClicker â€” active only when holding down use key (Right Click)
        if (Vertex.config().misc.rightClicker && mc.options.keyUse.isDown()) {
            // Reset Minecraft's internal rightClickDelay
            ((MinecraftAccessor) mc).setItemUseCooldown(0);

            int minCps = Math.max(1, Vertex.config().misc.minRightCps);
            int maxCps = Math.max(minCps, Vertex.config().misc.maxRightCps);

            if (!rightClock.isScheduled() || rightClock.passed()) {
                KeyBindUtil.rightClick();
                ((MinecraftAccessor) mc).setItemUseCooldown(0);

                double targetCps = ThreadLocalRandom.current().nextDouble(minCps, maxCps + 0.999);
                long delay = Math.max(10, (long) (1000.0 / targetCps));
                rightClock.schedule(delay);
            }
        } else {
            rightClock.reset();
        }
    }
}
