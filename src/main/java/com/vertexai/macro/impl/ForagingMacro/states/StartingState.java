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
        if (Vertex.config().foraging.foragingFig) {
            macro.setCurrentForagingMode("Fig");
        } else if (Vertex.config().foraging.foragingLushlilac) {
            macro.setCurrentForagingMode("Lushlilac");
        } else if (Vertex.config().foraging.foragingMangrove) {
            macro.setCurrentForagingMode("Mangrove");
        } else if (Vertex.config().foraging.foragingPark) {
            macro.setCurrentForagingMode("Park");
        } else if (Vertex.config().foraging.foragingHub) {
            macro.setCurrentForagingMode("Hub");
        } else {
            macro.disable("Please enable at least one Foraging Mode in the config!");
            return this;
        }

        log("Starting Foraging Macro in mode: " + macro.getCurrentForagingMode());
        return new PathfindingState();
    }

    @Override
    public void onEnd(ForagingMacro macro) {
    }
}
