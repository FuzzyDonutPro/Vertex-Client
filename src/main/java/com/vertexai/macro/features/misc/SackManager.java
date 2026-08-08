package com.vertexai.macro.features.misc;

import lombok.Getter;
import com.vertexai.macro.AbstractFeature;
import com.vertexai.macro.MacroManager;
import com.vertexai.util.helper.Clock;

/**
 * SackManager â€” Automatically deposits harvested crops, ores, and drops into Sacks
 * or Personal Compactor to prevent inventory clogging during macros.
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

    @Override
    protected void onTick() {
        if (mc.player == null || mc.level == null) return;
        if (!MacroManager.getInstance().isRunning()) return;
        if (cooldown.isScheduled() && !cooldown.passed()) return;

        // Check if main inventory (slots 9 to 35) is >80% full
        int filledSlots = 0;
        for (int i = 9; i < 36; i++) {
            if (!mc.player.getInventory().getItem(i).isEmpty()) {
                filledSlots++;
            }
        }

        if (filledSlots >= 22) { // 22/27 slots filled = ~81%
            depositToSacks();
        }
    }

    public void depositToSacks() {
        if (mc.player == null || mc.player.connection == null) return;
        if (cooldown.isScheduled() && !cooldown.passed()) return;

        log("SackManager: Depositing items into Sacks...");
        mc.player.connection.sendCommand("sacks");
        cooldown.schedule(12000); // 12s cooldown
    }
}
