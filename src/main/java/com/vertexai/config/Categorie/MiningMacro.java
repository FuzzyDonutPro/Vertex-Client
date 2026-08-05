package com.vertexai.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class MiningMacro {

    @ConfigOption(name = "Ore Type", desc = "")
    @ConfigEditorDropdown(
            values = {
                    "Mithril",
                    "Diamond",
                    "Emerald",
                    "Redstone",
                    "Lapis",
                    "Gold",
                    "Iron",
                    "Coal",
            }
    )
    public int oreType = 0;

    @ConfigOption(name = "Mine Gray Mithril (Gray Wool)", desc = "Target Gray Wool Mithril blocks in Dwarven Mines")
    @ConfigEditorBoolean
    public boolean mineGrayMithril = true;

    @ConfigOption(name = "Mine Gray Terracotta Mithril", desc = "Target Cyan & Gray Terracotta Mithril blocks in Dwarven Mines")
    @ConfigEditorBoolean
    public boolean mineGrayTerracottaMithril = true;

    @ConfigOption(name = "Mine Green Mithril (Prismarine)", desc = "")
    @ConfigEditorBoolean
    public boolean mineGreenMithril = true;

    @ConfigOption(name = "Mine Blue Mithril (Blue Wool)", desc = "")
    @ConfigEditorBoolean
    public boolean mineBlueMithril = true;

    @ConfigOption(name = "Mine Titanium", desc = "")
    @ConfigEditorBoolean
    public boolean mineTitanium = true;

    @ConfigOption(name = "Default Priority - Gray Mithril", desc = "")
    @ConfigEditorSlider(minValue = 0, maxValue = 30, minStep = 1)
    public int mithrilPriorityGrayDefault = 1;

    @ConfigOption(name = "Default Priority - Green Mithril", desc = "")
    @ConfigEditorSlider(minValue = 0, maxValue = 30, minStep = 1)
    public int mithrilPriorityGreenDefault = 3;

    @ConfigOption(name = "Default Priority - Blue Mithril", desc = "")
    @ConfigEditorSlider(minValue = 0, maxValue = 30, minStep = 1)
    public int mithrilPriorityBlueDefault = 6;

    @ConfigOption(name = "Allow Pathfinder Walking", desc = "Use A* pathfinder to walk toward blocks that are out of immediate mining reach (> 3.5 blocks distance)")
    @ConfigEditorBoolean
    public boolean allowPathfinder = true;

    @ConfigOption(name = "Pathfinder Mode", desc = "Minimal: Max 5-block distance limit from starting position + sub-block sneak precision. Normal: Standard pathfinder.")
    @ConfigEditorDropdown(
            values = {
                    "Minimal",
                    "Normal"
            }
    )
    public int pathfinderMode = 0; // 0 = Minimal, 1 = Normal

    @ConfigOption(name = "Default Priority - Titanium", desc = "")
    @ConfigEditorSlider(minValue = 0, maxValue = 30, minStep = 1)
    public int mithrilPriorityTitaniumDefault = 10;
}
