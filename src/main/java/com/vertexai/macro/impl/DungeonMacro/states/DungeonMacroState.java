package com.vertexai.macro.impl.DungeonMacro.states;

import com.vertexai.macro.impl.DungeonMacro.DungeonMacro;

public interface DungeonMacroState {
    void onTick(DungeonMacro macro);
    void onEnable(DungeonMacro macro);
    void onDisable(DungeonMacro macro);
    String getName();
}
