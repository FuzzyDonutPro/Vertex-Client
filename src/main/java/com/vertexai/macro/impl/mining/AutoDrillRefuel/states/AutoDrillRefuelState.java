package com.vertexai.macro.impl.mining.AutoDrillRefuel.states;

import com.vertexai.macro.impl.mining.AutoDrillRefuel.AutoDrillRefuel;


public interface AutoDrillRefuelState {

    void onStart(AutoDrillRefuel refueler);

    AutoDrillRefuelState onTick(AutoDrillRefuel refueler);

    void onEnd(AutoDrillRefuel refueler);

    default void log(String message) {
        System.out.println("[" + this.getClass().getSimpleName() + "] " + message);
    }

    default void logError(String message) {
        System.out.println("[" + this.getClass().getSimpleName() + "] ERROR: " + message);
    }
}
