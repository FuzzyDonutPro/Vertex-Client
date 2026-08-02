package com.vertexai.macro.impl.SlayerMacro.states;

import com.vertexai.Vertex;
import com.vertexai.macro.impl.SlayerMacro.SlayerMacro;
import com.vertexai.macro.impl.SlayerMacro.SlayerMacroState;
import com.vertexai.feature.impl.AutoMobKiller.AutoMobKiller;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StartingState implements SlayerMacroState {

    @Override
    public void onStart(SlayerMacro macro) {
        log("Parsing slayer targets from config...");
    }

    @Override
    public SlayerMacroState onTick(SlayerMacro macro) {
        List<String> targetList = new java.util.ArrayList<>();
        switch (Vertex.config().combat.slayerTarget) {
            case 1 -> { // Rev
                targetList.add("Crypt Ghoul");
                targetList.add("Revenant Horror");
            }
            case 2 -> { // Tara
                targetList.add("Spider");
                targetList.add("Weaver Spider");
                targetList.add("Tarantula Broodfather");
            }
            case 3 -> { // Sven
                targetList.add("Wolf");
                targetList.add("Sven Packmaster");
            }
            case 4 -> targetList.add("Zealot");
            case 5 -> targetList.add("Ghost");
            case 6 -> targetList.add("Ice Walker");
            case 7 -> targetList.add("Treasure Hoarder");
            case 8 -> targetList.add("Goblin");
            case 9 -> targetList.add("Glacite Walker");
            case 10 -> targetList.add("Automoton");
            case 11 -> targetList.add("Sludge");
            case 12 -> targetList.add("Yog");
            case 13 -> targetList.add("Zombie");
        }
        
        if (targetList.isEmpty()) {
            macro.disable("Please enable at least one Slayer Target in the config!");
            return this;
        }
        
        String weapon = Vertex.config().general.slayerWeapon;
        String pickaxe = Vertex.config().general.miningTool;
        
        if ((weapon == null || weapon.trim().isEmpty()) && (pickaxe == null || pickaxe.trim().isEmpty())) {
            macro.disable("Please set a Slayer Weapon or Mining Tool in the config!");
            return this;
        }
        
        log("Starting AutoMobKiller with targets: " + String.join(", ", targetList));
        AutoMobKiller.getInstance().start(targetList, weapon, pickaxe, AutoMobKiller.SlayerProfile.GENERIC);
        
        return new SlayingState();
    }

    @Override
    public void onEnd(SlayerMacro macro) {
    }
}
