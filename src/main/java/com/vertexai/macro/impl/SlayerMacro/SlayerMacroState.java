package com.vertexai.macro.impl.SlayerMacro;

import com.vertexai.util.Logger;

public interface SlayerMacroState {
    void onStart(SlayerMacro macro);
    SlayerMacroState onTick(SlayerMacro macro);
    void onEnd(SlayerMacro macro);
    
    default void log(String message) {
        Logger.sendLog("[" + this.getClass().getSimpleName() + "] " + message);
    }
    
    default void logError(String message) {
        Logger.sendLog("[" + this.getClass().getSimpleName() + "] ERROR: " + message);
    }
}
