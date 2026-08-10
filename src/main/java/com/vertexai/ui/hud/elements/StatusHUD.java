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
        return 0xFF38BDF8; // Cyan / Sky Blue Theme Accent
    }

    @Override
    protected void getLines(List<String> lines, boolean example) {
        // Disabled: StatusHUD is now rendered in Svelte Overlay UI
        lines.clear();
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
}
