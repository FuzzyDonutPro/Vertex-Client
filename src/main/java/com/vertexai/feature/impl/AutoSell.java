package com.vertexai.feature.impl;

import lombok.Getter;
import com.vertexai.Vertex;
import com.vertexai.feature.AbstractFeature;
import com.vertexai.util.helper.Clock;

public class AutoSell extends AbstractFeature {

    public static final AutoSell instance = new AutoSell();
    public static AutoSell getInstance() { return instance; }

    private final Clock cooldown = new Clock();
    private boolean selling = false;

    @Override
    public String getName() {
        return "AutoSell";
    }

    @Override
    protected void onTick() {
        if (mc.player == null || mc.level == null) return;
        if (cooldown.isScheduled() && !cooldown.passed()) return;

        // Check if inventory is full (slots 9 to 35)
        boolean isFull = true;
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getItem(i).isEmpty()) {
                isFull = false;
                break;
            }
        }

        if (isFull && !selling) {
            selling = true;
            cooldown.schedule(5000); // 5s cooldown between sell commands
            log("Inventory full! Triggering Auto-Sell command /trades or /bazaar...");
            if (mc.player.connection != null) {
                mc.player.connection.sendCommand("bz");
            }
        } else if (!isFull) {
            selling = false;
        }
    }
}
