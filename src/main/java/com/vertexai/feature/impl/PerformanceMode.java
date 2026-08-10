package com.vertexai.feature.impl;

import lombok.Getter;
import com.vertexai.feature.AbstractFeature;

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
