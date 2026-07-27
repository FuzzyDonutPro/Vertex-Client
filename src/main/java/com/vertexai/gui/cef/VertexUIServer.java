package com.vertexai.gui.cef;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.vertexai.gui.web.MCEFBridge;
import com.vertexai.util.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class VertexUIServer {

    private static HttpServer server;
    public static final int PORT = 45678;

    public static synchronized void start() {
        if (server != null) return;
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
            server.createContext("/api/", new ApiHandler());
            server.createContext("/", new AssetHandler());
            server.setExecutor(null);
            server.start();
            Logger.sendLog("[VertexUIServer] Local UI Web Server running at http://127.0.0.1:" + PORT);
        } catch (Exception e) {
            Logger.sendLog("[VertexUIServer] Failed to start HTTP server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    static class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                String path = exchange.getRequestURI().getPath();
                String query = exchange.getRequestURI().getQuery();
                
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Content-Type", "application/json");

                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                String response = "{\"status\":\"ok\"}";
                if (path.endsWith("/macro/toggle")) {
                    String macroId = "";
                    boolean enabled = false;
                    if (query != null) {
                        for (String param : query.split("&")) {
                            String[] pair = param.split("=");
                            if (pair.length == 2) {
                                if ("id".equals(pair[0])) macroId = pair[1];
                                if ("enabled".equals(pair[0])) enabled = Boolean.parseBoolean(pair[1]);
                            }
                        }
                    }
                    Logger.sendLog("[VertexUIServer] Toggle macro API call: " + macroId + " -> " + enabled);
                    response = MCEFBridge.handleJsQuery("toggle_macro:" + macroId + ":" + enabled);
                } else if (path.endsWith("/config/gui")) {
                    response = MCEFBridge.handleJsQuery("open_config_gui");
                } else if (path.endsWith("/config/schema")) {
                    response = MCEFBridge.handleJsQuery("get_config_schema");
                } else if (path.endsWith("/config/update")) {
                    String categoryId = "";
                    String fieldId = "";
                    String val = "";
                    if (query != null) {
                        for (String param : query.split("&")) {
                            String[] pair = param.split("=");
                            if (pair.length == 2) {
                                if ("categoryId".equals(pair[0])) categoryId = pair[1];
                                if ("fieldId".equals(pair[0])) fieldId = pair[1];
                                if ("value".equals(pair[0])) val = pair[1];
                            }
                        }
                    }
                    Logger.sendLog("[VertexUIServer] Setting Config: cat=" + categoryId + " field=" + fieldId + " val=" + val);
                    response = MCEFBridge.handleJsQuery("update_config:" + categoryId + ":" + fieldId + ":" + val);
                } else if (path.endsWith("/status")) {
                    response = MCEFBridge.handleJsQuery("get_status");
                }

                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (Exception e) {
                try {
                    exchange.sendResponseHeaders(500, -1);
                } catch (Exception ignored) {}
            }
        }
    }

    static class AssetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                String path = exchange.getRequestURI().getPath();
                if (path.equals("/") || path.isEmpty()) {
                    path = "/index.html";
                }

                // Protect against directory traversal
                if (path.contains("..")) {
                    exchange.sendResponseHeaders(403, -1);
                    return;
                }

                String resourcePath = "assets/vertexai/gui/dist" + path;
                InputStream is = null;

                try {
                    if (Minecraft.getInstance() != null && Minecraft.getInstance().getResourceManager() != null) {
                        String relPath = resourcePath.substring("assets/vertexai/".length());
                        Identifier id = Identifier.fromNamespaceAndPath("vertexai", relPath);
                        is = Minecraft.getInstance().getResourceManager().getResource(id).map(r -> {
                            try {
                                return r.open();
                            } catch (Exception e) {
                                return null;
                            }
                        }).orElse(null);
                    }
                } catch (Throwable ignored) {}

                if (is == null) {
                    is = VertexUIServer.class.getClassLoader().getResourceAsStream(resourcePath);
                }

                if (is == null) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }

                String contentType = "text/plain";
                if (path.endsWith(".html")) contentType = "text/html; charset=UTF-8";
                else if (path.endsWith(".js")) contentType = "application/javascript; charset=UTF-8";
                else if (path.endsWith(".css")) contentType = "text/css; charset=UTF-8";
                else if (path.endsWith(".svg")) contentType = "image/svg+xml";
                else if (path.endsWith(".png")) contentType = "image/png";

                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, 0);

                try (OutputStream os = exchange.getResponseBody(); InputStream in = is) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                }
            } catch (Exception e) {
                try {
                    exchange.sendResponseHeaders(500, -1);
                } catch (Exception ignored) {}
            }
        }
    }
}

