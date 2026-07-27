package com.vertexai.macro.impl.DungeonMacro.states;

import com.vertexai.macro.impl.DungeonMacro.DungeonMacro;
import com.vertexai.util.Logger;
import net.minecraft.client.Minecraft;

public class StartingState implements DungeonMacroState {
    
    private int waitTicks = 0;

    @Override
    public void onEnable(DungeonMacro macro) {
        Logger.sendMessage("Queuing for Dungeon...");
        // Auto-queue command based on config
        Minecraft.getInstance().player.connection.sendCommand("joindungeon catacombs " + com.vertexai.Vertex.config().dungeons.dungeonFloor);
    }

    @Override
    public void onTick(DungeonMacro macro) {
        waitTicks++;
        // Wait up to 10 seconds for the server to warp us in
        if (waitTicks > 200) {
            // Once loaded in, transition to clearing the entrance room
            macro.setState(new ClearRoomState());
        }
    }

    @Override
    public void onDisable(DungeonMacro macro) { }

    @Override
    public String getName() {
        return "Starting";
    }
}
