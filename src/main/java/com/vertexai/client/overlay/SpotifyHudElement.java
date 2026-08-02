package com.vertexai.client.overlay;

import com.vertexai.VertexClient;
import com.vertexai.integration.spotify.SpotifyManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

public class SpotifyHudElement extends AbstractHUDElement {

    private static final SpotifyHudElement instance = new SpotifyHudElement();
    public static SpotifyHudElement getInstance() { return instance; }

    private static final int WIDTH = 180;
    private static final int HEIGHT = 46;

    public SpotifyHudElement() {
        super();
    }

    @Override
    public boolean isEnabled() {
        if (VertexClient.config == null || VertexClient.config.hud == null || VertexClient.config.spotify == null) {
            return false;
        }
        return VertexClient.config.hud.enableSpotifyHud && VertexClient.config.spotify.enableSpotify;
    }

    @Override
    public void render(GuiGraphics context, float tickDelta) {
        if (!isEnabled()) return;
        if (VertexClient.config != null && VertexClient.config.hud != null && VertexClient.config.hud.spotifyHUD != null) {
            this.x = VertexClient.config.hud.spotifyHUD.x;
            this.y = VertexClient.config.hud.spotifyHUD.y;
            this.anchor = VertexClient.config.hud.spotifyHUD.anchor;
            this.scale = VertexClient.config.hud.spotifyHUD.scale;
        }

        SpotifyManager spotify = SpotifyManager.getInstance();
        if (!spotify.isAuthenticated()) return;

        int renderX = (int) getActualX();
        int renderY = (int) getActualY();

        context.pose().pushMatrix();
        context.pose().translate((float) renderX, (float) renderY);
        context.pose().scale(scale, scale);

        // Background panel (Dark translucent glass)
        context.fill(0, 0, WIDTH, HEIGHT, 0xDD121212);
        // Accent border left (Spotify green)
        context.fill(0, 0, 3, HEIGHT, 0xFF1DB954);

        int contentX = 8;
        boolean showArt = VertexClient.config.spotify.hudShowAlbumArt;
        Identifier artLoc = spotify.getAlbumArtTextureLoc();

        if (showArt && artLoc != null) {
            // Draw 34x34 Album Art
            context.blit(artLoc, 8, 6, 0, 0, 34, 34, 34, 34);
            contentX = 48;
        }

        // Track Title
        String title = spotify.getTrackName();
        if (title.length() > 20) {
            title = title.substring(0, 18) + "..";
        }
        context.drawString(mc.font, "§f" + title, contentX, 6, 0xFFFFFFFF, true);

        // Artist Name
        String artist = spotify.getArtistName();
        if (artist.length() > 24) {
            artist = artist.substring(0, 22) + "..";
        }
        context.drawString(mc.font, "§7" + artist, contentX, 18, 0xAAAAAA, true);

        // Play / Pause status badge
        String statusIcon = spotify.isPlaying() ? "§a▶" : "§c❚❚";
        context.drawString(mc.font, statusIcon, WIDTH - 16, 6, 0xFFFFFFFF, true);

        // Progress Bar
        if (VertexClient.config.spotify.hudShowProgressBar && spotify.getDurationMs() > 0) {
            int barX = contentX;
            int barY = 32;
            int barW = WIDTH - barX - 10;
            int barH = 4;

            float progressRatio = (float) spotify.getProgressMs() / (float) spotify.getDurationMs();
            progressRatio = Math.max(0.0f, Math.min(1.0f, progressRatio));
            int filledW = (int) (barW * progressRatio);

            // Bar background
            context.fill(barX, barY, barX + barW, barY + barH, 0xFF333333);
            // Bar fill (Green)
            if (filledW > 0) {
                context.fill(barX, barY, barX + filledW, barY + barH, 0xFF1DB954);
            }
        }

        context.pose().popMatrix();
    }

    @Override
    public int getWidth() {
        return (int) (WIDTH * scale);
    }

    @Override
    public int getHeight() {
        return (int) (HEIGHT * scale);
    }
}
