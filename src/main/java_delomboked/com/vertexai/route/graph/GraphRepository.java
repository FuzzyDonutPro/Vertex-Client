package com.vertexai.route.graph;

import com.vertexai.Vertex;
import com.vertexai.util.Logger;
import com.vertexai.util.helper.graph.Graph;
import com.vertexai.util.helper.graph.GraphV2;
import com.vertexai.util.helper.route.RouteWaypoint;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class GraphRepository {

    private final Object fileWriteLock = new Object();

    public boolean writeGraphToDisk(String graphKey, Graph<RouteWaypoint> snapshot) {
        if (graphKey == null || graphKey.isEmpty() || snapshot == null) {
            return false;
        }

        final GraphV2 graphV2 = GraphV2.fromGraph(snapshot, graphKey, true);

        synchronized (fileWriteLock) {
            try (BufferedWriter writer = Files.newBufferedWriter(
                    Vertex.routesDirectory.resolve(graphKey + ".json"),
                    StandardCharsets.UTF_8
            )) {
                writer.write(Vertex.gson.toJson(graphV2));
                Logger.sendLog("Saved graph: " + graphKey);
                return true;
            } catch (Exception e) {
                Logger.sendLog("Failed to save graph: " + graphKey);
                return false;
            }
        }
    }
}
