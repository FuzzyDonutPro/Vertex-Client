package com.vertexai.feature.impl;

import com.vertexai.Vertex;
import com.vertexai.bypass.SprintBypass;
import com.vertexai.feature.AbstractFeature;

public class AutoSprint extends AbstractFeature {

    public static final AutoSprint instance = new AutoSprint();

    public static AutoSprint getInstance() {
        return instance;
    }

    @Override
    public String getName() {
        return "Auto Sprint";
    }

    @Override
    public boolean shouldStartAtLaunch() {
        return true;
    }

    @Override
    public boolean isRunning() {
        return mc.player != null && Vertex.config() != null && Vertex.config().utilities != null && Vertex.config().utilities.sprint;
    }

    @Override
    protected void onTick() {
        if (!isRunning() || mc.screen != null) return;

        // Ensure player is moving forward, has enough food/abilities, and passes Grim checks
        if (mc.options != null && mc.options.keyUp.isDown()) {
            if (SprintBypass.canSprint()) {
                mc.options.keySprint.setDown(true);
                if (mc.player != null) {
                    mc.player.setSprinting(true);
                }
            }
        }
    }
}
