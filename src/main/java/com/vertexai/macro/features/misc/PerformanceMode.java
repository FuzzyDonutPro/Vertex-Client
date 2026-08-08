package com.vertexai.macro.features.misc;

import lombok.Getter;
import com.vertexai.macro.AbstractFeature;

public class PerformanceMode extends AbstractFeature {

    @Getter
    public static final PerformanceMode instance = new PerformanceMode();

    @Override
    public String getName() {
        return "PerformanceMode";
    }

    @Override
    protected void onTick() {
        if (mc.player == null) return;
    }
}
