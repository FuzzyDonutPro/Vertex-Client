package com.vertexai.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class Farming {

    @ConfigOption(
            name = "Enable Crop/Wart S-Shape",
            desc = "Master toggle for S-Shape crop/wart farming macro"
    )
    @ConfigEditorBoolean
    public boolean enabled = false;

    @ConfigOption(
            name = "Farming Tool",
            desc = "Tool to hold while farming (e.g., Newton Nether Warts Pouch)"
    )
    @ConfigEditorText
    public String farmingTool = "";

    @ConfigOption(
            name = "Lane Shift Time (ms)",
            desc = "Failsafe: Maximum time to hold lane shift if edge detection fails"
    )
    @ConfigEditorSlider(minValue = 50, maxValue = 1000, minStep = 10)
    public int laneShiftTime = 250;
}
