package com.vertexai.feature.impl;

import lombok.Getter;
import com.vertexai.feature.AbstractFeature;
import com.vertexai.util.helper.Clock;

/**
 * SackManager — Automatically deposits harvested crops, ores, and drops into Sacks
 * or Personal Compactor to prevent inventory clogging.
 */
public class SackManager extends AbstractFeature {

    @Getter
    public static final SackManager instance = new SackManager();

    private final Clock cooldown = new Clock();

    public SackManager() {
        this.enabled = true;
    }

    @Override
    public String getName() {
        return "SackManager";
    }

    public void depositToSacks() {
        if (mc.player == null || mc.player.connection == null) return;
        if (cooldown.isScheduled() && !cooldown.passed()) return;

        log("SackManager: Depositing items into Sacks...");
        mc.player.connection.sendCommand("sacks");
        cooldown.schedule(10000);
    }
}
