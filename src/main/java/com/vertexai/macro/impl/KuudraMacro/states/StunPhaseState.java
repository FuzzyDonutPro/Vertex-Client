package com.vertexai.macro.impl.KuudraMacro.states;

import com.vertexai.macro.AbstractMacro;
import com.vertexai.macro.states.MacroState;

/**
 * Phase 3: Pod Head Stun Phase
 * Etherwarps onto Kuudra's pod head when Kuudra emerges from lava and hits stun spot.
 */
public class StunPhaseState implements MacroState {

    @Override
    public String getName() {
        return "Kuudra - Pod Head Stun Phase";
    }

    @Override
    public void onEnter(AbstractMacro macro) {
    }

    @Override
    public void onTick(AbstractMacro macro) {
        // Scans for Kuudra Pod entity
        // Uses EtherwarpHelper to teleport directly onto Kuudra's pod head for instant stun
    }

    @Override
    public void onExit(AbstractMacro macro) {
    }
}
