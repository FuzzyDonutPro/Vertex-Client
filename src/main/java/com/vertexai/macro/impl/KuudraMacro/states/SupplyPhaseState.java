package com.vertexai.macro.impl.KuudraMacro.states;

import com.vertexai.macro.AbstractMacro;
import com.vertexai.macro.states.MacroState;

/**
 * Phase 1: Supply Rush
 * Auto-locates supply crates, pathfinds / etherwarps to pick them up, and delivers them to the center pod.
 */
public class SupplyPhaseState implements MacroState {

    @Override
    public String getName() {
        return "Kuudra - Supply Rush Phase";
    }

    @Override
    public void onEnter(AbstractMacro macro) {
    }

    @Override
    public void onTick(AbstractMacro macro) {
        // Scans entity list for Kuudra Crates ("Crate", "Supply")
        // Etherwarps / pathfinds to supply crate position
        // Delivers crate back to center pod (0, 75, 0)
    }

    @Override
    public void onExit(AbstractMacro macro) {
    }
}
