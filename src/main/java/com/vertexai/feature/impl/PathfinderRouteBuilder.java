package com.vertexai.feature.impl;

import com.vertexai.Vertex;
import com.vertexai.feature.AbstractFeature;
import com.vertexai.handler.RouteHandler;
import com.vertexai.util.KeyPressUtil;
import com.vertexai.util.Logger;
import com.vertexai.util.PlayerUtil;
import com.vertexai.util.helper.route.Route;
import com.vertexai.util.helper.route.RouteWaypoint;
import com.vertexai.util.helper.route.WaypointType;
import net.minecraft.client.Minecraft;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PathfinderRouteBuilder extends AbstractFeature {

    private static PathfinderRouteBuilder instance;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static PathfinderRouteBuilder getInstance() {
        if (instance == null) {
            instance = new PathfinderRouteBuilder();
        }
        return instance;
    }

    @Override
    public String getName() {
        return "PathfinderRouteBuilder";
    }

    public void toggle() {
        if (!this.enabled) {
            this.start();
        } else {
            this.stop();
        }
    }

    @Override
    public void start() {
        this.enabled = true;
        scheduler.schedule(
                RouteHandler.getInstance()::savePathfinderData,
                0,
                TimeUnit.MILLISECONDS
        );
        send("Enabling Pathfinder RouteBuilder.");
    }

    @Override
    public void stop() {
        this.enabled = false;
        send("Disabling Pathfinder RouteBuilder.");
    }

    @Override
    protected void onTick() {
        var config = Vertex.config();
        if (config == null) {
            return;
        }

        com.mojang.blaze3d.platform.Window window = Minecraft.getInstance().getWindow();
        boolean walkPressed = KeyPressUtil.wasPressed(window, config.routeMiner.routeBuilderWalkAddKeybind, this.enabled);
        boolean etherwarpPressed = KeyPressUtil.wasPressed(window, config.routeMiner.routeBuilderEtherwarpAddKeybind, this.enabled);
        boolean removePressed = KeyPressUtil.wasPressed(window, config.routeMiner.routeBuilderRemoveKeybind, this.enabled);

        if (!this.enabled) {
            return;
        }

        if (walkPressed) {
            if (this.addToRoute(WaypointType.WALK)) {
                Logger.sendMessage("Added Pathfinder Walk Node");
            }
        }

        if (etherwarpPressed) {
            if (this.addToRoute(WaypointType.ETHERWARP)) {
                Logger.sendMessage("Added Pathfinder Etherwarp Node");
            }
        }

        if (removePressed) {
            Route selectedRoute = RouteHandler.getInstance().getSelectedPathfinderRoute();
            if (selectedRoute.isEmpty()) {
                return;
            }

            if (PlayerUtil.getBlockStandingOn() != null) {
                Optional<RouteWaypoint> closest = selectedRoute.getClosest(PlayerUtil.getBlockStandingOn());
                if (!closest.isPresent()) {
                    return;
                }

                int index = selectedRoute.indexOf(closest.get());

                if (index == -1) {
                    return;
                }

                this.removeFromRoute(index);
                Logger.sendMessage("Removed Pathfinder Node");
            }
        }
    }

    public boolean addToRoute(final WaypointType method) {
        return RouteHandler.getInstance().addToCurrentPathfinderRoute(
                PlayerUtil.getBlockStandingOn(),
                method
        );
    }

    public void removeFromRoute(int index) {
        RouteHandler.getInstance().removeFromCurrentPathfinderRoute(index);
    }

    public void replaceNode(final int index) {
        RouteHandler.getInstance().replaceInCurrentPathfinderRoute(
                index,
                new RouteWaypoint(
                        PlayerUtil.getBlockStandingOn(),
                        WaypointType.ETHERWARP
                )
        );
    }
}
