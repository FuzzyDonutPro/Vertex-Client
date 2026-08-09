package com.vertexai.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import com.vertexai.config.ConfigActions;
import com.vertexai.config.VertexConfig;

public class HUD {

    @ConfigOption(
            name = "HUD Editor",
            desc = "Open the HUD editor to move/anchor HUD elements"
    )
    @ConfigEditorButton(buttonText = "Open")
    public transient Runnable openHudEditor = ConfigActions::openHudEditor;

    @ConfigOption(name = "Enable Status HUD", desc = "Show status overlay with profit, current state, and macro time")
    @ConfigEditorBoolean
    public boolean enableStatusHud = true;
    public VertexConfig.HUDPos statusHUD = new VertexConfig.HUDPos(5, 5, 0, 1.0f);

    @ConfigOption(name = "Enable Inventory HUD", desc = "Show inventory overlay on top of hotbar")
    @ConfigEditorBoolean
    public boolean enableInventoryHud = false;
    public VertexConfig.HUDPos inventoryHUD = new VertexConfig.HUDPos(0, 52, 2, 1.0f);

    @ConfigOption(name = "Enable Profit HUD", desc = "Show Bazaar profit tracker and item count overlay")
    @ConfigEditorBoolean
    public boolean enableProfitHud = false;
    public VertexConfig.HUDPos profitHUD = new VertexConfig.HUDPos(5, 120, 0, 1.0f);

    @ConfigOption(name = "Enable Commission HUD", desc = "Show the commission overlay")
    @ConfigEditorBoolean
    public boolean enableCommissionHud = true;
    public VertexConfig.HUDPos commissionHUD = new VertexConfig.HUDPos(5, 5, 0, 1.0f);

    @ConfigOption(name = "Enable Glacial HUD", desc = "Show the glacial commission overlay")
    @ConfigEditorBoolean
    public boolean enableGlacialHud = false;
    public VertexConfig.HUDPos glacialHUD = new VertexConfig.HUDPos(5, 5, 0, 1.0f);

    @ConfigOption(name = "Enable Debug HUD", desc = "Show debug overlay")
    @ConfigEditorBoolean
    public boolean enableDebugHud = false;
    public VertexConfig.HUDPos debugHUD = new VertexConfig.HUDPos(1, 10, 0, 1.0f);

    @ConfigOption(name = "Enable RouteBuilder HUD", desc = "Show RouteBuilder edit overlay")
    @ConfigEditorBoolean
    public boolean enableRouteBuilderHud = true;
    public VertexConfig.HUDPos routeBuilderHUD = new VertexConfig.HUDPos(5, 90, 0, 1.0f);

    @ConfigOption(name = "Enable Fishing HUD", desc = "Show Galatea fishing runtime details")
    @ConfigEditorBoolean
    public boolean enableFishingHud = true;
    public VertexConfig.HUDPos fishingHUD = new VertexConfig.HUDPos(5, 185, 0, 1.0f);

    @ConfigOption(name = "Enable Spotify HUD", desc = "Show Spotify media player overlay")
    @ConfigEditorBoolean
    public boolean enableSpotifyHud = true;
    public VertexConfig.HUDPos spotifyHUD = new VertexConfig.HUDPos(5, 230, 0, 1.0f);

    @ConfigOption(name = "Enable Mining HUD", desc = "Show custom Mining Macro overlay with status, target, speed, and rates")
    @ConfigEditorBoolean
    public boolean enableMiningHud = true;
    public VertexConfig.HUDPos miningHUD = new VertexConfig.HUDPos(5, 130, 0, 1.0f);
}
