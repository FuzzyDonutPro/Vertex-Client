package com.vertexai.macro.features.misc;

import lombok.Getter;
import com.vertexai.macro.AbstractFeature;
import com.vertexai.util.helper.Clock;

/**
 * WardrobeSwapper â€” Automatically swaps armor sets via /wardrobe
 * when switching activity macro modes.
 */
public class WardrobeSwapper extends AbstractFeature {

    @Getter
    public static final WardrobeSwapper instance = new WardrobeSwapper();

    private final Clock cooldown = new Clock();

    public WardrobeSwapper() {
        this.enabled = true;
    }

    @Override
    public String getName() {
        return "WardrobeSwapper";
    }

    public void swapToSlot(int slot) {
        if (mc.player == null || mc.player.connection == null) return;
        if (cooldown.isScheduled() && !cooldown.passed()) return;

        log("WardrobeSwapper: Equipping Wardrobe slot " + slot + "...");
        mc.player.connection.sendCommand("wardrobe " + slot);
        cooldown.schedule(5000);
    }
}
