package com.vertexai.macro.impl.KuudraMacro.states;

import com.vertexai.macro.AbstractMacro;
import com.vertexai.macro.states.MacroState;

/**
 * Phase 4: Core DPS Phase
 * Aims at Kuudra core and fires Hyperion / Terminator weapons continuously until Kuudra is defeated.
 */
public class DpsPhaseState implements MacroState {

    @Override
    public String getName() {
        return "Kuudra - Core DPS Phase";
    }

    @Override
    public void onEnter(AbstractMacro macro) {
    }

    @Override
    public void onTick(AbstractMacro macro) {
        // Auto-aims at Kuudra heart/core entity and triggers weapon attacks
    }

    @Override
    public void onExit(AbstractMacro macro) {
    }
}
