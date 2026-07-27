package com.vertexai.dungeons.puzzles;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * Interface for all Dungeon Puzzle solvers (Tic-Tac-Toe, Water Board, etc.)
 */
public interface PuzzleSolver {
    
    /**
     * Checks if the puzzle has been successfully solved.
     */
    boolean isSolved();
    
    /**
     * Called every tick when the player is inside the puzzle room.
     * Contains the core solving logic (scanning, pathfinding, clicking).
     */
    void onTick(Minecraft mc);
    
    /**
     * Returns the name of the puzzle.
     */
    String getName();
}
