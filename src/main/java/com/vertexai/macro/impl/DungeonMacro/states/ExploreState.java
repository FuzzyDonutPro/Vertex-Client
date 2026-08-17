package com.vertexai.macro.impl.DungeonMacro.states;

import com.vertexai.dungeons.DungeonDoorNavigator;
import com.vertexai.dungeons.RoomTracker;
import com.vertexai.macro.impl.DungeonMacro.DungeonMacro;
import com.vertexai.util.Logger;
import net.minecraft.client.Minecraft;

public class ExploreState implements DungeonMacroState {

    private final Minecraft mc = Minecraft.getInstance();
    private RoomTracker.DungeonRoom initialRoom = null;

    @Override
    public void onEnable(DungeonMacro macro) {
        Logger.sendMessage("§9[Dungeon AI] Entering Room Exploration & Door Navigation...");
        DungeonDoorNavigator.getInstance().reset();
        this.initialRoom = RoomTracker.getInstance().getCurrentRoom();
    }

    @Override
    public void onTick(DungeonMacro macro) {
        if (mc.player == null || mc.level == null) return;

        RoomTracker.getInstance().update(mc);
        RoomTracker.DungeonRoom currentRoom = RoomTracker.getInstance().getCurrentRoom();

        // If we entered a new room that is not cleared, immediately transition to clearing it
        if (currentRoom != null && currentRoom != this.initialRoom && !currentRoom.isCleared) {
            Logger.sendMessage("§a[Dungeon AI] Entered new room, starting room clear...");
            DungeonDoorNavigator.getInstance().reset();
            macro.setState(new ClearRoomState());
            return;
        }

        // Execute door navigation, key collection, and door unlocking
        DungeonDoorNavigator.getInstance().onTick();
    }

    @Override
    public void onDisable(DungeonMacro macro) {
        DungeonDoorNavigator.getInstance().reset();
    }

    @Override
    public String getName() {
        return "Door Exploration";
    }
}
