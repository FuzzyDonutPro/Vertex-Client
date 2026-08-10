package com.vertexai.feature.impl;

import lombok.Getter;
import com.vertexai.feature.AbstractFeature;
import com.vertexai.macro.MacroManager;

public class KillSwitch extends AbstractFeature {

    @Getter
    public static final KillSwitch instance = new KillSwitch();

    private float healthThreshold = 0.25f; // Disconnect at 25% HP

    @Override
    public String getName() {
        return "KillSwitch";
    }

    @Override
    protected void onTick() {
        if (mc.player == null || mc.level == null) return;

        float maxHp = mc.player.getMaxHealth();
        float currentHp = mc.player.getHealth();

        if (maxHp > 0 && (currentHp / maxHp) <= healthThreshold && MacroManager.getInstance().isRunning()) {
            log("CRITICAL: Player HP dropped below 25%! Triggering Emergency KillSwitch!");
            MacroManager.getInstance().disable();
            if (mc.player.connection != null) {
                mc.player.connection.sendCommand("lobby");
            }
        }
    }
}
