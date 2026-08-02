package com.vertexai.integration.spotify;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.vertexai.VertexClient;
import com.vertexai.util.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SpotifyManager {

    private static final SpotifyManager INSTANCE = new SpotifyManager();
    public static SpotifyManager getInstance() { return INSTANCE; }

    private static final String DEFAULT_CLIENT_ID = "00c3b060d4b84b6f84d0b1a039750d4f";
    private static final String REDIRECT_URI = "http://localhost:8888/callback";
    private static final String SCOPES = "user-read-playback-state user-modify-playback-state user-read-currently-playing";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // OAuth state
    private String codeVerifier;
    private String accessToken = "";
    private String refreshToken = "";
    private long tokenExpiresAt = 0;

    // Track status
    private boolean authenticated = false;
    private boolean isPlaying = false;
    private String trackName = "No Track Playing";
    private String artistName = "Spotify";
    private String albumName = "";
    private String albumArtUrl = "";
    private int progressMs = 0;
    private int durationMs = 0;

    // Album art texture
    private Identifier albumArtTextureLoc = null;
    private String loadedArtUrl = "";

    private SpotifyManager() {
        // Schedule periodic playback state updates
        scheduler.scheduleAtFixedRate(this::pollPlaybackState, 2, 2, TimeUnit.SECONDS);
    }

    public String getClientId() {
        if (VertexClient.config != null && VertexClient.config.spotify != null && 
            VertexClient.config.spotify.customClientId != null && !VertexClient.config.spotify.customClientId.trim().isEmpty()) {
            return VertexClient.config.spotify.customClientId.trim();
        }
        return DEFAULT_CLIENT_ID;
    }

    public void startAuthFlow() {
        try {
            this.codeVerifier = generateCodeVerifier();
            String codeChallenge = generateCodeChallenge(codeVerifier);

            String authUrl = "https://accounts.spotify.com/authorize?" +
                    "response_type=code" +
                    "&client_id=" + URLEncoder.encode(getClientId(), StandardCharsets.UTF_8) +
                    "&scope=" + URLEncoder.encode(SCOPES, StandardCharsets.UTF_8) +
                    "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                    "&code_challenge_method=S256" +
                    "&code_challenge=" + URLEncoder.encode(codeChallenge, StandardCharsets.UTF_8);

            SpotifyAuthServer.start(new SpotifyAuthServer.AuthCallback() {
                @Override
                public void onCodeReceived(String code) {
                    exchangeCodeForTokens(code);
                }

                @Override
                public void onError(String error) {
                    Logger.sendLog("[Spotify] Auth failed: " + error);
                }
            });

            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(new URI(authUrl));
            }
            Logger.sendLog("[Spotify] Opened browser for authorization...");
        } catch (Exception e) {
            Logger.sendLog("[Spotify] Failed to initiate auth flow: " + e.getMessage());
        }
    }

    private void exchangeCodeForTokens(String code) {
        try {
            String requestBody = "grant_type=authorization_code" +
                    "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8) +
                    "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                    "&client_id=" + URLEncoder.encode(getClientId(), StandardCharsets.UTF_8) +
                    "&code_verifier=" + URLEncoder.encode(codeVerifier, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://accounts.spotify.com/api/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                this.accessToken = json.get("access_token").getAsString();
                if (json.has("refresh_token")) {
                    this.refreshToken = json.get("refresh_token").getAsString();
                }
                int expiresIn = json.get("expires_in").getAsInt();
                this.tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000L);
                this.authenticated = true;

                Logger.sendLog("[Spotify] Auth successful! Access token acquired.");
                pollPlaybackState();
            } else {
                Logger.sendLog("[Spotify] Token exchange failed HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            Logger.sendLog("[Spotify] Token exchange error: " + e.getMessage());
        }
    }

    private void refreshTokenIfNeeded() {
        if (!authenticated || refreshToken == null || refreshToken.isEmpty()) return;
        if (System.currentTimeMillis() < tokenExpiresAt - 60000) return; // refresh 1 min before expiry

        try {
            String requestBody = "grant_type=refresh_token" +
                    "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8) +
                    "&client_id=" + URLEncoder.encode(getClientId(), StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://accounts.spotify.com/api/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                this.accessToken = json.get("access_token").getAsString();
                if (json.has("refresh_token")) {
                    this.refreshToken = json.get("refresh_token").getAsString();
                }
                int expiresIn = json.get("expires_in").getAsInt();
                this.tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000L);
                Logger.sendLog("[Spotify] Access token refreshed.");
            }
        } catch (Exception e) {
            Logger.sendLog("[Spotify] Failed to refresh token: " + e.getMessage());
        }
    }

    public synchronized void pollPlaybackState() {
        if (!authenticated || accessToken.isEmpty()) return;
        refreshTokenIfNeeded();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.spotify.com/v1/me/player"))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null && !response.body().isEmpty()) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

                this.isPlaying = json.has("is_playing") && json.get("is_playing").getAsBoolean();
                this.progressMs = json.has("progress_ms") && !json.get("progress_ms").isJsonNull() ? json.get("progress_ms").getAsInt() : 0;

                if (json.has("item") && !json.get("item").isJsonNull()) {
                    JsonObject item = json.getAsJsonObject("item");
                    this.trackName = item.has("name") ? item.get("name").getAsString() : "Unknown Track";
                    this.durationMs = item.has("duration_ms") ? item.get("duration_ms").getAsInt() : 0;

                    if (item.has("artists")) {
                        JsonArray artists = item.getAsJsonArray("artists");
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < artists.size(); i++) {
                            if (i > 0) sb.append(", ");
                            sb.append(artists.get(i).getAsJsonObject().get("name").getAsString());
                        }
                        this.artistName = sb.toString();
                    }

                    if (item.has("album")) {
                        JsonObject album = item.getAsJsonObject("album");
                        this.albumName = album.has("name") ? album.get("name").getAsString() : "";
                        if (album.has("images")) {
                            JsonArray images = album.getAsJsonArray("images");
                            if (images.size() > 0) {
                                String newArtUrl = images.get(0).getAsJsonObject().get("url").getAsString();
                                if (!newArtUrl.equals(this.loadedArtUrl)) {
                                    this.albumArtUrl = newArtUrl;
                                    loadAlbumArtTexture(newArtUrl);
                                }
                            }
                        }
                    }
                }
            } else if (response.statusCode() == 204) {
                // No active playback device
                this.isPlaying = false;
                this.trackName = "No Active Device";
                this.artistName = "Open Spotify";
            }
        } catch (Exception e) {
            // Ignore temporary net drops
        }
    }

    public void play() { sendPlayerCommand("PUT", "https://api.spotify.com/v1/me/player/play"); }
    public void pause() { sendPlayerCommand("PUT", "https://api.spotify.com/v1/me/player/pause"); }
    public void togglePlay() {
        if (isPlaying) pause();
        else play();
    }
    public void nextTrack() { sendPlayerCommand("POST", "https://api.spotify.com/v1/me/player/next"); }
    public void previousTrack() { sendPlayerCommand("POST", "https://api.spotify.com/v1/me/player/previous"); }

    private void sendPlayerCommand(String method, String url) {
        if (!authenticated || accessToken.isEmpty()) {
            Logger.sendLog("[Spotify] Not authenticated. Run /spotify auth first.");
            return;
        }
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                refreshTokenIfNeeded();
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "Bearer " + accessToken);

                if ("PUT".equalsIgnoreCase(method)) {
                    builder.PUT(HttpRequest.BodyPublishers.noBody());
                } else if ("POST".equalsIgnoreCase(method)) {
                    builder.POST(HttpRequest.BodyPublishers.noBody());
                }

                HttpResponse<String> resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 204 || resp.statusCode() == 200) {
                    Thread.sleep(300);
                    pollPlaybackState();
                } else {
                    Logger.sendLog("[Spotify] Command failed (" + resp.statusCode() + "): " + resp.body());
                }
            } catch (Exception e) {
                Logger.sendLog("[Spotify] Command error: " + e.getMessage());
            }
        });
    }

    private void loadAlbumArtTexture(String urlStr) {
        this.loadedArtUrl = urlStr;
        Executors.newSingleThreadExecutor().submit(() -> {
            try (InputStream in = new URL(urlStr).openStream()) {
                NativeImage nativeImage = NativeImage.read(in);
                Minecraft.getInstance().execute(() -> {
                    try {
                        DynamicTexture dynTexture = new DynamicTexture(() -> "spotify_album_art", nativeImage);
                        Identifier loc = Identifier.fromNamespaceAndPath("vertexai", "spotify_album_art");
                        Minecraft.getInstance().getTextureManager().register(loc, dynTexture);
                        this.albumArtTextureLoc = loc;
                    } catch (Exception e) {
                        Logger.sendLog("[Spotify] Failed to register album art texture: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                Logger.sendLog("[Spotify] Failed to load album art image: " + e.getMessage());
            }
        });
    }

    private String generateCodeVerifier() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateCodeChallenge(String verifier) throws Exception {
        byte[] bytes = verifier.getBytes(StandardCharsets.US_ASCII);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    // Getters
    public boolean isAuthenticated() { return authenticated; }
    public boolean isPlaying() { return isPlaying; }
    public String getTrackName() { return trackName; }
    public String getArtistName() { return artistName; }
    public String getAlbumName() { return albumName; }
    public String getAlbumArtUrl() { return albumArtUrl; }
    public int getProgressMs() { return progressMs; }
    public int getDurationMs() { return durationMs; }
    public Identifier getAlbumArtTextureLoc() { return albumArtTextureLoc; }
}
