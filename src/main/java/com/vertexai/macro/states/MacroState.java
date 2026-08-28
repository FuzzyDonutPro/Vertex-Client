package com.vertexai.macro.states;

import com.vertexai.macro.AbstractMacro;

public interface MacroState {
    
    /**
     * Called when the state is first entered.
     */
    void onEnter(AbstractMacro macro);

    /**
     * Called every client tick while the state is active.
     */
    void onTick(AbstractMacro macro);

    /**
     * Called when the state is exiting.
     */
    void onExit(AbstractMacro macro);

    /**
     * Return the name of the state.
     */
    String getName();

    default void onWorldRender(AbstractMacro macro, com.vertexai.util.WorldRenderContextWrapper context) {}
    
    default void onOverlayRender(AbstractMacro macro, net.minecraft.client.gui.GuiGraphicsExtractor graphics) {}
}
