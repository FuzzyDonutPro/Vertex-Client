package com.vertexai.macro.states;

import com.vertexai.macro.FishingMacro;

public class PathfindingState implements FishingMacroState {
    @Override
    public void onStart(FishingMacro macro) {
        log("Entered PathfindingState (Stub)");
    }

    @Override
    public FishingMacroState onTick(FishingMacro macro) {
        // Dummy implementation: just stay in this state
        return this;
    }

    @Override
    public void onEnd(FishingMacro macro) {
        log("Leaving PathfindingState");
    }
}
