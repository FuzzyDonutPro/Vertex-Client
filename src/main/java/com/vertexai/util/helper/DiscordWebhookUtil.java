package com.vertexai.util.helper;

import com.vertexai.Vertex;
import com.vertexai.util.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class DiscordWebhookUtil {

    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void sendWebhook(String message) {
        String url = Vertex.config().webhook.webhookUrl;
        if (url == null || url.trim().isEmpty()) {
            return;
        }

        String discordId = Vertex.config().webhook.discordId;
        String mention = "";
        if (discordId != null && !discordId.trim().isEmpty()) {
            mention = "<@" + discordId.trim() + "> ";
        }

        // Extremely simple JSON escaping to prevent payload breaks
        String safeMessage = message.replace("\"", "\\\"").replace("\n", "\\n");
        String jsonPayload = "{\"content\": \"" + mention + safeMessage + "\"}";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url.trim()))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            Logger.sendLog("Discord webhook sent successfully.");
                        } else {
                            Logger.sendError("Failed to send webhook. Response code: " + response.statusCode());
                        }
                    })
                    .exceptionally(e -> {
                        Logger.sendError("Exception while sending webhook: " + e.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            Logger.sendError("Failed to build webhook request: " + e.getMessage());
        }
    }
}
