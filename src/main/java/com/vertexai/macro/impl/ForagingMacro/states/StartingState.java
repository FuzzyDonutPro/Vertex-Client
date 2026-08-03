package com.vertexai.macro.impl.ForagingMacro.states;

import com.vertexai.Vertex;
import com.vertexai.macro.impl.ForagingMacro.ForagingMacro;
import com.vertexai.macro.impl.ForagingMacro.ForagingMacroState;

public class StartingState implements ForagingMacroState {

    @Override
    public void onStart(ForagingMacro macro) {
        log("Checking foraging mode...");
    }

    @Override
    public ForagingMacroState onTick(ForagingMacro macro) {
        int treeTypeIdx = Vertex.config().foraging.foragingTreeType;
        String mode = "Dark Oak";
        switch (treeTypeIdx) {
            case 1: mode = "Acacia"; break;
            case 2: mode = "Jungle"; break;
            case 3: mode = "Spruce"; break;
            case 4: mode = "Oak"; break;
            case 5: mode = "Birch"; break;
            default: mode = "Dark Oak"; break;
        }
        macro.setCurrentForagingMode(mode);

        log("Starting Foraging Macro for wood type: " + macro.getCurrentForagingMode());
        return new PathfindingState();
    }

    @Override
    public void onEnd(ForagingMacro macro) {
    }
}
