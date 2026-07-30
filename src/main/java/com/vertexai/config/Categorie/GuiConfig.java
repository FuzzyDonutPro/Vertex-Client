package com.vertexai.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import com.vertexai.config.ConfigActions;

public class GuiConfig {

    @ConfigOption(
            name = "GUI Font Typography",
            desc = "Select the interface font (Inter, Outfit, Roboto, JetBrains Mono, Minecraft)"
    )
    @ConfigEditorDropdown(
            values = {"Inter", "Outfit", "Roboto", "JetBrains Mono", "Minecraft"}
    )
    public int guiFont = 0;

    @ConfigOption(
            name = "HUD Editor",
            desc = "Open the HUD editor to move, scale, and resize HUD elements"
    )
    @ConfigEditorButton(buttonText = "Open HUD Editor")
    public transient Runnable openHudEditor = ConfigActions::openHudEditor;

    @ConfigOption(
            name = "UI Sound Style",
            desc = "Choose which sound pack to use for the GUI"
    )
    @ConfigEditorDropdown(
            values = {"Muted", "Mechanical Click", "Bubbly Pop", "Subtle Chime"}
    )
    public int uiSoundStyle = 1;

    @ConfigOption(
            name = "Free Look Keybind",
            desc = "Hold or press to detach your camera"
    )
    @io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind(defaultKey = org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT)
    public int freeLookKeybind = org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT;

    @ConfigOption(
            name = "Free Look Mode",
            desc = "Whether to Hold the key or Toggle it"
    )
    @ConfigEditorDropdown(values = {"Hold", "Toggle"})
    public int freeLookMode = 0;

}
