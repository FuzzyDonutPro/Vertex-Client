package com.vertexai.ui.hud.elements;

import lombok.Getter;
import com.vertexai.client.overlay.TextHud;
import com.vertexai.failsafe.FailsafeManager;
import com.vertexai.macro.AbstractMacro;
import com.vertexai.macro.MacroManager;

import java.util.List;

/**
 * StatusHUD — Top-left overlay showing profit, current macro state, and macro runtime.
 */
public class StatusHUD extends TextHud {

    private static final StatusHUD instance = new StatusHUD();

    private long macroStartTime = -1;
    private long totalProfit = 0;

    public StatusHUD() {
        super();
        this.x = 5;
        this.y = 5;
        this.anchor = 0; // Top-Left
        this.enabled = true;
    }

    public static StatusHUD getInstance() {
        return instance;
    }

    @Override
    protected int getAccentColor() {
        return 0xFF3B82F6; // Primary Blue
    }

    @Override
    protected void getLines(List<String> lines, boolean example) {
        if (example) {
            lines.add("§f§lVERTEX STATUS");
            lines.add("§8§m------------------------");
            lines.add("§8» §7State: §aCrop/Wart S-Shape");
            lines.add("§8» §7Macro Time: §f00:24:15");
            lines.add("§8» §7Profit: §a+1,250,000 §8(§73.1M/hr§8)");
            return;
        }

        lines.add("§f§lVERTEX STATUS");
        lines.add("§8§m------------------------");

        if (mc.player == null) {
            lines.add("§8» §cOffline");
            return;
        }

        MacroManager mm = MacroManager.getInstance();
        AbstractMacro active = mm.getActiveMacro();
        boolean isRunning = active != null && active.isEnabled();

        // 1. Current State
        String stateStr;
        if (FailsafeManager.getInstance().triggeredFailsafe.isPresent()) {
            stateStr = "§cFAILSAFE TRIGGERED";
        } else if (isRunning) {
            String macroName = active.getName();
            String subState = getMacroSubState(active);
            stateStr = "§a" + macroName + (subState.isEmpty() ? "" : " §8(§f" + subState + "§8)");
        } else {
            stateStr = "§7IDLE";
        }
        lines.add("§8» §7State: " + stateStr);

        // 2. Macro Runtime Duration
        if (isRunning) {
            if (macroStartTime == -1) {
                macroStartTime = System.currentTimeMillis();
            }
            long elapsedSec = (System.currentTimeMillis() - macroStartTime) / 1000;
            lines.add("§8» §7Macro Time: §f" + formatTime(elapsedSec));
        } else {
            macroStartTime = -1;
            lines.add("§8» §7Macro Time: §700:00:00");
        }

        // 3. Profit Calculation
        long elapsedSec = (isRunning && macroStartTime > 0) ? Math.max(1, (System.currentTimeMillis() - macroStartTime) / 1000) : 0;
        long profitPerHour = elapsedSec > 0 ? (totalProfit * 3600) / elapsedSec : 0;
        String profitStr = String.format("§a+%,d §8(§7%,d/hr§8)", totalProfit, profitPerHour);
        lines.add("§8» §7Profit: " + profitStr);
    }

    private String getMacroSubState(AbstractMacro macro) {
        if (macro == null) return "";
        try {
            var state = macro.getStateMachine().getCurrentState();
            if (state != null) {
                return state.getClass().getSimpleName().replace("State", "");
            }
        } catch (Exception ignored) {}
        return "";
    }

    private String formatTime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public void addProfit(long amount) {
        this.totalProfit += amount;
    }

    public void resetProfit() {
        this.totalProfit = 0;
        this.macroStartTime = -1;
    }

    @Override
    protected boolean shouldShow() {
        if (!enabled || mc.player == null || mc.level == null) return false;
        if (com.vertexai.macro.impl.mining.BlockMiner.BlockMiner.getInstance().isEnabled()) {
            return false;
        }
        return com.vertexai.macro.MacroManager.getInstance().isRunning();
    }
}
