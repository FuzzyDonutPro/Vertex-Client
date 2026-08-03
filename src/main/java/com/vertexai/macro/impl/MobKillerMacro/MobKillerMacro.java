package com.vertexai.macro.impl.MobKillerMacro;

import com.vertexai.Vertex;
import com.vertexai.feature.impl.AutoMobKiller.AutoMobKiller;
import com.vertexai.macro.AbstractMacro;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * MobKillerMacro — Dedicated macro for farming general combat mobs (Zealots, Ghosts, Goblins, etc.).
 * Fully separated from Slayer Boss Quests.
 */
public class MobKillerMacro extends AbstractMacro {

    @Getter
    private static final MobKillerMacro instance = new MobKillerMacro();

    @Override
    public String getName() {
        return "Mob Killer Macro";
    }

    @Override
    public List<String> getNecessaryItems() {
        return new ArrayList<>();
    }

    @Override
    public void onEnable() {
        log("Enabling General Mob Killer Macro");
        List<String> targetList = new ArrayList<>();
        switch (Vertex.config().combat.mobKillerTarget) {
            case 0 -> targetList.add("Zealot");
            case 1 -> targetList.add("Ghost");
            case 2 -> targetList.add("Ice Walker");
            case 3 -> targetList.add("Treasure Hoarder");
            case 4 -> targetList.add("Goblin");
            case 5 -> targetList.add("Glacite Walker");
            case 6 -> targetList.add("Automoton");
            case 7 -> targetList.add("Sludge");
            case 9 -> {
                targetList.add("Zombie");
                targetList.add("Graveyard Zombie");
            }
            case 10 -> {
                targetList.add("Voracious Spider");
                targetList.add("Dasher Spider");
                targetList.add("Weaver Spider");
                targetList.add("Spider");
                targetList.add("Silverfish");
            }
            default -> {
                targetList.add("Zombie");
                targetList.add("Graveyard Zombie");
            }
        }

        String weapon = Vertex.config().general.slayerWeapon;
        String pickaxe = Vertex.config().general.miningTool;

        log("Starting Mob Killer with targets: " + String.join(", ", targetList));
        AutoMobKiller.getInstance().start(targetList, weapon, pickaxe, AutoMobKiller.SlayerProfile.GENERIC);
    }

    @Override
    public void onDisable() {
        log("Disabling Mob Killer Macro");
        try {
            AutoMobKiller.getInstance().stop();
        } catch (Exception ignored) {}
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        super.onTick();
        checkStuckAndRecover();
    }
}
