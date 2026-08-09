package com.vertexai.command;

import com.vertexai.Vertex;
import com.vertexai.macro.impl.navigation.PathfinderRouteBuilder;
import com.vertexai.handler.RouteHandler;
import com.vertexai.util.KeyPressUtil;
import com.vertexai.util.Logger;
import com.vertexai.util.helper.route.WaypointType;
import net.minecraft.client.Minecraft;

import java.util.Objects;

public class PathfinderRouteBuilderCommand {

    private static String normalizeRouteName(String routeName) {
        if (routeName == null) {
            return "";
        }
        return routeName.trim().replaceAll("\\s+", " ");
    }

    public void main() {
        Logger.sendMessage("Use these commands to manage pathfinder highway routes.");
        info("   1. /rbpf list -> List all available highway routes.");
        info("   2. /rbpf start <route-name> -> Start recording a new or existing highway route.");
        info("   3. /rbpf stop -> Stop recording the current highway route.");
        info("   4. /rbpf add <walk|etherwarp> -> Add the block player is standing on to selected route.");
        info("   5. /rbpf remove <index> -> Remove the block player is standing on from selected route.");
        info("   6. /rbpf replace <index> <walk|etherwarp> -> Replaces Specified Index from the route.");
        info("   7. /rbpf delete <route-name> -> Deletes the route.");
        info("   8. /rbpf keys -> Show Pathfinder RouteBuilder keybinds.");
        info("   9. /rbpf select <route-name> -> Select the specified route name without starting recording.");
        keys();
    }

    public void keys() {
        var config = Vertex.config();
        if (config == null) {
            Logger.sendError("Config is not loaded yet.");
            return;
        }

        Logger.sendMessage("Pathfinder RouteBuilder keybinds:");
        info("   Add WALK waypoint: " + KeyPressUtil.getKeyName(config.routeMiner.routeBuilderWalkAddKeybind));
        info("   Add ETHERWARP waypoint: " + KeyPressUtil.getKeyName(config.routeMiner.routeBuilderEtherwarpAddKeybind));
        info("   Remove closest waypoint: " + KeyPressUtil.getKeyName(config.routeMiner.routeBuilderRemoveKeybind));
    }

    public void list() {
        StringBuilder sb = new StringBuilder();
        sb.append("Available Pathfinder Routes: ");

        RouteHandler.getInstance().getPathfinderRoutes().forEach((key, val) -> {
            String str = key;
            if (Objects.equals(RouteHandler.getInstance().getSelectedPathfinderRoute(), val)) str += "*";
            sb.append(str).append(", ");
        });

        Logger.sendMessage(sb.toString());
    }

    public void reload() {
        RouteHandler.getInstance().loadData();
        Logger.sendMessage("Refreshed routes file.");
    }

    public void select(final String routeName) {
        String normalized = normalizeRouteName(routeName);
        if (normalized.isEmpty()) {
            Logger.sendError("Route name cannot be empty.");
            return;
        }

        RouteHandler.getInstance().selectPathfinderRoute(normalized);
        int waypointCount = RouteHandler.getInstance().getPathfinderRouteSize(normalized);
        Logger.sendMessage("Selected highway: " + normalized + " (" + waypointCount + " waypoint" + (waypointCount == 1 ? "" : "s") + ")");
        if (waypointCount == 0) {
            Logger.sendWarning("Selected highway has no waypoints. Add one with P/I or /rbpf add <walk|etherwarp>.");
        }
    }

    public void create(final String routeName) {
        String normalized = normalizeRouteName(routeName);
        if (normalized.isEmpty()) {
            Logger.sendError("Route name cannot be empty.");
            return;
        }

        boolean created = RouteHandler.getInstance().createPathfinderRoute(normalized);
        this.select(normalized);
        if (created) {
            Logger.sendMessage("Created highway: " + normalized);
        } else {
            Logger.sendWarning("Highway already exists. Selected existing: " + normalized);
        }
    }

    public void start(final String routeName) {
        String normalized = normalizeRouteName(routeName);
        if (normalized.isEmpty()) {
            Logger.sendError("Route name cannot be empty.");
            return;
        }

        RouteHandler.getInstance().createPathfinderRoute(normalized);
        this.select(normalized);
        
        if (!PathfinderRouteBuilder.getInstance().isRunning()) {
            PathfinderRouteBuilder.getInstance().start();
        }
    }

    public void stop() {
        if (PathfinderRouteBuilder.getInstance().isRunning()) {
            PathfinderRouteBuilder.getInstance().stop();
        } else {
            Logger.sendMessage("Pathfinder RouteBuilder is not currently running.");
        }
    }

    public void add(final String name) {
        if (isRouteBuilderNotRunning()) return;
        WaypointType type = WaypointType.ETHERWARP;

        if (name.equalsIgnoreCase("walk")) {
            type = WaypointType.WALK;
        } else if (!name.equalsIgnoreCase("etherwarp")) {
            Logger.sendError("You must specify a proper option. Run /rbpf for more information.");
            return;
        }

        if (PathfinderRouteBuilder.getInstance().addToRoute(type)) {
            Logger.sendMessage("Added " + type.name().charAt(0) + type.name().substring(1).toLowerCase());
        }
    }

    public void remove(int index) {
        if (isRouteBuilderNotRunning()) return;
        PathfinderRouteBuilder.getInstance().removeFromRoute(index - 1);
        Logger.sendMessage("Removed point at index: " + index);
    }

    public void delete(final String routeName) {
        if (isRouteBuilderNotRunning()) return;
        String normalized = normalizeRouteName(routeName);
        if (normalized.isEmpty()) {
            Logger.sendError("Route name cannot be empty.");
            return;
        }
        RouteHandler.getInstance().deletePathfinderRoute(normalized);
        Logger.sendMessage("Deleted Highway: " + normalized);
    }

    public void replace(final int indexToReplace, final String name) {
        if (isRouteBuilderNotRunning()) return;
        if (indexToReplace <= 0) return;
        WaypointType type = WaypointType.ETHERWARP;

        if (name.equalsIgnoreCase("walk")) {
            type = WaypointType.WALK;
        } else if (!name.equalsIgnoreCase("etherwarp")) {
            Logger.sendError("You must specify a proper option. Run /rbpf for more information.");
            return;
        }

        PathfinderRouteBuilder.getInstance().replaceNode(indexToReplace - 1);
        Logger.sendMessage("Replaced index " + indexToReplace + " with " + type.name().charAt(0) + type.name().substring(1).toLowerCase());
    }

    private boolean isRouteBuilderNotRunning() {
        if (!PathfinderRouteBuilder.getInstance().isRunning()) {
            Logger.sendError("Pathfinder Route Builder is not enabled! Enable it via /rbpf start <name>.");
            return true;
        }
        return false;
    }

    private void info(final String message) {
        if (Minecraft.getInstance().player != null) {
            com.vertexai.util.Logger.sendMessage(message);
        }
    }

}
