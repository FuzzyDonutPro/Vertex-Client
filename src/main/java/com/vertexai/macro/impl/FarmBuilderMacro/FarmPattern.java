package com.vertexai.macro.impl.FarmBuilderMacro;

import net.minecraft.core.BlockPos;

public interface FarmPattern {
    /**
     * Called when the pattern begins building.
     */
    void onStart();
    
    /**
     * Determines what tool/block should be placed at the specific plot-relative offset.
     * @param x Relative X (0 to 95)
     * @param z Relative Z (0 to 95)
     * @return The internal name of the tool or block needed (e.g., "InfiniDirt Wand", "Prismapump")
     */
    String getRequiredToolFor(int x, int z);
    
    /**
     * Executes the placement/build logic for the current state.
     */
    void onTick();
    
    /**
     * Returns true if the farm pattern is fully built.
     */
    boolean isFinished();
}
