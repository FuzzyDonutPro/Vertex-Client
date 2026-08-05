package com.vertexai.gui.web;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import com.vertexai.macro.MacroManager;
import com.vertexai.util.Logger;
import com.vertexai.config.ConfigSerializer;
import com.vertexai.VertexClient;

/**
 * MCEFBridge — Bridge for Minecraft Chromium Embedded Framework (MCEF 2.2.0).
 * Detects MCEF at runtime, manages browser instances, and provides
 * bidirectional JavaScript <-> Java IPC bindings.
 */
public class MCEFBridge {

    public static boolean isMcefAvailable() {
        try {
            return MCEF.isInitialized();
        } catch (Throwable e) {
            return false;
        }
    }

    public static MCEFBrowser createBrowser(String url, boolean transparent) {
        try {
            if (!MCEF.isInitialized()) {
                MCEF.initialize();
            }
            return MCEF.createBrowser(url, transparent);
        } catch (Throwable e) {
            Logger.sendLog("Failed to create MCEF browser: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Executes IPC actions triggered from HTML/JS frontend.
     */
    public static String handleJsQuery(String request) {
        if (request == null || request.isEmpty()) {
            return "{\"status\":\"error\",\"message\":\"empty_request\"}";
        }
        try {
            // Format: "toggle_macro:<macroId>:<state>" or "get_status"
            String[] parts = request.split(":");
            String action = parts[0];

            if ("toggle_macro".equals(action)) {
                String macroId = parts.length > 1 ? parts[1] : "";
                boolean enable = parts.length > 2 ? Boolean.parseBoolean(parts[2]) : false;
                
                Logger.sendLog("[IPC] Toggling macro: " + macroId + " -> " + enable);
                if (enable) {
                    com.vertexai.macro.AbstractMacro targetMacro = null;
                    switch (macroId) {
                        case "crop":
                        case "melon_pumpkin":
                            targetMacro = com.vertexai.macro.impl.FarmingMacro.FarmingMacro.getInstance();
                            break;
                        case "sugarcane":
                            targetMacro = com.vertexai.macro.impl.CaneCactusMacro.CaneCactusMacro.getInstance();
                            break;
                        case "farm_builder":
                            targetMacro = com.vertexai.macro.impl.FarmBuilderMacro.FarmBuilderMacro.getInstance();
                            break;
                        case "visitor":
                            targetMacro = com.vertexai.macro.impl.GardenVisitorMacro.GardenVisitorMacro.getInstance();
                            break;
                        case "pest_hunter":
                            targetMacro = com.vertexai.macro.impl.PestHunterMacro.PestHunterMacro.getInstance();
                            break;
                        case "commission":
                            targetMacro = com.vertexai.macro.impl.CommissionMacro.CommissionMacro.getInstance();
                            break;
                        case "gemstone":
                            targetMacro = com.vertexai.macro.impl.RouteMiner.RouteMinerMacro.getInstance();
                            break;
                        case "mining_general":
                            targetMacro = com.vertexai.macro.impl.MiningMacro.MiningMacro.getInstance();
                            break;
                        case "powder":
                            targetMacro = com.vertexai.macro.impl.PowderMacro.PowderMacro.getInstance();
                            break;
                        case "glacial":
                            targetMacro = com.vertexai.macro.impl.GlacialMacro.GlacialMacro.getInstance();
                            break;
                        case "nuker":
                            targetMacro = com.vertexai.macro.impl.NukerMacro.NukerMacro.getInstance();
                            break;
                        case "slayer":
                            targetMacro = com.vertexai.macro.impl.SlayerMacro.SlayerMacro.getInstance();
                            break;
                        case "mob_killer":
                            targetMacro = com.vertexai.macro.impl.MobKillerMacro.MobKillerMacro.getInstance();
                            break;
                        case "kuudra":
                            targetMacro = com.vertexai.macro.impl.KuudraMacro.KuudraMacro.getInstance();
                            break;
                        case "zealot":
                            targetMacro = com.vertexai.macro.impl.ZealotMacro.ZealotMacro.getInstance();
                            break;
                        case "dungeon":
                            targetMacro = com.vertexai.macro.impl.DungeonMacro.DungeonMacro.getInstance();
                            break;
                        case "fishing":
                            targetMacro = com.vertexai.macro.impl.FishingMacro.FishingMacro.getInstance();
                            break;
                        case "trophy_fishing":
                            targetMacro = com.vertexai.macro.impl.TrophyFishingMacro.TrophyFishingMacro.getInstance();
                            break;

                        case "foraging":
                            targetMacro = com.vertexai.macro.impl.ForagingMacro.ForagingMacro.getInstance();
                            break;
                        case "alchemy":
                            targetMacro = com.vertexai.macro.impl.BrewingMacro.BrewingMacro.getInstance();
                            break;
                        case "flip":
                            targetMacro = com.vertexai.macro.impl.FlipMacro.FlipMacro.getInstance();
                            break;
                        case "diana":
                            targetMacro = com.vertexai.macro.impl.DianaBurrowMacro.DianaBurrowMacro.getInstance();
                            break;
                        default:
                            targetMacro = MacroManager.getInstance().getCurrentMacro();
                            break;
                    }
                    com.vertexai.macro.AbstractMacro finalMacro = targetMacro;
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        if (finalMacro != null) {
                            MacroManager.getInstance().enableMacro(finalMacro);
                        } else {
                            MacroManager.getInstance().enable();
                        }
                    });
                } else {
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        MacroManager.getInstance().disable();
                    });
                }
                return "{\"status\":\"ok\",\"macro\":\"" + macroId + "\",\"enabled\":" + enable + "}";
            } else if ("set_target".equals(action)) {
                String macroId = parts.length > 1 ? parts[1] : "";
                String target = parts.length > 2 ? parts[2] : "";
                Logger.sendLog("[IPC] Setting target for " + macroId + " -> " + target);

                if ("slayer".equalsIgnoreCase(macroId)) {
                    int index = 0;
                    if (target.contains("Tarantula")) index = 1;
                    else if (target.contains("Sven")) index = 2;
                    else if (target.contains("Voidgloom")) index = 3;
                    com.vertexai.Vertex.config().combat.slayerTarget = index;
                    com.vertexai.VertexClient.configManager.saveConfig();
                } else if ("mob_killer".equalsIgnoreCase(macroId)) {
                    int index = 0;
                    String lower = target.toLowerCase(java.util.Locale.ROOT);
                    if (lower.contains("ghost")) index = 1;
                    else if (lower.contains("ice walker")) index = 2;
                    else if (lower.contains("hoarder")) index = 3;
                    else if (lower.contains("goblin")) index = 4;
                    else if (lower.contains("glacite")) index = 5;
                    else if (lower.contains("automoton")) index = 6;
                    else if (lower.contains("sludge")) index = 7;
                    else if (lower.contains("yog")) index = 8;
                    else if (lower.contains("zombie")) index = 9;
                    else if (lower.contains("spider") || lower.contains("silverfish")) index = 10;
                    com.vertexai.Vertex.config().combat.mobKillerTarget = index;
                    com.vertexai.VertexClient.configManager.saveConfig();
                } else if ("foraging".equalsIgnoreCase(macroId)) {
                    int index = 0;
                    String lower = target.toLowerCase(java.util.Locale.ROOT);
                    if (lower.contains("acacia")) index = 1;
                    else if (lower.contains("jungle")) index = 2;
                    else if (lower.contains("spruce")) index = 3;
                    else if (lower.contains("oak") && !lower.contains("dark")) index = 4;
                    else if (lower.contains("birch")) index = 5;
                    com.vertexai.Vertex.config().foraging.foragingTreeType = index;
                    com.vertexai.VertexClient.configManager.saveConfig();
                }
                return "{\"status\":\"ok\",\"macro\":\"" + macroId + "\",\"target\":\"" + target + "\"}";
            } else if ("open_config_gui".equals(action)) {
                net.minecraft.client.Minecraft.getInstance().execute(() -> {
                    com.vertexai.config.ConfigGuiManager.openNativeConfigGui();
                });
                return "{\"status\":\"ok\"}";
            } else if ("get_config_schema".equals(action)) {
                return ConfigSerializer.serialize(VertexClient.config).toString();
            } else if ("click_button".equals(action)) {
                String[] btnParts = request.split(":", 3);
                if (btnParts.length >= 3) {
                    String catId = btnParts[1];
                    String fieldId = btnParts[2];
                    Logger.sendLog("[IPC] click_button received: category=" + catId + " field=" + fieldId);
                    return ConfigSerializer.executeButton(VertexClient.config, catId, fieldId).toString();
                }
                return "{\"status\":\"error\",\"message\":\"invalid_args\"}";
            } else if ("update_config".equals(action)) {
                String[] configParts = request.split(":", 4);
                if (configParts.length >= 3) {
                    String categoryId = configParts[1];
                    String fieldId = configParts[2];
                    String value = configParts.length > 3 ? configParts[3] : "";
                    Logger.sendLog("[IPC] update_config received: category=" + categoryId + " field=" + fieldId + " value=" + value);
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        ConfigSerializer.updateField(VertexClient.config, categoryId, fieldId, value);
                    });
                    return "{\"status\":\"ok\"}";
                }
                return "{\"status\":\"error\",\"message\":\"invalid_args\"}";
            } else if ("get_status".equals(action)) {
                String playerName = "Player";
                int fps = 0;
                try {
                    if (net.minecraft.client.Minecraft.getInstance().getUser() != null) {
                        playerName = net.minecraft.client.Minecraft.getInstance().getUser().getName();
                    }
                    fps = net.minecraft.client.Minecraft.getInstance().getFps();
                } catch (Throwable ignored) {}

                var active = MacroManager.getInstance().getActiveMacro();
                String macroName = active != null ? active.getName() : "None";
                boolean isRunning = MacroManager.getInstance().isRunning();
                String bpsStr = isRunning ? "20.0 BPS" : "0.0 BPS";
                String estProfitStr = isRunning ? "Calculating..." : "0 / hr";

                return String.format("{\"status\":\"ok\",\"playerName\":\"%s\",\"activeMacro\":\"%s\",\"isRunning\":%b,\"bps\":\"%s\",\"estProfit\":\"%s\",\"fps\":%d}",
                        playerName, macroName, isRunning, bpsStr, estProfitStr, fps);
            } else if ("spotify_auth".equals(action)) {
                com.vertexai.integration.spotify.SpotifyManager.getInstance().startAuthFlow();
                return "{\"status\":\"ok\"}";
            } else if ("spotify_play".equals(action)) {
                com.vertexai.integration.spotify.SpotifyManager.getInstance().play();
                return "{\"status\":\"ok\"}";
            } else if ("spotify_pause".equals(action)) {
                com.vertexai.integration.spotify.SpotifyManager.getInstance().pause();
                return "{\"status\":\"ok\"}";
            } else if ("spotify_toggle".equals(action)) {
                com.vertexai.integration.spotify.SpotifyManager.getInstance().togglePlay();
                return "{\"status\":\"ok\"}";
            } else if ("spotify_next".equals(action)) {
                com.vertexai.integration.spotify.SpotifyManager.getInstance().nextTrack();
                return "{\"status\":\"ok\"}";
            } else if ("spotify_prev".equals(action)) {
                com.vertexai.integration.spotify.SpotifyManager.getInstance().previousTrack();
                return "{\"status\":\"ok\"}";
            } else if ("spotify_get_status".equals(action)) {
                var s = com.vertexai.integration.spotify.SpotifyManager.getInstance();
                return String.format("{\"status\":\"ok\",\"authenticated\":%b,\"isPlaying\":%b,\"trackName\":\"%s\",\"artistName\":\"%s\",\"albumName\":\"%s\",\"albumArtUrl\":\"%s\",\"progressMs\":%d,\"durationMs\":%d}",
                        s.isAuthenticated(), s.isPlaying(),
                        s.getTrackName().replace("\"", "\\\""),
                        s.getArtistName().replace("\"", "\\\""),
                        s.getAlbumName().replace("\"", "\\\""),
                        s.getAlbumArtUrl(),
                        s.getProgressMs(), s.getDurationMs());
            }
        } catch (Exception e) {
            return "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
        }
        return "{\"status\":\"unknown_command\"}";
    }
}

