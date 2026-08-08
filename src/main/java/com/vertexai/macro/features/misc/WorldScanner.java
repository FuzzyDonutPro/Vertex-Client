package com.vertexai.macro.features.misc;

import lombok.Getter;
import com.vertexai.macro.AbstractFeature;

public class WorldScanner extends AbstractFeature {

    public static WorldScanner instance = new WorldScanner();
    public static WorldScanner getInstance() { return instance; }

    @Override
    public String getName() {
        return "WorldScanner";
    }

}
