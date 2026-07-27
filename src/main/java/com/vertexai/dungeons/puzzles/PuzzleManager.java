package com.vertexai.dungeons.puzzles;

import com.vertexai.dungeons.RoomTracker;
import net.minecraft.client.Minecraft;

/**
 * Detects if the current room is a puzzle room and delegates to the appropriate solver.
 */
public class PuzzleManager {

    private static final PuzzleManager instance = new PuzzleManager();
    private PuzzleSolver activeSolver = null;

    public static PuzzleManager getInstance() {
        return instance;
    }

    public void update(Minecraft mc, RoomTracker.DungeonRoom currentRoom) {
        if (currentRoom == null) return;
        
        // If we don't have an active solver, try to detect the puzzle based on room signatures
        if (activeSolver == null) {
            detectPuzzle(mc, currentRoom);
        }

        // If we found a puzzle, tick the solver
        if (activeSolver != null) {
            activeSolver.onTick(mc);
            
            if (activeSolver.isSolved()) {
                activeSolver = null;
                currentRoom.isCleared = true; // Mark room as cleared so we can move on
            }
        }
    }

    private void detectPuzzle(Minecraft mc, RoomTracker.DungeonRoom room) {
        // Placeholder for structural detection (e.g., scanning for Item Frames in a 3x3 grid)
        
        if (isTicTacToeRoom(mc, room)) {
            activeSolver = new TicTacToeSolver();
        } else if (isWaterBoardRoom(mc, room)) {
            activeSolver = new WaterBoardSolver();
        } else if (isCreeperBeamRoom(mc, room)) {
            activeSolver = new CreeperBeamSolver();
        } else if (isBlazeRoom(mc, room)) {
            activeSolver = new BlazeRoomSolver();
        }
    }

    private boolean isTicTacToeRoom(Minecraft mc, RoomTracker.DungeonRoom room) {
        return false; // Disabled until fully structurally mapped
    }

    private boolean isWaterBoardRoom(Minecraft mc, RoomTracker.DungeonRoom room) {
        return false;
    }

    private boolean isCreeperBeamRoom(Minecraft mc, RoomTracker.DungeonRoom room) {
        return false;
    }

    private boolean isBlazeRoom(Minecraft mc, RoomTracker.DungeonRoom room) {
        return false;
    }
}
