package com.vertexai.client;

import net.fabricmc.api.ClientModInitializer;

public class VertexAIModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Client-side initialization
        com.vertexai.ai.ChatAIHandler.init();
        com.vertexai.pathing.PathRenderer.init();
        com.vertexai.gui.KeybindManager.init();
        com.vertexai.macro.FishingMacro.init();
    }
}
