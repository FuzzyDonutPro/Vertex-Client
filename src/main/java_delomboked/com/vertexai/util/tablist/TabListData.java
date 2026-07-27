package com.vertexai.util.tablist;

import com.vertexai.VertexClient;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TabListData {

    public static final TabListData EMPTY = new TabListData();
    public Set<WidgetType> activeWidgets;
    public Map<WidgetType, List<String>> widgetLines = new EnumMap<>(
            WidgetType.class
    );

    public String serialize() {
        return VertexClient.GSON.toJson(this);
    }
}
