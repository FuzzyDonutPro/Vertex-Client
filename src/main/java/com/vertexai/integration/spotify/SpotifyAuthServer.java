package com.vertexai.integration.spotify;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.vertexai.util.Logger;

import java.io.OutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class SpotifyAuthServer {

    private static HttpServer server;
    private static final int PORT = 8888;

    public interface AuthCallback {
        void onCodeReceived(String code);
        void onError(String error);
    }

    public static synchronized void start(AuthCallback callback) {
        stop();
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/callback", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    String query = exchange.getRequestURI().getQuery();
                    String code = null;
                    String error = null;

                    if (query != null) {
                        for (String param : query.split("&")) {
                            String[] pair = param.split("=");
                            if (pair.length == 2) {
                                if ("code".equals(pair[0])) {
                                    code = pair[1];
                                } else if ("error".equals(pair[0])) {
                                    error = pair[1];
                                }
                            }
                        }
                    }

                    String responseHtml;
                    if (code != null) {
                        responseHtml = "<html><body style='background:#121212;color:#1DB954;font-family:sans-serif;text-align:center;padding-top:100px;'>" +
                                "<h1>Vertex Client - Spotify Authenticated!</h1>" +
                                "<p style='color:#fff;'>You can now close this tab and return to Minecraft.</p>" +
                                "</body></html>";
                        exchange.sendResponseHeaders(200, responseHtml.getBytes().length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(responseHtml.getBytes());
                        os.close();

                        final String authCode = code;
                        new Thread(() -> callback.onCodeReceived(authCode)).start();
                    } else {
                        responseHtml = "<html><body style='background:#121212;color:#e74c3c;font-family:sans-serif;text-align:center;padding-top:100px;'>" +
                                "<h1>Authentication Failed</h1>" +
                                "<p style='color:#fff;'>" + (error != null ? error : "No authorization code received.") + "</p>" +
                                "</body></html>";
                        exchange.sendResponseHeaders(400, responseHtml.getBytes().length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(responseHtml.getBytes());
                        os.close();

                        final String err = error;
                        new Thread(() -> callback.onError(err != null ? err : "missing_code")).start();
                    }

                    stop();
                }
            });

            server.setExecutor(Executors.newSingleThreadExecutor());
            server.start();
            Logger.sendLog("[SpotifyAuth] Authorization server started on port " + PORT);
        } catch (Exception e) {
            Logger.sendLog("[SpotifyAuth] Failed to start local server: " + e.getMessage());
            callback.onError(e.getMessage());
        }
    }

    public static synchronized void stop() {
        if (server != null) {
            try {
                server.stop(0);
                server = null;
                Logger.sendLog("[SpotifyAuth] Authorization server stopped.");
            } catch (Exception ignored) {}
        }
    }
}
