package com.vertexai.feature.impl;

import lombok.Getter;
import com.vertexai.feature.AbstractFeature;
import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.helper.Clock;
import net.minecraft.world.entity.projectile.FishingHook;

public class AutoFisher extends AbstractFeature {

    @Getter
    public static final AutoFisher instance = new AutoFisher();

    private final Clock reelClock = new Clock();
    private boolean waitingForCatch = false;

    @Override
    public String getName() {
        return "AutoFisher";
    }

    @Override
    protected void onTick() {
        if (mc.player == null || mc.level == null) return;
        if (reelClock.isScheduled() && !reelClock.passed()) return;

        FishingHook bobber = mc.player.fishing;

        // If no bobber active, cast rod
        if (bobber == null) {
            if (waitingForCatch) {
                waitingForCatch = false;
            }
            KeyBindUtil.rightClick();
            reelClock.schedule(1500); // Wait 1.5s after cast
            return;
        }

        waitingForCatch = true;

        // Detect bobber splash down motion (Y velocity drop or splash event)
        if (bobber.getDeltaMovement().y < -0.08) {
            log("AutoFisher: Splash detected! Reeling in catch...");
            KeyBindUtil.rightClick();
            waitingForCatch = false;
            reelClock.schedule(1200); // 1.2s delay before next cast
        }
    }
}
