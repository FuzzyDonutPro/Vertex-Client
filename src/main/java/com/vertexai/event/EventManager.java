package com.vertexai.event;

import com.vertexai.Vertex;
import com.vertexai.failsafe.FailsafeManager;
import com.vertexai.macro.AbstractFeature;
import com.vertexai.macro.FeatureManager;
import com.vertexai.macro.impl.navigation.RouteBuilder;
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
 * Central event manager that registers all Fabric callbacks with robust exception handling.
 */
public class EventManager {

    private static boolean registered = false;

    public static synchronized void registerAll() {
        if (registered) return;
        registered = true;

        registerInternalEventBus();
        registerTickEvents();
        registerRenderEvents();
        registerInputEvents();
    }

    private static void registerInternalEventBus() {
        UpdateScoreboardEvent.register(event -> {
            try {
                GameStateHandler.getInstance().onScoreboardUpdate(event);
            } catch (Throwable t) {
                if (Vertex.config() != null && Vertex.config().debug.debugMode) t.printStackTrace();
            }
        });

        UpdateScoreboardLineEvent.register(event -> {
            try {
                ScoreboardUtil.onScoreboardLineUpdate(event);
            } catch (Throwable t) {
                if (Vertex.config() != null && Vertex.config().debug.debugMode) t.printStackTrace();
            }
        });

        UpdateTablistEvent.register(event -> {
            try {
                TabListParser.updateCache();
                GameStateHandler.getInstance().onTablistUpdate(event);
                MacroManager.getInstance().onTablistUpdate(event);
                AbstractFeature[] active = FeatureManager.getInstance().getActiveFeatures();
                for (int i = 0; i < active.length; i++) {
                    if (active[i] != null) active[i].handleTablistUpdate(event);
                }
            } catch (Throwable t) {
                if (Vertex.config() != null && Vertex.config().debug.debugMode) t.printStackTrace();
            }
        });

        UpdateTablistFooterEvent.register(event -> {
            try {
                GameStateHandler.getInstance().onTablistFooterUpdate(event);
            } catch (Throwable t) {
                if (Vertex.config() != null && Vertex.config().debug.debugMode) t.printStackTrace();
            }
        });
    }

    private static void registerTickEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null || client.player == null) return;

            try {
                GameStateHandler.getInstance().onTick();
                RotationHandler.getInstance().onTick();
                GraphHandler.instance.onTick();
                MacroManager.getInstance().onTick();
                FailsafeManager.getInstance().onTick();

                AbstractFeature[] active = FeatureManager.getInstance().getActiveFeatures();
                for (int i = 0; i < active.length; i++) {
                    if (active[i] != null) active[i].handleTick();
                }

                ScoreboardUtil.update();
                TablistUtil.update();
            } catch (Throwable t) {
                if (Vertex.config() != null && Vertex.config().debug.debugMode) t.printStackTrace();
            }
        });
    }

    private static void registerRenderEvents() {
        WorldRenderEvents.END_MAIN.register(ctx -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;

            try {
                com.vertexai.util.WorldRenderContextWrapper context = new com.vertexai.util.WorldRenderContextWrapper(ctx);
                RenderUtil.beginWorldRender(context);
                RotationHandler.getInstance().onWorldRender(context);
                RouteHandler.getInstance().onWorldRender(context);
                GraphHandler.instance.onWorldRender(context);
                MacroManager.getInstance().onWorldRender(context);

                AbstractFeature[] active = FeatureManager.getInstance().getActiveFeatures();
                for (int i = 0; i < active.length; i++) {
                    if (active[i] != null) active[i].handleWorldRender(context);
                }

                RenderUtil.endWorldRender();
            } catch (Throwable t) {
                if (Vertex.config() != null && Vertex.config().debug.debugMode) t.printStackTrace();
            }
        });

        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((guiGraphics, tickCounter) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;

            try {
                HUDManager.getInstance().onHudRender(guiGraphics);
                MacroManager.getInstance().onHudRender(guiGraphics);

                AbstractFeature[] active = FeatureManager.getInstance().getActiveFeatures();
                for (int i = 0; i < active.length; i++) {
                    if (active[i] != null) active[i].handleHudRender(guiGraphics);
                }

                RenderUtil.renderQueuedLineOverlays(guiGraphics);
            } catch (Throwable t) {
                if (Vertex.config() != null && Vertex.config().debug.debugMode) t.printStackTrace();
            }
        });
    }

    private static void registerInputEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null || client.player == null) return;

            try {
                handleConfigGuiShortcut(client);
                handleRouteBuilderShortcut(client);
                handleFailsafeStopShortcut(client);
                GraphHandler.instance.onInput();
                MacroManager.getInstance().onInput();
            } catch (Throwable t) {
                if (Vertex.config() != null && Vertex.config().debug.debugMode) t.printStackTrace();
            }
        });
    }

    private static void handleFailsafeStopShortcut(Minecraft client) {
        var config = Vertex.config();
        if (config == null || config.failsafe == null) return;

        int key = config.failsafe.failsafeStopKeybind;
        if (key != 0 && KeyPressUtil.wasPressed(client.getWindow(), key, client.screen == null)) {
            if (FailsafeManager.getInstance().isFailsafeActiveOrTriggered()) {
                FailsafeManager.getInstance().stopFailsafes();
            }
        }
    }

    private static void handleConfigGuiShortcut(Minecraft client) {
        var config = Vertex.config();
        if (config == null) return;

        int key = config.general.openConfigGuiKeybind;
        if (KeyPressUtil.wasPressed(client.getWindow(), key, client.screen == null)) {
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
