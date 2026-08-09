package com.vertexai.macro.impl.SlayerMacro.states;

import com.vertexai.macro.impl.combat.AutoMobKiller.AutoMobKiller;
import com.vertexai.macro.impl.SlayerMacro.SlayerMacro;
import com.vertexai.macro.impl.SlayerMacro.SlayerMacroState;

public class SlayingState implements SlayerMacroState {

    @Override
    public void onStart(SlayerMacro macro) {
    }

    @Override
    public SlayerMacroState onTick(SlayerMacro macro) {
        if (!AutoMobKiller.getInstance().isRunning()) {
            macro.disable("AutoMobKiller stopped unexpectedly.");
            return this;
        }

        switch (AutoMobKiller.getInstance().getError()) {
            case NO_ENTITIES:
                // Just log occasionally or show on HUD, macro shouldn't stop
                break;
            case NONE:
                break;
        }
        
        return this;
    }

    @Override
    public void onEnd(SlayerMacro macro) {
    }
}
