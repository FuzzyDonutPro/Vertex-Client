package com.vertexai.feature.impl;

import lombok.Getter;
import com.vertexai.feature.AbstractFeature;

public class WorldScanner extends AbstractFeature {

    @Getter
    public static WorldScanner instance = new WorldScanner();

    @Override
    public String getName() {
        return "WorldScanner";
    }

}
