package com.vertexai.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class Foraging {



    @ConfigOption(name = "Fig", desc = "The best method for general foraging no matter the gear you have. Supports jump boost, scaffolding, axe throwing, and more.")
    @ConfigEditorBoolean
    public boolean foragingFig = false;

    @ConfigOption(name = "Lushlilac", desc = "One of the best methods to get Whispers without actually foraging, running around Galatea breaking bushes.")
    @ConfigEditorBoolean
    public boolean foragingLushlilac = false;

    @ConfigOption(name = "Mangrove", desc = "An alternative to Fig foraging but for users with better gear and Sweep, breaking Mangrove trees in lower Galatea.")
    @ConfigEditorBoolean
    public boolean foragingMangrove = false;

    @ConfigOption(name = "Park", desc = "Select your foraging area and endlessly autowalk around breaking trees at high speed.")
    @ConfigEditorBoolean
    public boolean foragingPark = false;

    @ConfigOption(name = "Hub", desc = "Downgraded version of the Park Foraging script, but it's a good start and nice for new accounts.")
    @ConfigEditorBoolean
    public boolean foragingHub = false;
}
