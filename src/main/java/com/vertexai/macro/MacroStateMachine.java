package com.vertexai.macro;

import com.vertexai.macro.states.MacroState;

public class MacroStateMachine {

    private final AbstractMacro macro;
    private MacroState currentState;
    private MacroState previousState;

    public MacroStateMachine(AbstractMacro macro) {
        this.macro = macro;
    }

    public void transitionTo(MacroState newState) {
        if (this.currentState != null) {
            this.currentState.onExit(macro);
        }
        
        this.previousState = this.currentState;
        this.currentState = newState;
        
        if (this.currentState != null) {
            this.macro.log("Transitioned to state: " + this.currentState.getName());
            this.currentState.onEnter(macro);
        }
    }

    public void onTick() {
        if (this.currentState != null) {
            this.currentState.onTick(macro);
        }
    }

    public void onWorldRender(com.vertexai.util.WorldRenderContextWrapper context) {
        if (this.currentState != null) {
            this.currentState.onWorldRender(macro, context);
        }
    }

    public void onOverlayRender(net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
        if (this.currentState != null) {
            this.currentState.onOverlayRender(macro, graphics);
        }
    }

    public MacroState getCurrentState() {
        return currentState;
    }

    public MacroState getPreviousState() {
        return previousState;
    }
}
