package com.vertexai.command;

import com.mojang.brigadier.CommandDispatcher;
import com.vertexai.integration.spotify.SpotifyManager;
import com.vertexai.util.Logger;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class SpotifyCommand {

    public void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                literal("spotify")
                        .executes(context -> {
                            showStatus();
                            return 1;
                        })
                        .then(literal("auth").executes(context -> {
                            SpotifyManager.getInstance().startAuthFlow();
                            return 1;
                        }))
                        .then(literal("play").executes(context -> {
                            SpotifyManager.getInstance().play();
                            return 1;
                        }))
                        .then(literal("pause").executes(context -> {
                            SpotifyManager.getInstance().pause();
                            return 1;
                        }))
                        .then(literal("toggle").executes(context -> {
                            SpotifyManager.getInstance().togglePlay();
                            return 1;
                        }))
                        .then(literal("next").executes(context -> {
                            SpotifyManager.getInstance().nextTrack();
                            return 1;
                        }))
                        .then(literal("skip").executes(context -> {
                            SpotifyManager.getInstance().nextTrack();
                            return 1;
                        }))
                        .then(literal("prev").executes(context -> {
                            SpotifyManager.getInstance().previousTrack();
                            return 1;
                        }))
                        .then(literal("status").executes(context -> {
                            showStatus();
                            return 1;
                        }))
        );
    }

    private void showStatus() {
        SpotifyManager spotify = SpotifyManager.getInstance();
        if (!spotify.isAuthenticated()) {
            Logger.sendLog("§c[Spotify] Not authenticated. Type §e/spotify auth§c to link your Spotify account.");
            return;
        }
        String status = spotify.isPlaying() ? "§aPlaying" : "§cPaused";
        Logger.sendLog(String.format("§a[Spotify] %s: §f%s §7by §f%s §7(%d/%ds)",
                status,
                spotify.getTrackName(),
                spotify.getArtistName(),
                spotify.getProgressMs() / 1000,
                spotify.getDurationMs() / 1000
        ));
    }
}
