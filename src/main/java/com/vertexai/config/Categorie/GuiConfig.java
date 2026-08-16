package com.vertexai.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
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
            name = "Theme Color",
            desc = "Hex color for the global HUD and Pathfinder theme (e.g. #38BDF8)"
    )
    @ConfigEditorText
    public String themeColor = "#38BDF8";

    public int getThemeColorInt() {
        try {
            return 0xFF000000 | Integer.parseInt(themeColor.replace("#", ""), 16);
        } catch (Exception e) {
            return 0xFF38BDF8;
        }
    }

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

    @ConfigOption(
            name = "Hand Chams",
            desc = "Render the held item with a solid, opaque glowing effect"
    )
    @ConfigEditorBoolean
    public boolean handChams = false;

    @ConfigOption(
            name = "Glow Amount",
            desc = "Intensity of the hand chams glow"
    )
    @ConfigEditorSlider(minValue = 0.0f, maxValue = 2.0f, minStep = 0.1f)
    public float chamsGlowAmount = 1.0f;

    @ConfigOption(
            name = "Glow Color",
            desc = "Hex color for the hand chams (e.g. #FF0000)"
    )
    @ConfigEditorText
    public String chamsGlowColor = "#00FFFF";

}
