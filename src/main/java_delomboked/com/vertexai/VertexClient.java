package com.vertexai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.Command;
import com.vertexai.command.CommandManager;
import com.vertexai.config.ConfigGuiManager;
import com.vertexai.config.ConfigManager;
import com.vertexai.config.VertexConfig;
import com.vertexai.event.*;
import com.vertexai.failsafe.FailsafeManager;
import com.vertexai.feature.FeatureManager;
import com.vertexai.handler.GameStateHandler;
import com.vertexai.handler.GraphHandler;
import com.vertexai.handler.RotationHandler;
import com.vertexai.handler.RouteHandler;
import com.vertexai.macro.MacroManager;
import com.vertexai.ui.hud.HUDManager;
import com.vertexai.util.ChatPacketUtil;
import com.vertexai.util.Logger;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;


public class VertexClient implements ClientModInitializer {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final String MOD_ID = "vertexai";
    public static ConfigManager configManager;
    public static VertexConfig config;
    public static VertexClient instance;

    public final String VERSION = FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");

    public static Minecraft mc() {
        return Minecraft.getInstance();
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        Logger.sendLog("Initializing VertexMain...");

        // Initialize config
        initializeConfig();

        // Initialize managers (will be registered to events later)
        initializeManagers();

        // Register Fabric events
        EventManager.registerAll();

        // Register commands
        new CommandManager().registerAll();

        // Load routes after a tick to ensure world is ready
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level != null && !VertexMain.routesLoaded) {
                VertexMain.loadRoutes();
            }
        });

        Logger.sendLog("VertexMain initialized!");
    }

    private void initializeConfig() {
        configManager = new ConfigManager();
        configManager.firstLoad();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            if (configManager != null && config != null) {
                configManager.saveConfig();
            }
        });

        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> {
                    Command<FabricClientCommandSource> action = context -> {
                        Minecraft.getInstance().execute(() -> {
                            ConfigGuiManager.openConfigGui(null);
                        });
                        return 1;
                    };

                    dispatcher.register(literal("vertexai").executes(action));
                    dispatcher.register(literal("vf").executes(action));
                }
        );
    }

    private void initializeManagers() {
        // Managers are singletons - just access them to ensure they're created
        GameStateHandler.getInstance();
        RotationHandler.getInstance();
        RouteHandler.getInstance();
        GraphHandler.instance.toString(); // Static instance
        MacroManager.getInstance();
        FailsafeManager.getInstance();
        FeatureManager.getInstance();
        HUDManager.getInstance().loadPositions();

        PacketEvent.registerReceived(event -> Minecraft.getInstance().execute(() -> {
            FailsafeManager.getInstance().onPacketReceive(event.getPacket());
            MacroManager.getInstance().onPacketReceive(event);
            FeatureManager.getInstance().allFeatures.forEach(feature -> feature.handlePacketReceive(event.getPacket()));

            String message = ChatPacketUtil.extractMessage(event.getPacket());
            if (message != null) {
                FailsafeManager.getInstance().onChat(message);
                MacroManager.getInstance().onChat(message);
                FeatureManager.getInstance().allFeatures.forEach(feature -> feature.handleChat(message));
            }
        }));

        BlockChangeEvent.register(event -> Minecraft.getInstance().execute(() -> {
            FailsafeManager.getInstance().onBlockChange(event);
            FeatureManager.getInstance().allFeatures.forEach(feature -> feature.handleBlockChange(event));
        }));

        BlockDestroyEvent.register(event ->
                FeatureManager.getInstance().allFeatures.forEach(feature -> feature.handleBlockDestroy(event))
        );

        SpawnParticleEvent.register(event ->
                FeatureManager.getInstance().allFeatures.forEach(feature -> feature.handleParticleSpawn(event))
        );

        MotionUpdateEvent.register(RotationHandler.getInstance()::onMotionUpdate);
    }
}
