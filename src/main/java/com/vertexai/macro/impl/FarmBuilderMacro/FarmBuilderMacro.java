package com.vertexai.macro.impl.FarmBuilderMacro;

import com.vertexai.Vertex;
import com.vertexai.macro.AbstractMacro;
import com.vertexai.macro.impl.FarmBuilderMacro.states.InitState;

public class FarmBuilderMacro extends AbstractMacro {

    public static final FarmBuilderMacro instance = new FarmBuilderMacro();
    public static FarmBuilderMacro getInstance() { return instance; }

    public FarmBuilderMacro() {}
    
    @Override
    public String getName() {
        return "FarmBuilder";
    }

    @Override
    public java.util.List<String> getNecessaryItems() {
        return java.util.Collections.emptyList();
    }

    @Override
    public boolean isEnabled() {
        return Vertex.config().farmBuilder.enabled;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        getStateMachine().transitionTo(new com.vertexai.macro.impl.FarmBuilderMacro.states.PreviewState(this));
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        
        getStateMachine().onTick();
    }

    @Override
    public void onWorldRender(com.vertexai.util.WorldRenderContextWrapper context) {
        if (!isEnabled()) return;
        getStateMachine().onWorldRender(context);
    }

    @Override
    public void onOverlayRender(net.minecraft.client.gui.GuiGraphics graphics) {
        if (!isEnabled()) return;
        getStateMachine().onOverlayRender(graphics);
    }
}
