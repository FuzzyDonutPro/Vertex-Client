package com.vertexai.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class Utilities {

    @ConfigOption(
            name = "Sprint",
            desc = "Automatically sprints whenever walking forward"
    )
    @ConfigEditorBoolean
    public boolean sprint = false;
}
