package com.vertexai.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class Misc {

    @ConfigOption(
            name = "Left Autoclicker",
            desc = "Automatically clicks left mouse button while holding left click"
    )
    @ConfigEditorBoolean
    public boolean leftClicker = false;

    @ConfigOption(
            name = "Min Left CPS",
            desc = "Minimum clicks per second for the Left Autoclicker"
    )
    @ConfigEditorSlider(
            minValue = 1.0f,
            maxValue = 40.0f,
            minStep = 1.0f
    )
    public int minLeftCps = 8;

    @ConfigOption(
            name = "Max Left CPS",
            desc = "Maximum clicks per second for the Left Autoclicker"
    )
    @ConfigEditorSlider(
            minValue = 1.0f,
            maxValue = 40.0f,
            minStep = 1.0f
    )
    public int maxLeftCps = 12;

    @ConfigOption(
            name = "Right Autoclicker",
            desc = "Automatically clicks right mouse button while holding right click"
    )
    @ConfigEditorBoolean
    public boolean rightClicker = false;

    @ConfigOption(
            name = "Min Right CPS",
            desc = "Minimum clicks per second for the Right Autoclicker"
    )
    @ConfigEditorSlider(
            minValue = 1.0f,
            maxValue = 40.0f,
            minStep = 1.0f
    )
    public int minRightCps = 8;

    @ConfigOption(
            name = "Max Right CPS",
            desc = "Maximum clicks per second for the Right Autoclicker"
    )
    @ConfigEditorSlider(
            minValue = 1.0f,
            maxValue = 40.0f,
            minStep = 1.0f
    )
    public int maxRightCps = 12;

    @ConfigOption(
            name = "NoRender Mode (5x5 Area)",
            desc = "Only renders a 5x5 area around the player while a macro is running to save frames"
    )
    @ConfigEditorBoolean
    public boolean noRenderMode = false;
    @ConfigOption(
            name = "Fast Break Speed",
            desc = "Speed multiplier for Fast Break (1.0x = Normal, 2.0x = 2x Ticks Break Speed)"
    )
    @ConfigEditorSlider(
            minValue = 1.0f,
            maxValue = 2.0f,
            minStep = 0.1f
    )
    public float fastBreakSpeed = 1.5f;
}
