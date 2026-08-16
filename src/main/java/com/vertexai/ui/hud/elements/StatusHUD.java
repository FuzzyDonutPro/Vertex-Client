package com.vertexai.ui.hud.elements;

import com.vertexai.client.overlay.TextHud;

import java.util.List;

/**
 * StatusHUD — The legacy Java text HUD is silenced because the ultra-crisp Svelte Web HUD
 * is rendered directly through the Chromium / MCEF overlay on the in-game HUD.
 */
public class StatusHUD extends TextHud {

    private static final StatusHUD instance = new StatusHUD();

    public StatusHUD() {
        super();
        this.x = 5;
        this.y = 5;
        this.anchor = 0;
        this.enabled = true;
    }

    public static StatusHUD getInstance() {
        return instance;
    }

    @Override
    protected boolean shouldShow() {
        // Disabled: Handled by Svelte Web Overlay
        return false;
    }

    @Override
    protected void getLines(List<String> lines, boolean example) {
        lines.clear();
    }
}
