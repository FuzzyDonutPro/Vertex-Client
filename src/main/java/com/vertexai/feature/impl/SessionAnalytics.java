package com.vertexai.feature.impl;

import lombok.Getter;
import com.vertexai.Vertex;
import com.vertexai.feature.AbstractFeature;
import com.vertexai.macro.MacroManager;
import com.vertexai.util.Logger;
import com.vertexai.util.helper.Clock;
import com.vertexai.util.helper.DiscordWebhookUtil;

public class SessionAnalytics extends AbstractFeature {

    private static final SessionAnalytics instance = new SessionAnalytics();

    public static SessionAnalytics getInstance() {
        return instance;
    }

    private long sessionStartTime = 0;
    private long totalItemsHarvested = 0;
    private int failsafesTriggeredCount = 0;
    private final Clock webhookIntervalClock = new Clock();

    public SessionAnalytics() {
        this.enabled = true;
    }

    @Override
    public String getName() {
        return "SessionAnalytics";
    }

    public void onMacroStart() {
        this.sessionStartTime = System.currentTimeMillis();
        this.totalItemsHarvested = 0;
        this.failsafesTriggeredCount = 0;
        this.webhookIntervalClock.schedule(1800000); // Send periodic summary every 30 minutes
        Logger.sendLog("[Analytics] Session tracking started.");
    }

    public void onMacroStop(String reason) {
        if (sessionStartTime == 0) return;

        long durationMs = System.currentTimeMillis() - sessionStartTime;
        long minutes = durationMs / 60000;
        long seconds = (durationMs % 60000) / 1000;

        String macroName = MacroManager.getInstance().getActiveMacro() != null ?
                MacroManager.getInstance().getActiveMacro().getName() : "Macro";

        String summary = String.format(
                "**[Vertex Client] Session Summary**\n" +
                "• **Macro**: %s\n" +
                "• **Duration**: %dm %ds\n" +
                "• **Total Harvested**: %d items\n" +
                "• **Failsafes Triggered**: %d\n" +
                "• **Stop Reason**: %s",
                macroName, minutes, seconds, totalItemsHarvested, failsafesTriggeredCount, reason
        );

        if (Vertex.config() != null && Vertex.config().webhook != null && 
            Vertex.config().webhook.webhookUrl != null && !Vertex.config().webhook.webhookUrl.trim().isEmpty()) {
            DiscordWebhookUtil.sendWebhook(summary);
        }

        this.sessionStartTime = 0;
    }

    @Override
    protected void onTick() {
        if (sessionStartTime != 0 && webhookIntervalClock.isScheduled() && webhookIntervalClock.passed()) {
            webhookIntervalClock.schedule(1800000);
            sendPeriodicSummary();
        }
    }

    private void sendPeriodicSummary() {
        long durationMs = System.currentTimeMillis() - sessionStartTime;
        long minutes = durationMs / 60000;
        String macroName = MacroManager.getInstance().getActiveMacro() != null ?
                MacroManager.getInstance().getActiveMacro().getName() : "Macro";

        String payload = String.format(
                "**[Vertex Client] Periodic Session Update**\n" +
                "• **Active Macro**: %s\n" +
                "• **Uptime**: %d minutes\n" +
                "• **Items Harvested**: %d",
                macroName, minutes, totalItemsHarvested
        );

        if (Vertex.config() != null && Vertex.config().webhook != null && 
            Vertex.config().webhook.webhookUrl != null && !Vertex.config().webhook.webhookUrl.trim().isEmpty()) {
            DiscordWebhookUtil.sendWebhook(payload);
        }
    }

    public void incrementHarvestCount(int amount) {
        this.totalItemsHarvested += amount;
    }

    public void incrementFailsafeCount() {
        this.failsafesTriggeredCount++;
    }
}
