package com.vertexai.macro.impl.DungeonMacro.states;

import com.vertexai.macro.impl.DungeonMacro.DungeonMacro;

public class ExploreState implements DungeonMacroState {

    @Override
    public void onEnable(DungeonMacro macro) { }

    @Override
    public void onTick(DungeonMacro macro) {
        // Placeholder for exploration
        // 1. Scan for unopened Wither/Blood/Normal doors
        // 2. Pathfind through the door
        // 3. Once entered a new room, transition back to ClearRoomState
    }

    @Override
    public void onDisable(DungeonMacro macro) { }

    @Override
    public String getName() {
        return "Exploring";
    }
}
