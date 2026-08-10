package com.vertexai.feature.impl;

import lombok.Getter;
import com.vertexai.feature.AbstractFeature;
import com.vertexai.util.Logger;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

public class DungeonSolver extends AbstractFeature {

    @Getter
    public static final DungeonSolver instance = new DungeonSolver();

    private final Map<String, String> triviaAnswers = new HashMap<>();

    public DungeonSolver() {
        this.enabled = true;
        initTriviaDatabase();
    }

    @Override
    public String getName() {
        return "DungeonSolver";
    }

    private void initTriviaDatabase() {
        // Hypixel Dungeons ORA/Trivia answer database
        triviaAnswers.put("What is the name of the Dragon in the End?", "Ender Dragon");
        triviaAnswers.put("How many total fairy souls are there?", "242");
        triviaAnswers.put("What is the base health of a Zombie?", "20");
        triviaAnswers.put("Which NPC sells the Grappling Hook schematic?", "Spider Slayer");
    }

    public void onChatMessage(String message) {
        if (!enabled) return;

        for (Map.Entry<String, String> entry : triviaAnswers.entrySet()) {
            if (message.contains(entry.getKey())) {
                Logger.sendMessage("§b[Dungeon Solver] Trivia Answer: §e" + entry.getValue());
                break;
            }
        }
    }
}
