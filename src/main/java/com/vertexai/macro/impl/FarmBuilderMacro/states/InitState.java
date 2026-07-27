package com.vertexai.macro.impl.FarmBuilderMacro.states;

import com.vertexai.Vertex;
import com.vertexai.macro.AbstractMacro;
import com.vertexai.macro.impl.FarmBuilderMacro.BuilderToolUtil;
import com.vertexai.util.PlayerUtil;
import com.vertexai.util.Logger;
import com.vertexai.util.helper.Clock;

public class InitState extends FarmBuilderState {

    private final Clock delayClock = new Clock();
    private boolean checkedTools = false;

    public InitState(AbstractMacro macro) {
        super(macro);
    }

    @Override
    public String getName() {
        return "Initializing FarmBuilder";
    }

    @Override
    public void onEnable() {
        checkedTools = false;
        delayClock.reset();
    }

    @Override
    public void onTick() {
        if (!checkedTools) {
            if (!delayClock.passed()) return;

            // Check for required tools based on pattern (simplified for now to just check InfiniDirt Wand)
            if (!BuilderToolUtil.hasTool("InfiniDirt Wand")) {
                Logger.sendMessage("§c[FarmBuilder] Missing required tool: InfiniDirt Wand! Stopping macro.");
                macro.toggle();
                return;
            }

            Logger.sendMessage("§a[FarmBuilder] Required tools confirmed. Starting strafe build...");
            checkedTools = true;
            macro.getStateMachine().transitionTo(new StrafeBuildState(macro));
        }
    }
}
