package com.vertexai.failsafe.impl;

import lombok.Getter;
import com.vertexai.failsafe.AbstractFailsafe;
import com.vertexai.feature.impl.AutoWarp;
import com.vertexai.macro.MacroManager;

public class WorldChangeFailsafe extends AbstractFailsafe {

    public static final WorldChangeFailsafe instance = new WorldChangeFailsafe();
    public static WorldChangeFailsafe getInstance() { return instance; }
    private static final Failsafe failsafeType = Failsafe.TELEPORT;

    @Override
    public String getName() {
        return "WorldChangeFailsafe";
    }

    @Override
    public Failsafe getFailsafeType() {
        return failsafeType;
    }

    @Override
    public int getPriority() {
        return 5;
    }

    @Override
    public boolean react() {
        warn("Stopping macro due to world change.");
        MacroManager.getInstance().disable();
        return true;
    }

    @Override
    public boolean onWorldUnload() {
        if (!MacroManager.getInstance().isEnabled()) return false;
        return AutoWarp.getInstance() != null && AutoWarp.getInstance().isDoneWarping();
    }

}
