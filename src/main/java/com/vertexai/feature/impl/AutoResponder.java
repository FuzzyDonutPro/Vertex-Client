package com.vertexai.feature.impl;

import lombok.Getter;
import com.vertexai.feature.AbstractFeature;
import com.vertexai.macro.MacroManager;
import com.vertexai.util.DiscordWebhookNotifier;

/**
 * AutoResponder — Monitors chat for whispers (/msg), direct mentions, and admin check prompts.
 * Pauses macro and triggers high-priority Discord alert embed.
 */
public class AutoResponder extends AbstractFeature {

    @Getter
    public static final AutoResponder instance = new AutoResponder();

    public AutoResponder() {
        this.enabled = true;
    }

    @Override
    public String getName() {
        return "AutoResponder";
    }

    public void onChat(String message) {
        if (mc.player == null) return;
        String msg = message.toLowerCase();
        String playerName = mc.player.getName().getString().toLowerCase();

        boolean isWhisper = msg.contains("from ") || msg.contains("whispers:") || msg.contains("to ");
        boolean isMentioned = msg.contains(playerName);
        boolean isAdminCheck = msg.contains("are you macroing") || msg.contains("are you botting") ||
                               msg.contains("reply to") || msg.contains("type ") || msg.contains("captcha");

        if (isWhisper || (isMentioned && !msg.contains("joined")) || isAdminCheck) {
            warn("ALERT: Direct message or admin check detected: \"" + message + "\"! Pausing macro...");

            if (MacroManager.getInstance().isRunning()) {
                MacroManager.getInstance().pause();
            }

            DiscordWebhookNotifier.sendWebhookNotification("💬 CHAT ALERT / ADMIN CHECK", "Message received: `" + message + "`\nMacro paused automatically.", 0xFFF59E0B);
        }
    }
}
