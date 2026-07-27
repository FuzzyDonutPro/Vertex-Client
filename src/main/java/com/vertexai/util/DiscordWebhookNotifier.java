package com.vertexai.util;

import com.vertexai.Vertex;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * DiscordWebhookNotifier — Asynchronously dispatches rich Discord Webhook embeds.
 */
public class DiscordWebhookNotifier {

    public static void sendWebhookNotification(String title, String description, int color) {
        String webhookUrl = Vertex.config().webhook.webhookUrl;
        if (webhookUrl == null || webhookUrl.trim().isEmpty() || !webhookUrl.startsWith("http")) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String jsonPayload = String.format(
                        "{\"embeds\":[{\"title\":\"%s\",\"description\":\"%s\",\"color\":%d,\"footer\":{\"text\":\"Vertex AI Client\"}}]}",
                        escapeJson(title),
                        escapeJson(description),
                        color
                );

                URL url = new URL(webhookUrl.trim());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("User-Agent", "VertexClient/1.0");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                }
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception ignored) {
            }
        });
    }

    private static String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}
