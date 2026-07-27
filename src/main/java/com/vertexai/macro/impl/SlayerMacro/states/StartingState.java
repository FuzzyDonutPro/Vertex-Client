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
        if (Vertex.config().combat.slayerIceWalkers) targetList.add("Ice Walker");
        if (Vertex.config().combat.slayerTreasureHoarders) targetList.add("Treasure Hoarder");
        if (Vertex.config().combat.slayerGoblins) targetList.add("Goblin");
        if (Vertex.config().combat.slayerGlaciteWalkers) targetList.add("Glacite Walker");
        if (Vertex.config().combat.slayerAutomotons) targetList.add("Automoton");
        if (Vertex.config().combat.slayerSludge) targetList.add("Sludge");
        if (Vertex.config().combat.slayerYog) targetList.add("Yog");
        
        // Add new targets from the Combat tab
        if (Vertex.config().combat.slayerGhosts) targetList.add("Ghost");
        if (Vertex.config().combat.slayerZealots) targetList.add("Zealot");
        if (Vertex.config().combat.slayerRev) {
            targetList.add("Crypt Ghoul");
            targetList.add("Revenant Horror");
        }
        if (Vertex.config().combat.slayerSven) {
            targetList.add("Wolf");
            targetList.add("Sven Packmaster");
        }
        if (Vertex.config().combat.slayerTara) {
            targetList.add("Spider");
            targetList.add("Weaver Spider");
            targetList.add("Tarantula Broodfather");
        }
        if (Vertex.config().combat.slayerGraveyard) targetList.add("Zombie");
        
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
