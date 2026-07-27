package com.vertexai.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;

public class Combat {

    @ConfigOption(name = "Kill Ghosts", desc = "Kill Ghosts in the Dwarven Mines")
    @ConfigEditorBoolean
    public boolean slayerGhosts = false;

    @ConfigOption(name = "Kill Ice Walkers", desc = "Kill Ice Walkers in the Dwarven Mines")
    @ConfigEditorBoolean
    public boolean slayerIceWalkers = false;

    @ConfigOption(name = "Kill Treasure Hoarders", desc = "Kill Treasure Hoarders in the Dwarven Mines")
    @ConfigEditorBoolean
    public boolean slayerTreasureHoarders = false;

    @ConfigOption(name = "Kill Goblins", desc = "Kill Goblins in the Dwarven Mines")
    @ConfigEditorBoolean
    public boolean slayerGoblins = false;

    @ConfigOption(name = "Kill Glacite Walkers", desc = "Kill Glacite Walkers in the Glacite Tunnels")
    @ConfigEditorBoolean
    public boolean slayerGlaciteWalkers = false;

    @ConfigOption(name = "Kill Automotons", desc = "Kill Automotons in the Crystal Hollows")
    @ConfigEditorBoolean
    public boolean slayerAutomotons = false;

    @ConfigOption(name = "Kill Sludge", desc = "Kill Sludge in the Jungle")
    @ConfigEditorBoolean
    public boolean slayerSludge = false;

    @ConfigOption(name = "Kill Yog", desc = "Kill Yog in the Magma Fields")
    @ConfigEditorBoolean
    public boolean slayerYog = false;

    @ConfigOption(name = "Kill Zealots (Melee)", desc = "Kill Zealots in the Dragon's Nest or Bruiser Hideout")
    @ConfigEditorBoolean
    public boolean slayerZealots = false;

    @ConfigOption(name = "Crypt / Rev Slayer", desc = "Kill Crypt Ghouls for Revenant Slayers")
    @ConfigEditorBoolean
    public boolean slayerRev = false;

    @ConfigOption(name = "Wolf / Sven Slayer", desc = "Kill Wolves in the Park or Ruins for Sven Slayers")
    @ConfigEditorBoolean
    public boolean slayerSven = false;

    @ConfigOption(name = "Spider / Tara Slayer", desc = "Kill Spiders in the Spider's Den or Crimson Isle for Tarantula Slayers")
    @ConfigEditorBoolean
    public boolean slayerTara = false;

    @ConfigOption(name = "Graveyard Zombies", desc = "Kill Zombies in the Graveyard for Bestiary")
    @ConfigEditorBoolean
    public boolean slayerGraveyard = false;

    @ConfigOption(name = "Auto-Heal", desc = "Automatically swap to a healing item and use it when health is low")
    @ConfigEditorBoolean
    public boolean autoHealEnabled = true;

    @ConfigOption(name = "Healing Item", desc = "Name of the healing item in your hotbar (e.g., Florid Zombie Sword)")
    @ConfigEditorText
    public String healingItem = "Florid Zombie Sword";

    @ConfigOption(name = "Auto-Heal Threshold (%)", desc = "Use healing item when health drops below this percentage")
    @ConfigEditorSlider(minValue = 10, maxValue = 90, minStep = 5)
    public int autoHealThreshold = 40;
}
