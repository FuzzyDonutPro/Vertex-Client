package com.vertexai.dungeons.puzzles;

import com.vertexai.Vertex;
import com.vertexai.util.InventoryUtil;
import com.vertexai.util.Logger;
import com.vertexai.util.helper.Clock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DungeonTerminalSolver
 * <p>
 * Full automated solver for F7 / M7 Goldor terminals:
 * 1. Click in Order (Numbers 1-14)
 * 2. Select all the [Color] items!
 * 3. What starts with 'X'?
 * 4. Change all to same color (Rubix)
 */
public class DungeonTerminalSolver {

    private static final DungeonTerminalSolver instance = new DungeonTerminalSolver();
    public static DungeonTerminalSolver getInstance() { return instance; }

    private final Minecraft mc = Minecraft.getInstance();
    private final Clock clickClock = new Clock();
    private int currentNumberTarget = 1;
    private String lastContainerTitle = "";

    public void onTick() {
        if (!Vertex.config().dungeons.autoTerminalSolver) return;
        if (mc.player == null || mc.screen == null) {
            this.currentNumberTarget = 1;
            this.lastContainerTitle = "";
            return;
        }

        if (!(mc.screen instanceof AbstractContainerScreen<?> containerScreen)) {
            this.currentNumberTarget = 1;
            return;
        }

        String title = containerScreen.getTitle().getString();
        if (title.isEmpty()) return;

        // If newly opened terminal, reset state
        if (!title.equals(this.lastContainerTitle)) {
            this.lastContainerTitle = title;
            this.currentNumberTarget = 1;
            this.clickClock.schedule(Vertex.config().dungeons.terminalClickDelay);
            return;
        }

        if (!clickClock.passed()) return;

        if (mc.player.containerMenu instanceof ChestMenu chest) {
            if (title.toLowerCase().contains("click in order")) {
                solveNumbersTerminal(chest);
            } else if (title.toLowerCase().contains("what starts with")) {
                solveStartsWithLetterTerminal(chest, title);
            } else if (title.toLowerCase().contains("select all the")) {
                solveColorTerminal(chest, title);
            } else if (title.toLowerCase().contains("change all to same color") || title.toLowerCase().contains("rubix")) {
                solveRubixTerminal(chest);
            }
        }
    }

    /**
     * Solves "Click in order!" (1 through 14)
     */
    private void solveNumbersTerminal(ChestMenu chest) {
        int targetSlot = -1;

        for (int i = 0; i < 36 && i < chest.slots.size(); i++) {
            Slot slot = chest.slots.get(i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            // In Hypixel numbers terminal, items have stack size equal to number (1-14)
            // or display name with the number
            int number = stack.getCount();
            String name = stack.getHoverName().getString();
            
            if (number == this.currentNumberTarget || name.trim().equals(String.valueOf(this.currentNumberTarget))) {
                // If it's a red/unclicked pane
                if (stack.getItem() == Items.RED_STAINED_GLASS_PANE || stack.getItem() != Items.LIME_STAINED_GLASS_PANE) {
                    targetSlot = i;
                    break;
                }
            }
        }

        if (targetSlot != -1) {
            clickSlot(targetSlot);
            this.currentNumberTarget++;
            this.clickClock.schedule(Vertex.config().dungeons.terminalClickDelay);
        }
    }

    /**
     * Solves "What starts with 'X'?"
     */
    private void solveStartsWithLetterTerminal(ChestMenu chest, String title) {
        Pattern pattern = Pattern.compile("What starts with: '([A-Za-z])'|What starts with '([A-Za-z])'");
        Matcher matcher = pattern.matcher(title);
        if (!matcher.find()) return;

        String letter = (matcher.group(1) != null ? matcher.group(1) : matcher.group(2)).toLowerCase();

        for (int i = 0; i < chest.slots.size() - 36; i++) {
            Slot slot = chest.slots.get(i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            String name = stack.getHoverName().getString().trim();
            // Remove color codes if any
            name = name.replaceAll("§[0-9a-fk-or]", "");

            if (name.toLowerCase().startsWith(letter)) {
                // Check if already clicked (glow/enchanted or green pane)
                if (!stack.isEnchanted() && stack.getItem() != Items.LIME_STAINED_GLASS_PANE) {
                    clickSlot(i);
                    this.clickClock.schedule(Vertex.config().dungeons.terminalClickDelay);
                    return;
                }
            }
        }
    }

    /**
     * Solves "Select all the [Color] items!"
     */
    private void solveColorTerminal(ChestMenu chest, String title) {
        Pattern pattern = Pattern.compile("Select all the ([A-Za-z]+) items!");
        Matcher matcher = pattern.matcher(title);
        if (!matcher.find()) return;

        String targetColor = matcher.group(1).toLowerCase();

        for (int i = 0; i < chest.slots.size() - 36; i++) {
            Slot slot = chest.slots.get(i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            String name = stack.getHoverName().getString().toLowerCase().replaceAll("§[0-9a-fk-or]", "");
            boolean matchesColor = name.contains(targetColor);

            if (matchesColor && !stack.isEnchanted() && stack.getItem() != Items.LIME_STAINED_GLASS_PANE) {
                clickSlot(i);
                this.clickClock.schedule(Vertex.config().dungeons.terminalClickDelay);
                return;
            }
        }
    }

    /**
     * Solves Rubix / Uniform Color panes
     */
    private void solveRubixTerminal(ChestMenu chest) {
        for (int i = 0; i < chest.slots.size() - 36; i++) {
            Slot slot = chest.slots.get(i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            // Click non-green panes until they turn lime/green
            if (stack.getItem() != Items.LIME_STAINED_GLASS_PANE && (stack.getItem() == Items.RED_STAINED_GLASS_PANE || stack.getItem() == Items.ORANGE_STAINED_GLASS_PANE)) {
                clickSlot(i);
                this.clickClock.schedule(Vertex.config().dungeons.terminalClickDelay);
                return;
            }
        }
    }

    private void clickSlot(int slotIndex) {
        InventoryUtil.clickContainerSlot(slotIndex, 0, InventoryUtil.ClickMode.PICKUP);
    }
}
