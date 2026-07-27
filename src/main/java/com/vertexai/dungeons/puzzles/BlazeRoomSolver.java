package com.vertexai.dungeons.puzzles;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Blaze;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Solves the Blaze Room puzzle by sorting Blazes by health and shooting them with a bow.
 */
public class BlazeRoomSolver implements PuzzleSolver {

    private boolean solved = false;
    private int shootDelay = 0;
    private List<Blaze> sortedBlazes = new ArrayList<>();
    private boolean ascending = true; // Determines if we shoot lowest or highest health first

    @Override
    public boolean isSolved() {
        return solved;
    }

    @Override
    public void onTick(Minecraft mc) {
        if (solved) return;
        
        if (shootDelay > 0) {
            shootDelay--;
            return;
        }

        if (sortedBlazes.isEmpty()) {
            scanAndSortBlazes(mc);
            if (sortedBlazes.isEmpty()) {
                solved = true; // No blazes found, room is already clear
                return;
            }
        }

        // Find the next alive blaze
        Blaze target = null;
        for (Blaze b : sortedBlazes) {
            if (b.isAlive()) {
                target = b;
                break;
            }
        }

        if (target != null) {
            shootBlaze(mc, target);
            shootDelay = 30; // Wait 1.5s for arrow to travel and blaze to die
        } else {
            solved = true; // All blazes are dead
        }
    }

    private void scanAndSortBlazes(Minecraft mc) {
        if (mc.level == null) return;

        // Collect all blazes in the room
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Blaze) {
                sortedBlazes.add((Blaze) entity);
            }
        }

        // Determine room type (Ascending or Descending) based on the room layout.
        // For demonstration, we assume Ascending (lowest health first)
        this.ascending = true; 

        // Sort blazes by their max health (which is tied to their size/height in the puzzle)
        sortedBlazes.sort(Comparator.comparingDouble(Blaze::getMaxHealth));
        
        if (!ascending) {
            java.util.Collections.reverse(sortedBlazes);
        }
    }

    private void shootBlaze(Minecraft mc, Blaze target) {
        // Placeholder: 
        // 1. Swap hotbar slot to Bow
        // 2. Use HumanAimSimulator to track the blaze
        // 3. Calculate projectile drop curve based on distance
        // 4. Hold Right Click -> Release
        com.vertexai.util.Logger.sendMessage("Shooting Blaze with HP: " + target.getMaxHealth());
    }

    @Override
    public String getName() {
        return "Blaze Room Solver";
    }
}
