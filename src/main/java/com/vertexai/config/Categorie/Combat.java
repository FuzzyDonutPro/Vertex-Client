package com.vertexai.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;

public class Combat {

    @ConfigOption(
            name = "Slayer Boss Target",
            desc = "Select the Slayer boss quest to farm (Auto-summons boss & auto-claims Maddox quest)"
    )
    @ConfigEditorDropdown(values = {
            "Revenant Horror (Crypt Ghouls)",
            "Tarantula Broodfather (Spiders)",
            "Sven Packmaster (Wolves)",
            "Voidgloom Seraph (Endermen)"
    })
    public int slayerTarget = 0; // Default: Revenant Horror

    @ConfigOption(
            name = "Mob Killer Target",
            desc = "Select the general area mob to farm for combat EXP, drops, and bestiary"
    )
    @ConfigEditorDropdown(values = {
            "Zealots",
            "Ghosts",
            "Ice Walkers",
            "Treasure Hoarders",
            "Goblins",
            "Glacite Walkers",
            "Automotons",
            "Sludge",
            "Yog",
            "Graveyard Zombies",
            "Spider's Den Spiders & Silverfish",
            "Endermen"
    })
    public int mobKillerTarget = 9; // Default: Graveyard Zombies

    @ConfigOption(name = "Auto-Heal", desc = "Automatically swap to a healing item and use it when health is low")
    @ConfigEditorBoolean
    public boolean autoHealEnabled = true;

    @ConfigOption(name = "Auto Rogue Sword", desc = "Automatically swap to Rogue Sword and right-click when mana is 50+ for speed boost")
    @ConfigEditorBoolean
    public boolean autoRogueSword = false;

    @ConfigOption(name = "Healing Item", desc = "Name of the healing item in your hotbar (e.g., Florid Zombie Sword)")
    @ConfigEditorText
    public String healingItem = "Florid Zombie Sword";

    @ConfigOption(name = "Auto-Heal Threshold (%)", desc = "Use healing item when health drops below this percentage")
    @ConfigEditorSlider(minValue = 10, maxValue = 90, minStep = 5)
    public int autoHealThreshold = 40;

    // Backwards compatibility helpers
    public boolean isSlayerRev() { return slayerTarget == 1; }
    public boolean isSlayerTara() { return slayerTarget == 2; }
    public boolean isSlayerSven() { return slayerTarget == 3; }
    public boolean isSlayerZealots() { return slayerTarget == 4; }
    public boolean isSlayerGhosts() { return slayerTarget == 5; }
    public boolean isSlayerIceWalkers() { return slayerTarget == 6; }
    public boolean isSlayerTreasureHoarders() { return slayerTarget == 7; }
    public boolean isSlayerGoblins() { return slayerTarget == 8; }
    public boolean isSlayerGlaciteWalkers() { return slayerTarget == 9; }
    public boolean isSlayerAutomotons() { return slayerTarget == 10; }
    public boolean isSlayerSludge() { return slayerTarget == 11; }
    public boolean isSlayerYog() { return slayerTarget == 12; }
    public boolean isSlayerGraveyard() { return slayerTarget == 13; }
}
