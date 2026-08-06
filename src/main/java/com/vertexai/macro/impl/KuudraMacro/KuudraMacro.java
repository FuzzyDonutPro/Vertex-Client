package com.vertexai.macro.impl.KuudraMacro;

import com.vertexai.macro.AbstractMacro;
import com.vertexai.macro.impl.KuudraMacro.states.*;
import com.vertexai.macro.states.MacroState;
import com.vertexai.util.Logger;
import lombok.Getter;

import java.util.List;

/**
 * KuudraMacro — Complete T1-T5 Kuudra Boss Suite.
 * Automates Supply Crate Rush, Cannon Building & Fueling, Pod Head Stun, and Core DPS.
 */
public class KuudraMacro extends AbstractMacro {

    private static final KuudraMacro instance = new KuudraMacro();

    public static KuudraMacro getInstance() {
        return instance;
    }

    public final SupplyPhaseState supplyState = new SupplyPhaseState();
    public final BuildPhaseState buildState = new BuildPhaseState();
    public final StunPhaseState stunState = new StunPhaseState();
    public final DpsPhaseState dpsState = new DpsPhaseState();

    @Override
    public String getName() {
        return "Kuudra Macro";
    }

    @Override
    public List<String> getNecessaryItems() {
        return List.of("Aspect of the Void", "Hyperion", "Terminator", "Auto-Pet");
    }

    @Override
    public void onEnable() {
        log("Starting Kuudra Boss Suite Automation");
        getStateMachine().transitionTo(supplyState);
    }

    @Override
    public void onDisable() {
        log("Stopping Kuudra Boss Suite Automation");
        getStateMachine().transitionTo(null);
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        getStateMachine().onTick();
    }
}
