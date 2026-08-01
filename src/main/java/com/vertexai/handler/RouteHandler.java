package com.vertexai.handler;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.Expose;
import lombok.Getter;
import com.vertexai.Vertex;
import com.vertexai.VertexClient;
import com.vertexai.feature.impl.RouteBuilder;
import com.vertexai.util.Logger;
import com.vertexai.util.helper.route.Route;
import com.vertexai.util.helper.route.RouteWaypoint;
import com.vertexai.util.helper.route.WaypointType;
import com.vertexai.util.WorldRenderContextWrapper;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class RouteHandler {
    private static final long SAVE_DEBOUNCE_MS = 250L;
    public static RouteHandler instance;
    private final Object saveLock = new Object();
    @Expose
    private final HashMap<String, Route> routes = new HashMap<String, Route>() {{
        put("Default", new Route());
    }};
    
    @Expose
    private final HashMap<String, Route> pathfinderRoutes = new HashMap<String, Route>() {{
        put("Default", new Route());
    }};
    
    private Route selectedRoute = this.routes.get("Default");
    private Route selectedPathfinderRoute = this.pathfinderRoutes.get("Default");
    private volatile boolean dirty = false;
    private long lastDirtyAtMs = 0L;
    private volatile boolean pathfinderDirty = false;
    private long lastPathfinderDirtyAtMs = 0L;

    public HashMap<String, Route> getRoutes() { return routes; }
    public HashMap<String, Route> getPathfinderRoutes() { return pathfinderRoutes; }
    public Route getSelectedRoute() { return selectedRoute; }
    public Route getSelectedPathfinderRoute() { return selectedPathfinderRoute; }

    public static RouteHandler getInstance() {
        if (instance == null) {
            instance = new RouteHandler();
        }
        return instance;
    }

    private static String normalizeRouteName(String routeName) {
        if (routeName == null) {
            return "";
        }
        return routeName.trim().replaceAll("\\s+", " ");
    }

    public void onWorldRender(WorldRenderContextWrapper context) {
        boolean shouldRender = isRouteRenderActive();
        if (!shouldRender || selectedRoute == null || selectedRoute.isEmpty()) {
            return;
        }

        selectedRoute.drawRoute();
    }

    public void selectRoute(String routeName) {
        String normalized = normalizeRouteName(routeName);
        if (normalized.isEmpty()) {
            return;
        }

        String resolved = resolveExistingRouteKey(normalized);
        String targetKey = resolved != null ? resolved : normalized;
        if (!this.routes.containsKey(targetKey)) {
            this.createRoute(targetKey);
        }
        this.selectedRoute = routes.get(targetKey);
        this.markDirty();
    }

    public boolean createRoute(String routeName) {
        String normalized = normalizeRouteName(routeName);
        if (normalized.isEmpty()) return false;
        if (resolveExistingRouteKey(normalized) != null) return false;
        this.routes.put(normalized, new Route());
        this.markDirty();
        return true;
    }

    public boolean addToCurrentRoute(final BlockPos block, final WaypointType method) {
        if (this.selectedRoute == this.routes.get("Default")) {
            Logger.sendError("Cannot edit Default route. Use /rb new <name> and then /rb select <name>.");
            return false;
        }

        if (block == null) {
            Logger.sendError("Cannot add waypoint because your standing block could not be resolved.");
            return false;
        }

        final RouteWaypoint waypoint = new RouteWaypoint(block, method);
        if (this.selectedRoute.indexOf(waypoint) != -1) {
            return false;
        }

        this.selectedRoute.insert(waypoint);
        this.markDirty();
        return true;
    }

    public void removeFromCurrentRoute(final int index) {
        this.selectedRoute.remove(index);
        this.markDirty();
    }

    public void replaceInCurrentRoute(final int index, final RouteWaypoint waypoint) {
        this.selectedRoute.replace(index, waypoint);
        this.markDirty();
    }

    public void deleteRoute(final String routeName) {
        String resolved = resolveExistingRouteKey(routeName);
        if (resolved == null) {
            return;
        }

        if (this.selectedRoute == this.routes.remove(resolved)) {
            this.selectedRoute = this.routes.get("Default");
            Vertex.config().routeMiner.selectedRoute = "";
            VertexClient.configManager.saveConfig();
        }

        this.markDirty();
    }

    public void selectPathfinderRoute(String routeName) {
        String normalized = normalizeRouteName(routeName);
        if (normalized.isEmpty()) return;

        String resolved = resolveExistingPathfinderRouteKey(normalized);
        String targetKey = resolved != null ? resolved : normalized;
        if (!this.pathfinderRoutes.containsKey(targetKey)) {
            this.createPathfinderRoute(targetKey);
        }
        this.selectedPathfinderRoute = pathfinderRoutes.get(targetKey);
        this.markPathfinderDirty();
    }

    public boolean createPathfinderRoute(String routeName) {
        String normalized = normalizeRouteName(routeName);
        if (normalized.isEmpty()) return false;
        if (resolveExistingPathfinderRouteKey(normalized) != null) return false;
        this.pathfinderRoutes.put(normalized, new Route());
        this.markPathfinderDirty();
        return true;
    }

    public boolean addToCurrentPathfinderRoute(final BlockPos block, final WaypointType method) {
        if (this.selectedPathfinderRoute == this.pathfinderRoutes.get("Default")) {
            Logger.sendError("Cannot edit Default route. Use /rbpf start <name>.");
            return false;
        }

        if (block == null) {
            Logger.sendError("Cannot add waypoint because your standing block could not be resolved.");
            return false;
        }

        final RouteWaypoint waypoint = new RouteWaypoint(block, method);
        if (this.selectedPathfinderRoute.indexOf(waypoint) != -1) {
            return false;
        }

        this.selectedPathfinderRoute.insert(waypoint);
        this.markPathfinderDirty();
        return true;
    }

    public void removeFromCurrentPathfinderRoute(final int index) {
        this.selectedPathfinderRoute.remove(index);
        this.markPathfinderDirty();
    }

    public void replaceInCurrentPathfinderRoute(final int index, final RouteWaypoint waypoint) {
        this.selectedPathfinderRoute.replace(index, waypoint);
        this.markPathfinderDirty();
    }

    public void deletePathfinderRoute(final String routeName) {
        String resolved = resolveExistingPathfinderRouteKey(routeName);
        if (resolved == null) return;

        if (this.selectedPathfinderRoute == this.pathfinderRoutes.remove(resolved)) {
            this.selectedPathfinderRoute = this.pathfinderRoutes.get("Default");
        }

        this.markPathfinderDirty();
    }

    private String resolveExistingRouteKey(String routeName) {
        if (routeName == null) {
            return null;
        }
        String normalized = normalizeRouteName(routeName);
        if (normalized.isEmpty()) {
            return null;
        }

        if (this.routes.containsKey(normalized)) {
            return normalized;
        }

        for (String key : this.routes.keySet()) {
            if (normalizeRouteName(key).equalsIgnoreCase(normalized)) {
                return key;
            }
        }

        return null;
    }

    public boolean hasRoute(String routeName) {
        return resolveExistingRouteKey(routeName) != null;
    }

    public int getRouteSize(String routeName) {
        String resolved = resolveExistingRouteKey(routeName);
        if (resolved == null) {
            return 0;
        }

        Route route = this.routes.get(resolved);
        return route == null ? 0 : route.size();
    }

    private String resolveExistingPathfinderRouteKey(String routeName) {
        if (routeName == null) return null;
        String normalized = normalizeRouteName(routeName);
        if (normalized.isEmpty()) return null;

        if (this.pathfinderRoutes.containsKey(normalized)) return normalized;

        for (String key : this.pathfinderRoutes.keySet()) {
            if (normalizeRouteName(key).equalsIgnoreCase(normalized)) return key;
        }
        return null;
    }

    public int getPathfinderRouteSize(String routeName) {
        String resolved = resolveExistingPathfinderRouteKey(routeName);
        if (resolved == null) return 0;
        Route route = this.pathfinderRoutes.get(resolved);
        return route == null ? 0 : route.size();
    }

    public String getSelectedRouteName() {
        for (Map.Entry<String, Route> entry : this.routes.entrySet()) {
            if (entry.getValue() == this.selectedRoute) {
                return entry.getKey();
            }
        }

        if (Vertex.config() != null) {
            String configured = normalizeRouteName(Vertex.config().routeMiner.selectedRoute);
            if (!configured.isEmpty()) {
                return configured;
            }
        }

        return "Default";
    }

    public void markDirty() {
        synchronized (saveLock) {
            this.dirty = true;
            this.lastDirtyAtMs = System.currentTimeMillis();
            saveLock.notifyAll();
        }
    }

    public void markPathfinderDirty() {
        synchronized (saveLock) {
            this.pathfinderDirty = true;
            this.lastPathfinderDirtyAtMs = System.currentTimeMillis();
            saveLock.notifyAll();
        }
    }

    public void loadData() {
        if (!Files.exists(Vertex.routesFile)) {
            ensureDefaultRoutePresent();
            rebindSelectedRouteFromConfig();
            return;
        }

        try (Reader reader = Files.newBufferedReader(Vertex.routesFile)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root == null || !root.isJsonObject()) {
                throw new IllegalStateException("routes.json root must be a JSON object");
            }
            JsonObject jsonObject = root.getAsJsonObject();
            JsonElement routesElement = jsonObject.has("routes")
                    ? jsonObject.get("routes")
                    : jsonObject;
            if (routesElement == null || routesElement.isJsonNull()) {
                throw new IllegalStateException("routes.json does not contain routes");
            }

            HashMap<String, Route> loadedRoutes = Vertex.gson.fromJson(
                    routesElement,
                    new TypeToken<HashMap<String, Route>>() {
                    }.getType()
            );
            if (loadedRoutes == null) {
                throw new IllegalStateException("routes.json contained null routes");
            }

            routes.clear();
            routes.putAll(loadedRoutes);
        } catch (Exception e) {
            Logger.sendWarning("Failed to load routes: " + Vertex.routesFile);
            Vertex.LOGGER.error("Failed to load routes: {}", Vertex.routesFile, e);
        } 
        
        if (Files.exists(Vertex.pathfinderRoutesFile)) {
            try (Reader reader = Files.newBufferedReader(Vertex.pathfinderRoutesFile)) {
                JsonElement root = JsonParser.parseReader(reader);
                if (root != null && root.isJsonObject()) {
                    JsonObject jsonObject = root.getAsJsonObject();
                    JsonElement routesElement = jsonObject.has("pathfinderRoutes")
                            ? jsonObject.get("pathfinderRoutes")
                            : jsonObject;
                            
                    if (routesElement != null && !routesElement.isJsonNull()) {
                        HashMap<String, Route> loadedPf = Vertex.gson.fromJson(
                                routesElement,
                                new TypeToken<HashMap<String, Route>>() {}.getType()
                        );
                        if (loadedPf != null) {
                            pathfinderRoutes.clear();
                            pathfinderRoutes.putAll(loadedPf);
                        }
                    }
                }
            } catch (Exception e) {
                Vertex.LOGGER.warn("Failed to load pathfinder routes from config: {}", Vertex.pathfinderRoutesFile);
            }
        } else {
            try (java.io.InputStream is = Vertex.class.getResourceAsStream("/pathfinder_routes.json")) {
                if (is != null) {
                    try (Reader reader = new java.io.InputStreamReader(is, StandardCharsets.UTF_8)) {
                        JsonElement root = JsonParser.parseReader(reader);
                        if (root != null && root.isJsonObject()) {
                            JsonObject jsonObject = root.getAsJsonObject();
                            JsonElement routesElement = jsonObject.has("pathfinderRoutes")
                                    ? jsonObject.get("pathfinderRoutes")
                                    : jsonObject;
                                    
                            if (routesElement != null && !routesElement.isJsonNull()) {
                                HashMap<String, Route> loadedPf = Vertex.gson.fromJson(
                                        routesElement,
                                        new TypeToken<HashMap<String, Route>>() {}.getType()
                                );
                                if (loadedPf != null) {
                                    pathfinderRoutes.clear();
                                    pathfinderRoutes.putAll(loadedPf);
                                }
                            }
                        }
                    }
                    Vertex.LOGGER.info("Loaded default pathfinder highways from mod resources.");
                }
            } catch (Exception e) {
                Vertex.LOGGER.warn("Failed to load default pathfinder routes from resources", e);
            }
        }
        
        ensureDefaultRoutePresent();
        rebindSelectedRouteFromConfig();
    }

    public synchronized void saveData() {
        while (RouteBuilder.getInstance().isRunning()) {
            try {
                boolean shouldSave;
                synchronized (saveLock) {
                    while (RouteBuilder.getInstance().isRunning() && !this.dirty) {
                        saveLock.wait(500L);
                    }
                    if (!RouteBuilder.getInstance().isRunning()) {
                        break;
                    }

                    long now = System.currentTimeMillis();
                    long waitMs = SAVE_DEBOUNCE_MS - (now - lastDirtyAtMs);
                    if (waitMs > 0L) {
                        saveLock.wait(Math.min(waitMs, 500L));
                    }

                    shouldSave = this.dirty;
                    this.dirty = false;
                }

                if (!shouldSave) {
                    continue;
                }
                
                JsonObject obj = new JsonObject();
                obj.add("routes", Vertex.gson.toJsonTree(this.routes));
                Files.write(Vertex.routesFile, Vertex.gson.toJson(obj).getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                Logger.sendWarning("Route save loop crashed; will retry");
                Vertex.LOGGER.error("Route save loop crashed", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public synchronized void savePathfinderData() {
        while (com.vertexai.feature.impl.PathfinderRouteBuilder.getInstance().isRunning()) {
            try {
                boolean shouldSave;
                synchronized (saveLock) {
                    while (com.vertexai.feature.impl.PathfinderRouteBuilder.getInstance().isRunning() && !this.pathfinderDirty) {
                        saveLock.wait(500L);
                    }
                    if (!com.vertexai.feature.impl.PathfinderRouteBuilder.getInstance().isRunning()) {
                        break;
                    }

                    long now = System.currentTimeMillis();
                    long waitMs = SAVE_DEBOUNCE_MS - (now - lastPathfinderDirtyAtMs);
                    if (waitMs > 0L) {
                        saveLock.wait(Math.min(waitMs, 500L));
                    }

                    shouldSave = this.pathfinderDirty;
                    this.pathfinderDirty = false;
                }

                if (!shouldSave) {
                    continue;
                }
                
                JsonObject obj = new JsonObject();
                obj.add("pathfinderRoutes", Vertex.gson.toJsonTree(this.pathfinderRoutes));
                String jsonStr = Vertex.gson.toJson(obj);
                Files.write(Vertex.pathfinderRoutesFile, jsonStr.getBytes(StandardCharsets.UTF_8));
                
                try {
                    java.nio.file.Path sourceFile = java.nio.file.Paths.get("C:\\Users\\jerem\\CLionProjects\\Vertex Client\\src\\main\\resources\\pathfinder_routes.json");
                    if (Files.exists(sourceFile.getParent())) {
                        Files.write(sourceFile, jsonStr.getBytes(StandardCharsets.UTF_8));
                    }
                } catch (Exception ignored) {
                    // Ignore if dev path is inaccessible
                }
            } catch (IOException e) {
                Logger.sendWarning("Pathfinder route save loop crashed; will retry");
                Vertex.LOGGER.error("Pathfinder route save loop crashed", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void onRender(WorldRenderContextWrapper context) {
        if (RouteBuilder.getInstance().isRunning() && this.selectedRoute != null && !this.selectedRoute.isEmpty()) {
            this.selectedRoute.drawRoute();
        }
        
        if (com.vertexai.feature.impl.PathfinderRouteBuilder.getInstance().isRunning() && this.selectedPathfinderRoute != null && !this.selectedPathfinderRoute.isEmpty()) {
            this.selectedPathfinderRoute.drawRoute();
        }
    }

    private boolean isRouteRenderActive() {
        return RouteBuilder.getInstance().isRunning();
    }

    private void ensureDefaultRoutePresent() {
        if (!this.routes.containsKey("Default")) {
            this.routes.put("Default", new Route());
        }
        if (!this.pathfinderRoutes.containsKey("Default")) {
            this.pathfinderRoutes.put("Default", new Route());
        }
    }

    private void rebindSelectedRouteFromConfig() {
        String configuredRouteName = "";
        if (Vertex.config() != null) {
            configuredRouteName = normalizeRouteName(Vertex.config().routeMiner.selectedRoute);
        }

        if (!configuredRouteName.isEmpty()) {
            String resolved = resolveExistingRouteKey(configuredRouteName);
            if (resolved != null) {
                this.selectedRoute = this.routes.get(resolved);
                return;
            }
        }

        this.selectedRoute = this.routes.get("Default");
    }
}
