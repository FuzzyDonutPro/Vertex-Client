package com.vertexai.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import org.lwjgl.glfw.GLFW;

public class Dungeons {



    @ConfigOption(
            name = "Dungeon Floor",
            desc = "Select which floor the AI should enter"
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

    @ConfigOption(name = "Auto-Secret Finder", desc = "Should the AI automatically scan for and claim secrets (chests, items, bats) in cleared rooms?")
    @ConfigEditorBoolean
    public boolean autoSecretFinder = true;

    @ConfigOption(name = "Auto-Blood Room", desc = "Should the AI camp the blood room and clear mobs as they spawn?")
    @ConfigEditorBoolean
    public boolean autoBloodRoom = true;
}
