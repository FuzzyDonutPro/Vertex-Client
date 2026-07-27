package com.vertexai.macro.impl.FarmingMacro;

import com.vertexai.util.Logger;

public interface FarmingMacroState {
    void onStart(FarmingMacro macro);
    FarmingMacroState onTick(FarmingMacro macro);
    void onEnd(FarmingMacro macro);

    default void log(String message) {
        Logger.sendLog("[Farming] " + message);
    }
}
