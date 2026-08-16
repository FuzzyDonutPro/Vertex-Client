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
            case 0 -> { // Rev Horror
                targetList.add("Crypt Ghoul");
                targetList.add("Golden Ghoul");
                targetList.add("Revenant Horror");
                targetList.add("Atoned Rev");
            }
            case 1 -> { // Tara Broodfather
                targetList.add("Spider Jockey");
                targetList.add("Voracious Spider");
                targetList.add("Tarantula Beast");
                targetList.add("Tarantula Broodfather");
            }
            case 2 -> { // Sven Packmaster
                targetList.add("Pack Alpha");
                targetList.add("Howling Spirit");
                targetList.add("Sven Follower");
                targetList.add("Sven Packmaster");
            }
            case 3 -> { // Voidgloom Seraph
                targetList.add("Voidling Fanatic");
                targetList.add("Voidling Extremist");
                targetList.add("Voidgloom Seraph");
            }
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
        
        log("Ensuring Slayer quest is active via Maddox Batphone (Tier " + Vertex.config().combat.getSlayerTierRoman() + ")...");
        com.vertexai.feature.impl.SlayerQoL.SlayerQoL.getInstance().startQuestIfNeeded();

        log("Starting AutoMobKiller with targets: " + String.join(", ", targetList));
        AutoMobKiller.getInstance().start(targetList, weapon, pickaxe, AutoMobKiller.SlayerProfile.GENERIC);
        
        return new SlayingState();
    }

    @Override
    public void onEnd(SlayerMacro macro) {
    }
}
