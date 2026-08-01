package com.vertexai.feature.impl;

import lombok.Getter;
import com.vertexai.feature.AbstractFeature;

public class WorldScanner extends AbstractFeature {

    public static WorldScanner instance = new WorldScanner();
    public static WorldScanner getInstance() { return instance; }

    @Override
    public String getName() {
        return "WorldScanner";
    }

}
