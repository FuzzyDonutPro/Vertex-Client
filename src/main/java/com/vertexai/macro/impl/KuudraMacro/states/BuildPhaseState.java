package com.vertexai.macro.impl.KuudraMacro.states;

import com.vertexai.macro.AbstractMacro;
import com.vertexai.macro.states.MacroState;

/**
 * Phase 2: Cannon & Ballista Build Phase
 * Auto-delivers cannonballs and fuel to Ballista cannon until build progress reaches 100%.
 */
public class BuildPhaseState implements MacroState {

    @Override
    public String getName() {
        return "Kuudra - Build Phase";
    }

    @Override
    public void onEnter(AbstractMacro macro) {
    }

    @Override
    public void onTick(AbstractMacro macro) {
        // Pathfinds to fuel piles and loads Ballista cannon
    }

    @Override
    public void onExit(AbstractMacro macro) {
    }
}
