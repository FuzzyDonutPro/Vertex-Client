package com.vertexai.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.*;
import com.vertexai.config.ConfigActions;
import org.lwjgl.glfw.GLFW;

public class General {

    @ConfigOption(
            name = "ENABLE MACRO",
            desc = "Start or Stop the currently selected macro instantly!"
    )
    @ConfigEditorButton(buttonText = "Toggle Macro")
    public transient Runnable enableMacroButton = ConfigActions::toggleMacro;

    @ConfigOption(
            name = "OPEN HUD EDITOR",
            desc = "Customize the layout and position of all overlay elements (Macro Status, Commission Tracker, etc.)"
    )
    @ConfigEditorButton(buttonText = "Edit HUD")
    public transient Runnable openHudEditorButton = ConfigActions::openHudEditor;

    @ConfigOption(
            name = "Macro Type",
            desc = "Select the macro type you want to use"
    )
    @ConfigEditorDropdown(
            values = {
                    "Dwarven Commission",
                    "Glacial Commissions",
                    "Mining Macro",
                    "Route Miner",
                    "Mithril Nuker",
                    "Slayer Macro",
                    "Farm Builder Macro",
                    "Kuudra Boss Suite"
            }
    )
    public int macroType = 0;

    @ConfigOption(
            name = "GUI Font",
            desc = "Select the typography font for the client interface (Inter, Outfit, Roboto, JetBrains Mono, Minecraft)"
    )
    @ConfigEditorDropdown(
            values = {
                    "Inter",
                    "Outfit",
                    "Roboto",
                    "JetBrains Mono",
                    "Minecraft"
            }
    )
    public int guiFont = 0;


    @ConfigOption(
            name = "Open Config GUI",
            desc = "Open the Vertex configuration screen"
    )
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_RIGHT_SHIFT)
    public int openConfigGuiKeybind = GLFW.GLFW_KEY_RIGHT_SHIFT;

    @ConfigOption(
            name = "Mining Tool",
            desc = "Mining tool that you use to mine blocks (and kill Ice/Glacite Walkers)"
    )
    @ConfigEditorText
    public String miningTool = "";

    @ConfigOption(
            name = "Set Mining Tool Button",
            desc = "Set the mining tool name from your currently held item"
    )
    @ConfigEditorButton(buttonText = "Set from hand")
    public transient Runnable setMiningToolButton = ConfigActions::setMiningTool;

    @ConfigOption(
            name = "Slayer Weapon",
            desc = "Main weapon for killing normal mobs (Treasure Hoarders, Goblins, etc.)"
    )
    @ConfigEditorText
    public String slayerWeapon = "";

    @ConfigOption(
            name = "Set Slayer Weapon Button",
            desc = "Set the slayer weapon name from your currently held item"
    )
    @ConfigEditorButton(buttonText = "Set from hand")
    public transient Runnable setSlayerWeaponButton = ConfigActions::setGeneralSlayerWeapon;

    @ConfigOption(name = "Sneak While Mining", desc = "")
    @ConfigEditorBoolean
    public boolean sneakWhileMining = false;

    @ConfigOption(
            name = "Use pickaxe ability",
            desc = "Only disable this if you are below HOTM 3"
    )
    @ConfigEditorBoolean
    public boolean usePickaxeAbility = true;

    @ConfigOption(
            name = "Precision Miner Info",
            desc = "You may turn off randomized rotations if you want to maximize the efficiency of precision miner"
    )
    @ConfigEditorInfoText
    public String precisionMinerInfo = "";

    @ConfigOption(
            name = "Precision Miner",
            desc = "Looks at particles spawned by precision miner perk"
    )
    @ConfigEditorBoolean
    public boolean precisionMiner = false;

    @ConfigOption(
            name = "Randomized rotations",
            desc = "Randomize rotations to make them look more human"
    )
    @ConfigEditorBoolean
    public boolean randomizedRotations = true;

    @ConfigOption(
            name = "Ore Respawn Wait Threshold (seconds)",
            desc = "How long to wait (in seconds) when no ores are present before stopping"
    )
    @ConfigEditorSlider(minValue = 2, maxValue = 10, minStep = 1)
    public int oreRespawnWaitThreshold = 5;

    @ConfigOption(name = "Enabled (Requires abiphone!)", desc = "")
    @ConfigEditorBoolean
    public boolean drillRefuel = false;

    @ConfigOption(name = "Machine Fuel", desc = "")
    @ConfigEditorDropdown(
            values = {"Volta", "Oil Barrel", "Sunflower Oil"}
    )
    public int refuelMachineFuel = 1;

    @ConfigOption(
            name = "Ungrab Mouse",
            desc = "Does not work for some Mac players"
    )
    @ConfigEditorBoolean
    public boolean ungrabMouse = false;

    @ConfigOption(name = "Mute Game", desc = "Mute Game")
    @ConfigEditorBoolean
    public boolean muteGame = true;

    @ConfigOption(
            name = "Full Block Hitbox",
            desc = "Gives a full block hitbox to blocks without a full block hitbox"
    )
    @ConfigEditorBoolean
    public boolean miscFullBlock = false;

    @ConfigOption(
            name = "Ignore Fall Damage In Pathfinding",
            desc = "OFF: pathfinding allows up to 17-block drops. ON: allow risky drops even if they can cause fall damage."
    )
    @ConfigEditorBoolean
    public boolean ignoreFallDamageInPathfinding = false;

    @ConfigOption(
            name = "Custom UI Sounds",
            desc = "Enable or disable custom UI sounds"
    )
    @ConfigEditorBoolean
    public boolean customUiSounds = true;
}
