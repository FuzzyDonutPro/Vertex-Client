package com.vertexai.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class Animations {

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
