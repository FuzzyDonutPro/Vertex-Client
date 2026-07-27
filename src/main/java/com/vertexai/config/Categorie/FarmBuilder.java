package com.vertexai.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class FarmBuilder {

    @ConfigOption(
            name = "Enable FarmBuilder",
            desc = "Master toggle for the Farm Builder macro"
    )
    @ConfigEditorBoolean
    public boolean enabled = false;

    @ConfigOption(
            name = "Pattern JSON File",
            desc = "The exact name of the JSON file in vertex/farm_patterns (e.g. s_shape_sugar_cane.json)"
    )
    @io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
    public String patternName = "s_shape_sugar_cane.json";

    @ConfigOption(
            name = "Plot Length",
            desc = "How many blocks long the plot is (Garden is typically 96)"
    )
    @ConfigEditorSlider(minValue = 10, maxValue = 160, minStep = 1)
    public int plotLength = 96;

    @ConfigOption(
            name = "Build Speed (ms delay)",
            desc = "Delay between major block placements to prevent lagbacks"
    )
    @ConfigEditorSlider(minValue = 0, maxValue = 500, minStep = 10)
    public int buildDelay = 50;
}
