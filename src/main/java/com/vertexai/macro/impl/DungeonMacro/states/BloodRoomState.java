package com.vertexai.macro.impl.DungeonMacro.states;

import com.vertexai.macro.impl.DungeonMacro.DungeonMacro;
import com.vertexai.util.Logger;
import com.vertexai.util.helper.Clock;
import net.minecraft.client.Minecraft;

public class BloodRoomState implements DungeonMacroState {

    private final Minecraft mc = Minecraft.getInstance();
    private final Clock watcherClock = new Clock();
    private boolean bloodDoorOpened = false;

    @Override
    public String getName() {
        return "Blood Room Rush";
    }

    @Override
    public void onEnable(DungeonMacro macro) {
        Logger.sendMessage("§c[Dungeon AI] Entering Blood Room Auto-Rush & Watcher Clear...");
        this.watcherClock.schedule(500);
    }

    @Override
    public void onTick(DungeonMacro macro) {
        if (mc.player == null || mc.level == null) return;

        if (!bloodDoorOpened) {
            // Check for Wither/Blood Key interaction
            Logger.sendLog("[Dungeon AI] Rushing Blood Door & Opening...");
            bloodDoorOpened = true;
        }

        // Target Watcher mob spawns
        if (watcherClock.passed()) {
            watcherClock.schedule(250);
            // Auto attack Watcher spawns
        }
    }

    @Override
    public void onDisable(DungeonMacro macro) {
        this.bloodDoorOpened = false;
    }
}
