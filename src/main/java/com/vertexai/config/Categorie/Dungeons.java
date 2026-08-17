package com.vertexai.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class Dungeons {

    @ConfigOption(
            name = "Dungeon Floor",
            desc = "Select which floor the AI should enter (0 = Entrance, 1 = F1 ... 7 = F7)"
    )
    @ConfigEditorDropdown(
            values = {
                    "Entrance",
                    "Floor 1",
                    "Floor 2",
                    "Floor 3",
                    "Floor 4",
                    "Floor 5",
                    "Floor 6",
                    "Floor 7"
            }
    )
    public int dungeonFloor = 1;

    @ConfigOption(name = "Auto-Clear Rooms", desc = "Should the AI automatically hunt down and kill mobs in uncleared rooms?")
    @ConfigEditorBoolean
    public boolean autoClearRooms = true;

    @ConfigOption(name = "Auto-Door Navigation", desc = "Automatically pathfind through doors and unlock Wither / Blood doors?")
    @ConfigEditorBoolean
    public boolean autoDoorNavigation = true;

    @ConfigOption(name = "Auto-Key Collector", desc = "Automatically detect and pick up dropped Wither Keys and Blood Keys?")
    @ConfigEditorBoolean
    public boolean autoKeyCollector = true;

    @ConfigOption(name = "Auto-Secret Finder", desc = "Should the AI automatically scan for and claim secrets (chests, items, bats) in cleared rooms?")
    @ConfigEditorBoolean
    public boolean autoSecretFinder = true;

    @ConfigOption(name = "Auto-Blood Room", desc = "Should the AI camp the blood room and clear mobs as they spawn?")
    @ConfigEditorBoolean
    public boolean autoBloodRoom = true;

    @ConfigOption(name = "F7 Boss Phase Automation", desc = "Automate Floor 7 Maxor crystals, Storm pads, Goldor terminals, and Necron DPS?")
    @ConfigEditorBoolean
    public boolean f7BossAutomation = true;

    @ConfigOption(name = "Auto-Terminal Solver", desc = "Automatically solve all F7 GUI terminals (Numbers 1-14, Colors, Letters, Rubix)?")
    @ConfigEditorBoolean
    public boolean autoTerminalSolver = true;

    @ConfigOption(name = "Terminal Click Delay (ms)", desc = "Humanized delay between terminal clicks in milliseconds")
    @ConfigEditorSlider(minValue = 60, maxValue = 300, minStep = 10)
    public int terminalClickDelay = 120;
}
