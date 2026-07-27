package com.vertexai.macro.impl.NukerMacro;

import com.vertexai.util.Logger;

public interface NukerMacroState {
    void onStart(NukerMacro macro);
    NukerMacroState onTick(NukerMacro macro);
    void onEnd(NukerMacro macro);
    
    default void log(String message) {
        Logger.sendLog("[" + this.getClass().getSimpleName() + "] " + message);
    }
    
    default void logError(String message) {
        Logger.sendLog("[" + this.getClass().getSimpleName() + "] ERROR: " + message);
    }
}
