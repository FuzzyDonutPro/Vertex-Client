package com.vertexai.macro.impl.FishingMacro.states;

import com.vertexai.macro.impl.FishingMacro.FishingMacro;
import com.vertexai.util.Logger;

public interface FishingMacroState {

    void onStart(FishingMacro macro);

    FishingMacroState onTick(FishingMacro macro);

    void onEnd(FishingMacro macro);

    default void log(String message) {
        Logger.sendLog("[" + this.getClass().getSimpleName() + "] " + message);
    }

    default void send(String message) {
        Logger.sendMessage("[" + this.getClass().getSimpleName() + "] " + message);
    }
}
