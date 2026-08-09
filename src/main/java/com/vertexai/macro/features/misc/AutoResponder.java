package com.vertexai.macro.features.misc;

import lombok.Getter;
import com.vertexai.macro.AbstractFeature;
import com.vertexai.macro.MacroManager;
import com.vertexai.util.DiscordWebhookNotifier;

/**
 * AutoResponder â€” Monitors chat for whispers (/msg), direct mentions, and admin check prompts.
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

        // Clean string check with color codes stripped if needed
        boolean isWhisper = msg.contains("from ") && (msg.contains(":") || msg.contains("->"));
        boolean isMentioned = !playerName.isEmpty() && msg.contains(playerName);
        boolean isAdminCheck = msg.contains("are you macroing") || msg.contains("are you botting") ||
                               msg.contains("reply to staff") || msg.contains("type the captcha") || msg.contains("bot check");

        if (isWhisper || (isMentioned && !msg.contains("joined") && !msg.contains("left")) || isAdminCheck) {
            // Calculate reading pause: 0.7 seconds (700ms) per word in the received message (minimum 2.5s pause)
            String[] words = message.trim().split("\\s+");
            long pauseDurationMs = Math.max(2500L, (long) (words.length * 700L));

            warn("ALERT: Direct message or admin check detected: \"" + message + "\" (" + words.length + " words)! Pausing movement for " + (pauseDurationMs / 1000.0) + "s...");

            if (MacroManager.getInstance().isRunning()) {
                MacroManager.getInstance().pause();

                // Schedule automated macro resumption after the calculated humanized reading pause
                com.vertexai.Vertex.executor().execute(() -> {
                    try {
                        Thread.sleep(pauseDurationMs);
                        if (MacroManager.getInstance().isPaused()) {
                            warn("Reading pause elapsed (" + (pauseDurationMs / 1000.0) + "s). Resuming macro movement...");
                            MacroManager.getInstance().resume();
                        }
                    } catch (InterruptedException ignored) {}
                });
            }

            DiscordWebhookNotifier.sendWebhookNotification("CHAT ALERT / ADMIN CHECK", "Message received: `" + message + "`\nMovement paused for `" + (pauseDurationMs / 1000.0) + "s` (" + words.length + " words at 0.7s/word).", 0xFFF59E0B);
        }
    }
}
