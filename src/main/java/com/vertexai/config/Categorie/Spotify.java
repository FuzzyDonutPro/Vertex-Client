package com.vertexai.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import com.vertexai.integration.spotify.SpotifyManager;

public class Spotify {

    @ConfigOption(
            name = "Authenticate Spotify",
            desc = "Click to open Spotify OAuth login in your browser"
    )
    @ConfigEditorButton(buttonText = "Link Account")
    public transient Runnable authenticateSpotify = () -> SpotifyManager.getInstance().startAuthFlow();

    @ConfigOption(name = "Enable Spotify Integration", desc = "Master toggle for Spotify Web API integration")
    @ConfigEditorBoolean
    public boolean enableSpotify = true;

    @ConfigOption(name = "Custom Client ID", desc = "Optional Spotify Developer Client ID override")
    @ConfigEditorText
    public String customClientId = "";

    @ConfigOption(name = "Pause on Failsafe", desc = "Automatically pause Spotify music when a macro failsafe is triggered")
    @ConfigEditorBoolean
    public boolean pauseOnFailsafe = true;

    @ConfigOption(name = "Show Album Art on HUD", desc = "Display album artwork thumbnail in the in-game Spotify HUD")
    @ConfigEditorBoolean
    public boolean hudShowAlbumArt = true;

    @ConfigOption(name = "Show Progress Bar on HUD", desc = "Display playback progress bar in the in-game Spotify HUD")
    @ConfigEditorBoolean
    public boolean hudShowProgressBar = true;
}
