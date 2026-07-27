package com.vertexai.util.helper.graph;

import com.vertexai.util.helper.route.RouteWaypoint;

public record GraphNormalizationResult(Graph<RouteWaypoint> graph, GraphValidationResult validation) {

    public boolean hasChanges() {
        return validation.hasViolations();
    }
}
