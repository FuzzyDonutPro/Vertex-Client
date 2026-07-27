package com.vertexai.macro.impl.DungeonMacro;

import com.vertexai.Vertex;
import com.vertexai.macro.AbstractMacro;
import com.vertexai.macro.impl.DungeonMacro.states.*;
import net.minecraft.client.Minecraft;

public class DungeonMacro extends AbstractMacro {

    private static final DungeonMacro instance = new DungeonMacro();

    public static DungeonMacro getInstance() {
        return instance;
    }

    private DungeonMacroState currentState;

    @Override
    public void onEnable() {
        super.onEnable();
        // Start by queuing into the dungeon
        this.currentState = new StartingState();
        this.currentState.onEnable(this);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (this.currentState != null) {
            this.currentState.onDisable(this);
            this.currentState = null;
        }
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        
        if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) {
            return; // Wait for world load
        }

        if (this.currentState != null) {
            this.currentState.onTick(this);
        }
    }

    @Override
    public java.util.List<String> getNecessaryItems() {
        return java.util.Collections.emptyList();
    }

    public void setState(DungeonMacroState newState) {
        if (this.currentState != null) {
            this.currentState.onDisable(this);
        }
        this.currentState = newState;
        if (this.currentState != null) {
            this.currentState.onEnable(this);
        }
    }

    public DungeonMacroState getCurrentState() {
        return this.currentState;
    }

    @Override
    public String getName() {
        return "Dungeon AI";
    }
}
