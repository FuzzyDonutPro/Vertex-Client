package com.vertexai.macro.impl.FarmBuilderMacro.patterns;

import com.vertexai.macro.impl.FarmBuilderMacro.FarmPattern;

public class FlatFarmPattern implements FarmPattern {

    private boolean finished = false;

    @Override
    public void onStart() {
        finished = false;
    }

    @Override
    public String getRequiredToolFor(int x, int z) {
        // Flat farms: Dirt everywhere, except water every 8 blocks for hydration
        // 0 1 2 3 [4] 5 6 7 8
        if (x % 8 == 4 && z % 8 == 4) { // Simplified water grid
            return "Prismapump";
        }
        return "InfiniDirt Wand";
    }

    @Override
    public void onTick() {
        // TODO: Implement pitch/yaw walking and right-clicking logic
    }

    @Override
    public boolean isFinished() {
        return finished;
    }
}
