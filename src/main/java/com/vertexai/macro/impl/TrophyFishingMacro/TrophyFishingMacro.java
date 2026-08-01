package com.vertexai.macro.impl.TrophyFishingMacro;

import lombok.Getter;
import com.vertexai.macro.AbstractMacro;
import com.vertexai.util.helper.Clock;

import net.minecraft.world.entity.projectile.FishingHook;

import java.util.Collections;
import java.util.List;

/**
 * TrophyFishingMacro — Handles Crimson Isle Lava & Trophy Fishing, monitors bobber motion,
 * reels in on bite, and auto-casts fishing rod.
 */
public class TrophyFishingMacro extends AbstractMacro {

    public static final TrophyFishingMacro instance = new TrophyFishingMacro();
    public static TrophyFishingMacro getInstance() { return instance; }

    private final Clock reelClock = new Clock();
    private boolean waitingForBite = false;

    @Override
    public String getName() {
        return "Trophy & Lava Fisher";
    }

    @Override
    public List<String> getNecessaryItems() {
        return Collections.emptyList();
    }

    @Override
    public void enable() {
        super.enable();
        log("TrophyFishingMacro: Enabled! Auto-casting into lava...");
        castRod();
    }

    @Override
    public void onTick() {
        if (!isEnabled() || mc.player == null || mc.gameMode == null) return;
        if (reelClock.isScheduled() && !reelClock.passed()) return;

        FishingHook hook = mc.player.fishing;

        if (hook == null && !waitingForBite) {
            castRod();
            return;
        }

        if (hook != null) {
            waitingForBite = true;
            // Check bobber motion in lava/water for bite detection
            if (hook.getDeltaMovement().y < -0.05) {
                log("TrophyFishingMacro: Bite detected! Reeling in...");
                mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
                waitingForBite = false;
                reelClock.schedule(1200); // 1.2s before re-casting
            }
        }
    }

    private void castRod() {
        if (mc.player == null || mc.gameMode == null) return;
        log("TrophyFishingMacro: Casting rod...");
        mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        waitingForBite = true;
        reelClock.schedule(2000);
    }
}
