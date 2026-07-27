package com.vertexai.macro.impl.DungeonMacro.states;

import com.vertexai.macro.impl.DungeonMacro.DungeonMacro;
import com.vertexai.dungeons.RoomTracker;
import com.vertexai.dungeons.puzzles.PuzzleManager;
import net.minecraft.client.Minecraft;

public class PuzzleState implements DungeonMacroState {

    @Override
    public void onEnable(DungeonMacro macro) { }

    @Override
    public void onTick(DungeonMacro macro) {
        RoomTracker.DungeonRoom room = RoomTracker.getInstance().getCurrentRoom();
        if (room == null) return;
        
        PuzzleManager.getInstance().update(Minecraft.getInstance(), room);
        
        if (room.isCleared) {
            // Puzzle is solved (PuzzleManager sets isCleared = true)
            if (com.vertexai.Vertex.config().dungeons.autoSecretFinder) {
                macro.setState(new SecretFinderState());
            } else {
                macro.setState(new ExploreState());
            }
        }
    }

    @Override
    public void onDisable(DungeonMacro macro) { }

    @Override
    public String getName() {
        return "Solving Puzzle";
    }
}
