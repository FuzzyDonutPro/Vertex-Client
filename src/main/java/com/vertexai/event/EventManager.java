package com.vertexai.event;

import com.vertexai.Vertex;
import com.vertexai.failsafe.FailsafeManager;
import com.vertexai.feature.AbstractFeature;
import com.vertexai.feature.FeatureManager;
import com.vertexai.feature.impl.RouteBuilder;
import com.vertexai.gui.VertexAIScreen;
import com.vertexai.handler.GameStateHandler;
import com.vertexai.handler.GraphHandler;
import com.vertexai.handler.RotationHandler;
import com.vertexai.handler.RouteHandler;
import com.vertexai.macro.MacroManager;
import com.vertexai.ui.hud.HUDManager;
import com.vertexai.util.KeyPressUtil;
import com.vertexai.util.RenderUtil;
import com.vertexai.util.ScoreboardUtil;
import com.vertexai.util.TablistUtil;
import com.vertexai.util.tablist.TabListParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;

/**
 * Central event manager that registers all Fabric callbacks.
 */
public class EventManager {

    public static void registerAll() {
        registerInternalEventBus();
        registerTickEvents();
        registerRenderEvents();
        registerInputEvents();
    }

    private static void registerInternalEventBus() {
        UpdateScoreboardEvent.register(GameStateHandler.getInstance()::onScoreboardUpdate);
        UpdateScoreboardLineEvent.register(ScoreboardUtil::onScoreboardLineUpdate);

        UpdateTablistEvent.register(event -> {
            TabListParser.updateCache();
            GameStateHandler.getInstance().onTablistUpdate(event);
            MacroManager.getInstance().onTablistUpdate(event);
            FeatureManager.getInstance().allFeatures.forEach(feature -> feature.handleTablistUpdate(event));
        });

        UpdateTablistFooterEvent.register(GameStateHandler.getInstance()::onTablistFooterUpdate);
    }

    private static void registerTickEvents() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.level == null || client.player == null) return;

            // Tick all managers
            GameStateHandler.getInstance().onTick();
            RotationHandler.getInstance().onTick();
            GraphHandler.instance.onTick();
            MacroManager.getInstance().onTick();
            FailsafeManager.getInstance().onTick();

            // Tick all features
            FeatureManager.getInstance().allFeatures.forEach(AbstractFeature::handleTick);

            // Update utilities
            ScoreboardUtil.update();
            TablistUtil.update();
            com.vertexai.dungeons.puzzles.DungeonTerminalSolver.getInstance().onTick();
        });
    }

    private static void registerRenderEvents() {
        WorldRenderEvents.END_MAIN.register(ctx -> {
            com.vertexai.util.WorldRenderContextWrapper context = new com.vertexai.util.WorldRenderContextWrapper(ctx);
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;

            RenderUtil.beginWorldRender(context);
            RotationHandler.getInstance().onWorldRender(context);
            RouteHandler.getInstance().onWorldRender(context);
            GraphHandler.instance.onWorldRender(context);
            MacroManager.getInstance().onWorldRender(context);
            FeatureManager.getInstance().allFeatures.forEach(feature -> feature.handleWorldRender(context));
            RenderUtil.endWorldRender();
        });

        // HUD rendering
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((guiGraphics, tickCounter) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;

            // Render high-res Svelte Chromium web overlay (StatusHUD, HUD widgets) only when macro is running
            if (mc.screen == null && com.vertexai.macro.MacroManager.getInstance().isRunning()) {
                com.vertexai.gui.cef.VertexCEFBrowser.getInstance().render(guiGraphics, 0, 0, 0);
            }

            HUDManager.getInstance().onHudRender(guiGraphics);
            MacroManager.getInstance().onHudRender(guiGraphics);
            FeatureManager.getInstance().allFeatures.forEach(feature -> feature.handleHudRender(guiGraphics));

            RenderUtil.renderQueuedLineOverlays(guiGraphics);
        });
    }

    private static void registerInputEvents() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.level == null || client.player == null) return;

            handleConfigGuiShortcut(client);
            handleRouteBuilderShortcut(client);
            handleFailsafeStopShortcut(client);
            GraphHandler.instance.onInput();
            MacroManager.getInstance().onInput();
        });
    }

    private static void handleFailsafeStopShortcut(Minecraft client) {
        var config = Vertex.config();
        if (config == null || config.failsafe == null) return;

        int key = config.failsafe.failsafeStopKeybind;
        if (key != 0 && com.vertexai.util.KeyPressUtil.wasPressed(client.getWindow(), key, client.screen == null)) {
            if (com.vertexai.failsafe.FailsafeManager.getInstance().isFailsafeActiveOrTriggered()) {
                com.vertexai.failsafe.FailsafeManager.getInstance().stopFailsafes();
            }
        }
    }

    private static void handleConfigGuiShortcut(Minecraft client) {
        var config = Vertex.config();
        if (config == null) return;

        int key = config.general.openConfigGuiKeybind; // Default: GLFW_KEY_RIGHT_SHIFT
        if (KeyPressUtil.wasPressed(client.getWindow(), key, client.screen == null)) {
            System.out.println("[vertexai/DEBUG] EventManager: KeyPressUtil detected openConfigGuiKeybind (" + key + ")! Opening WebDashboardScreen...");
            client.setScreen(new com.vertexai.gui.web.WebDashboardScreen());
        }
    }

    private static void handleRouteBuilderShortcut(Minecraft client) {
        var config = Vertex.config();
        if (config == null) return;

        int key = config.routeMiner.routeBuilder;
        boolean pressed = KeyPressUtil.wasPressed(client.getWindow(), key, client.screen == null);
        if (pressed) {
            RouteBuilder.getInstance().toggle();
        }
    }
}
