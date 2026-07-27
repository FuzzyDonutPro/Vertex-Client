package com.vertexai.macro.impl.CommissionMacro.states;

import com.vertexai.macro.impl.CommissionMacro.CommissionMacro;
import com.vertexai.util.Logger;

public interface CommissionMacroState {

    void onStart(CommissionMacro macro);

    CommissionMacroState onTick(CommissionMacro macro);

    void onEnd(CommissionMacro macro);

    default void log(String message) {
        Logger.sendLog("[" + this.getClass().getSimpleName() + "] " + message);
    }

    default void logError(String message) {
        Logger.sendLog("[" + this.getClass().getSimpleName() + "] ERROR: " + message);
    }

    default void send(String message) {
        Logger.addMessage("[" + this.getClass().getSimpleName() + "] " + message);
    }

}
