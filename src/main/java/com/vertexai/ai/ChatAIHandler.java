package com.vertexai.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vertexai.config.VertexAIConfig;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;


import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class ChatAIHandler {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            handleMessage(message.getString());
        });
    }

    private static void handleMessage(String messageString) {
        if (!VertexAIConfig.getInstance().enableAutoResponder) return;
        
        Minecraft client = Minecraft.getInstance();
        if (Minecraft.getInstance().player == null) return;
        
        String username = client.getUser().getName();
        
        // Don't reply to our own messages to avoid loops
        if (messageString.startsWith("<" + username + ">") || messageString.contains(username + " ")) {
            // Very basic check if it's our own message. Might need refinement depending on server chat format.
            // A simple heuristic: if it contains our username, and we didn't just send it.
            // Let's refine: trigger if our username is in the message string, but the message didn't originate from us.
            
            // To be safe against self-loops, we should track recent messages we sent.
            // But for V1, just check if the username is mentioned.
            if (messageString.toLowerCase().contains(username.toLowerCase())) {
                // To avoid triggering on our own messages when they echo back from the server,
                // we can just check if the message starts with our name (common for "<Username> hello").
                if (messageString.startsWith("<" + username + ">") || messageString.startsWith(username + ": ") || messageString.startsWith(username + " > ")) {
                    return; 
                }
                
                // Trigger AI
                generateReply(messageString);
            }
        }
    }

    private static void generateReply(String contextMessage) {
        String apiKey = VertexAIConfig.getInstance().apiKey;
        if (apiKey == null || apiKey.isEmpty()) {
            Minecraft.getInstance().player.displayClientMessage(Component.literal("§c[Vertex AI] API Key is missing in config!"), false);
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
                
                JsonObject root = new JsonObject();
                JsonArray contents = new JsonArray();
                JsonObject content = new JsonObject();
                JsonArray parts = new JsonArray();
                JsonObject part = new JsonObject();
                
                String prompt = "You are a Minecraft player AI assistant. Keep your responses short (under 100 characters). Reply naturally to this chat message as if you are playing on the server: " + contextMessage;
                part.addProperty("text", prompt);
                parts.add(part);
                content.add("parts", parts);
                contents.add(content);
                root.add("contents", contents);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(root.toString()))
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                    String reply = jsonResponse.getAsJsonArray("candidates")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("content")
                            .getAsJsonArray("parts")
                            .get(0).getAsJsonObject()
                            .get("text").getAsString();
                            
                    // Send the message to the server
                    Minecraft client = Minecraft.getInstance();
                    if (Minecraft.getInstance().player != null) {
                        client.execute(() -> {
                            client.player.connection.sendChat(reply.trim().replace("\n", " "));
                        });
                    }
                } else {
                    Minecraft.getInstance().player.displayClientMessage(Component.literal("§c[Vertex AI] API Error: " + response.statusCode()), false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
