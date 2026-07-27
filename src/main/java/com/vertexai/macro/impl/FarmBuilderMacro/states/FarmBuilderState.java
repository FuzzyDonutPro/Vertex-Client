package com.vertexai.macro.impl.FarmBuilderMacro.states;

import com.vertexai.macro.AbstractMacro;
import com.vertexai.macro.states.MacroState;
import net.minecraft.client.Minecraft;

public abstract class FarmBuilderState implements MacroState {

    protected final AbstractMacro macro;
    protected static final Minecraft mc = Minecraft.getInstance();

    public FarmBuilderState(AbstractMacro macro) {
        this.macro = macro;
    }

    @Override
    public void onEnter(AbstractMacro macro) {
        onEnable();
    }

    @Override
    public void onTick(AbstractMacro macro) {
        onTick();
    }

    @Override
    public void onExit(AbstractMacro macro) {
        onDisable();
    }
    
    // Legacy support for the methods I used
    public void onEnable() {}
    public void onDisable() {}
    public abstract void onTick();
}
