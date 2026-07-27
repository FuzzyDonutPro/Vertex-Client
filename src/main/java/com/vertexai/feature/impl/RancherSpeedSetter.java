package com.vertexai.feature.impl;

import lombok.Getter;
import com.vertexai.feature.AbstractFeature;
import com.vertexai.util.helper.Clock;

/**
 * RancherSpeedSetter — Automatically configures Rancher's Boots speed limit
 * via /setmaxspeed or /rancher based on active farming crop.
 */
public class RancherSpeedSetter extends AbstractFeature {

    @Getter
    public static final RancherSpeedSetter instance = new RancherSpeedSetter();

    private final Clock cooldown = new Clock();

    public RancherSpeedSetter() {
        this.enabled = true;
    }

    @Override
    public String getName() {
        return "RancherSpeedSetter";
    }

    public void setOptimalSpeed(int speed) {
        if (mc.player == null || mc.player.connection == null) return;
        if (cooldown.isScheduled() && !cooldown.passed()) return;

        log("RancherSpeedSetter: Setting max speed to " + speed + "...");
        mc.player.connection.sendCommand("setmaxspeed " + speed);
        cooldown.schedule(5000);
    }
}
