package com.vertexai.gui.cef;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.vertexai.VertexClient;
import com.vertexai.gui.web.MCEFBridge;
import com.vertexai.util.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class VertexUIServer {

    public static final int PORT = 45678;
    private static HttpServer server;

    public static synchronized void start() {
        if (server != null) return;

        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
            server.createContext("/", new AssetHandler());
            server.setExecutor(null);
            server.start();
            Logger.sendLog("[VertexUIServer] HTTP Local Server started at http://127.0.0.1:" + PORT);
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

    private static class AssetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                String path = exchange.getRequestURI().getPath();
                String query = exchange.getRequestURI().getQuery();

                // CORS headers for all local requests
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "*");

                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                // Handle API Endpoints
                if (path.startsWith("/api/")) {
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    String response = "{}";
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
                        response = MCEFBridge.handleJsQuery("update_config:" + categoryId + ":" + fieldId + ":" + val);
                    } else if (path.endsWith("/status")) {
                        response = MCEFBridge.handleJsQuery("get_status");
                    }

                    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                    return;
                }

                // Serve Static Web Assets (Vite build)
                if (path.equals("/")) {
                    path = "/index.html";
                }

                String resourcePath = "assets/vertexai/gui/dist" + path;
                InputStream is = VertexUIServer.class.getClassLoader().getResourceAsStream(resourcePath);

                if (is == null) {
                    try {
                        Identifier id = Identifier.parse("vertexai:gui/dist" + path);
                        var resource = Minecraft.getInstance().getResourceManager().getResource(id);
                        if (resource.isPresent()) {
                            is = resource.get().open();
                        }
                    } catch (Throwable ignored) {}
                }

                if (is == null) {
                    String notFound = "404 Not Found";
                    exchange.sendResponseHeaders(404, notFound.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(notFound.getBytes(StandardCharsets.UTF_8));
                    }
                    return;
                }

                String contentType = "text/html; charset=UTF-8";
                if (path.endsWith(".css")) {
                    contentType = "text/css; charset=UTF-8";
                } else if (path.endsWith(".js")) {
                    contentType = "application/javascript; charset=UTF-8";
                } else if (path.endsWith(".png")) {
                    contentType = "image/png";
                } else if (path.endsWith(".svg")) {
                    contentType = "image/svg+xml";
                } else if (path.endsWith(".json")) {
                    contentType = "application/json; charset=UTF-8";
                }

                exchange.getResponseHeaders().set("Content-Type", contentType);
                byte[] content = is.readAllBytes();
                is.close();

                exchange.sendResponseHeaders(200, content.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(content);
                }

            } catch (Exception e) {
                try {
                    exchange.sendResponseHeaders(500, -1);
                } catch (Exception ignored) {}
            }
        }
    }
}
