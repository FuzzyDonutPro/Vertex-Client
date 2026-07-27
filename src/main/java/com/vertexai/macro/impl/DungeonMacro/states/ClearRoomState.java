package com.vertexai.macro.impl.DungeonMacro.states;

import com.vertexai.macro.impl.DungeonMacro.DungeonMacro;
import com.vertexai.dungeons.RoomTracker;

public class ClearRoomState implements DungeonMacroState {

    @Override
    public void onEnable(DungeonMacro macro) {
        // Init mob scanner
    }

    @Override
    public void onTick(DungeonMacro macro) {
        RoomTracker.getInstance().update(net.minecraft.client.Minecraft.getInstance());
        RoomTracker.DungeonRoom room = RoomTracker.getInstance().getCurrentRoom();
        if (room == null) return;

        // Transition to AutoClearState if not cleared
        if (!room.isCleared) {
            macro.setState(new AutoClearState());
        } else {
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
        return "Clearing Room";
    }
}
