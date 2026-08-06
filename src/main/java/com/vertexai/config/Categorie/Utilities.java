package com.vertexai.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class Utilities {

    @ConfigOption(
            name = "Sprint",
            desc = "Automatically sprints whenever walking forward"
    )
    @ConfigEditorBoolean
    public boolean sprint = false;

    @ConfigOption(
            name = "World ESP",
            desc = "Enable 3D ESP rendering for mining targets, chests, and visitors"
    )
    @ConfigEditorBoolean
    public boolean enableWorldEsp = true;

    @ConfigOption(
            name = "ESP Render Through Walls",
            desc = "Render ESP boxes through blocks and walls"
    )
    @ConfigEditorBoolean
    public boolean renderEspThroughWalls = true;

    @ConfigOption(
            name = "Pathfinder Render Through Walls",
            desc = "Render pathfinding lines and nodes through blocks and walls"
    )
    @ConfigEditorBoolean
    public boolean renderPathfinderThroughWalls = true;
}
