package com.vertexai.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class MelonPumpkin {

    @ConfigOption(
            name = "Custom Target Pitch",
            desc = "Override the AI's default 30-degree pitch for Melons/Pumpkins"
    )
    @ConfigEditorSlider(minValue = -90, maxValue = 90, minStep = 1)
    public int customPitch = 30;

}
