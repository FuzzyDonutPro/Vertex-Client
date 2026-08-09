package com.vertexai.ui.hud.elements;

import lombok.Getter;
import com.vertexai.client.overlay.TextHud;
import com.vertexai.macro.MacroManager;

import java.util.List;

/**
 * ProfitHUD — Live item tracking & Bazaar profit overlay.
 */
public class ProfitHUD extends TextHud {

    private static final ProfitHUD instance = new ProfitHUD();

    private long sessionStartTime = -1;
    private long totalItemsHarvested = 0;
    private long estimatedCoins = 0;

    public ProfitHUD() {
        super();
        this.x = 5;
        this.y = 120;
        this.anchor = 0; // Top-Left
        this.enabled = true;
    }

    public static ProfitHUD getInstance() {
        return instance;
    }

    @Override
    protected int getAccentColor() {
        return 0xFF10B981; // Emerald Green
    }

    @Override
    protected void getLines(List<String> lines, boolean example) {
        if (example) {
            lines.add("§f§lPROFIT TRACKER");
            lines.add("§8§m------------------------");
            lines.add("§8» §7Items: §f14,250");
            lines.add("§8» §7Est. Value: §a3,850,000 Coins");
            lines.add("§8» §7Rate: §a4.2M Coins/hr");
            return;
        }

        lines.add("§f§lPROFIT TRACKER");
        lines.add("§8§m------------------------");

        if (mc.player == null) {
            lines.add("§8» §cOffline");
            return;
        }

        boolean isRunning = MacroManager.getInstance().isRunning();
        if (isRunning) {
            if (sessionStartTime == -1) {
                sessionStartTime = System.currentTimeMillis();
            }
            long elapsedSec = Math.max(1, (System.currentTimeMillis() - sessionStartTime) / 1000);
            long ratePerHour = (estimatedCoins * 3600) / elapsedSec;

            lines.add("§8» §7Items: §f" + String.format("%,d", totalItemsHarvested));
            lines.add("§8» §7Est. Value: §a" + String.format("%,d Coins", estimatedCoins));
            lines.add("§8» §7Rate: §a" + String.format("%.1fM/hr", ratePerHour / 1_000_000.0));
        } else {
            sessionStartTime = -1;
            lines.add("§8» §7Status: §7Macro Idle");
        }
    }

    public void addHarvestedItems(int count, long coinValue) {
        this.totalItemsHarvested += count;
        this.estimatedCoins += coinValue;
    }

    @Override
    protected boolean shouldShow() {
        if (!enabled || mc.player == null || mc.level == null) return false;
        if (com.vertexai.macro.impl.mining.BlockMiner.BlockMiner.getInstance().isEnabled()) return false;
        return MacroManager.getInstance().isRunning();
    }
}
