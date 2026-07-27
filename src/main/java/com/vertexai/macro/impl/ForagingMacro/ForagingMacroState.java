package com.vertexai.macro.impl.ForagingMacro;

import com.vertexai.util.Logger;

public interface ForagingMacroState {
    void onStart(ForagingMacro macro);
    ForagingMacroState onTick(ForagingMacro macro);
    void onEnd(ForagingMacro macro);

    default void log(String message) {
        Logger.sendLog(message);
    }
}
