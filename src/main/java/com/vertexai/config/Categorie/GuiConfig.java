package com.vertexai.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
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

    @ConfigOption(
            name = "Swing Animation",
            desc = "Customize your first-person swinging style"
    )
    @ConfigEditorDropdown(values = {"Normal", "1.8", "Slow", "1.8 Slow", "Pitch"})
    public int swingAnimation = 1;

    @ConfigOption(
            name = "Swing Speed",
            desc = "Speed multiplier for the swing animation"
    )
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 5.0f, minStep = 0.1f)
    public float swingSpeed = 1.0f;

    @ConfigOption(
            name = "Item Scale",
            desc = "Scale of the held item in first person"
    )
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 2.0f, minStep = 0.05f)
    public float itemScale = 1.0f;

    @ConfigOption(
            name = "Item Position X",
            desc = "Horizontal offset of the held item"
    )
    @ConfigEditorSlider(minValue = -1.0f, maxValue = 1.0f, minStep = 0.01f)
    public float itemPosX = 0.0f;

    @ConfigOption(
            name = "Item Position Y",
            desc = "Vertical offset of the held item"
    )
    @ConfigEditorSlider(minValue = -1.0f, maxValue = 1.0f, minStep = 0.01f)
    public float itemPosY = 0.0f;

    @ConfigOption(
            name = "Item Position Z",
            desc = "Depth offset of the held item"
    )
    @ConfigEditorSlider(minValue = -1.0f, maxValue = 1.0f, minStep = 0.01f)
    public float itemPosZ = 0.0f;
}
