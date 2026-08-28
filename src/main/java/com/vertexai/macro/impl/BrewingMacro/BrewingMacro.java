package com.vertexai.macro.impl.BrewingMacro;

import lombok.Getter;
import com.vertexai.macro.AbstractMacro;
import com.vertexai.util.InventoryUtil;
import com.vertexai.util.helper.Clock;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

import java.util.Collections;
import java.util.List;

/**
 * BrewingMacro — Auto Alchemy Potion Brewer.
 * Automates placing Water Bottles, Nether Wart, and Primary Ingredients (Enchanted Sugarcane, Melon, etc.)
 * into Brewing Stands and withdrawing finished Speed/EXP Potions.
 */
public class BrewingMacro extends AbstractMacro {

    public static final BrewingMacro instance = new BrewingMacro();
    public static BrewingMacro getInstance() { return instance; }

    private final Clock delayClock = new Clock();
    private int state = 0; // 0 = Idle/Opening, 1 = Inserting Bottles, 2 = Inserting Ingredient, 3 = Collecting

    @Override
    public String getName() {
        return "Auto Alchemy Brewer";
    }

    @Override
    public List<String> getNecessaryItems() {
        return Collections.emptyList();
    }

    @Override
    public void enable() {
        super.enable();
        state = 0;
        log("Auto Alchemy Brewer Enabled! Open a Brewing Stand or stand near your brewing setup.");
    }

    @Override
    public void disable() {
        super.disable();
        log("Auto Alchemy Brewer Disabled.");
    }

    @Override
    public void onTick() {
        if (!isEnabled() || mc.player == null || mc.gameMode == null) return;
        if (delayClock.isScheduled() && !delayClock.passed()) return;

        // Process inside Brewing Stand GUI
        if (mc.screen instanceof BrewingStandScreen brewScreen) {
            int containerId = brewScreen.getMenu().containerId;

            // Slot IDs in BrewingStandMenu:
            // 0, 1, 2 = Potion Slots (Bottom)
            // 3 = Ingredient Slot (Top)
            // 4 = Blaze Powder Slot (Fuel)

            // Step 1: Withdraw finished potions from slots 0, 1, 2 if brew is complete
            boolean hasPotion0 = !brewScreen.getMenu().getSlot(0).getItem().isEmpty();
            boolean hasIngredient = !brewScreen.getMenu().getSlot(3).getItem().isEmpty();

            if (hasPotion0 && !hasIngredient) {
                // Shift-click finished potions out
                for (int slot = 0; slot <= 2; slot++) {
                    if (!brewScreen.getMenu().getSlot(slot).getItem().isEmpty()) {
                        mc.gameMode.handleContainerInput(containerId, slot, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, mc.player);
                        delayClock.schedule(200);
                        return;
                    }
                }
            }

            // Step 2: Insert Water Bottles into empty slots 0, 1, 2
            int bottleSlotInInv = InventoryUtil.getSlotOfItemInMainInventory("Water Bottle");
            if (bottleSlotInInv == -1) bottleSlotInInv = InventoryUtil.getSlotOfItemInMainInventory("Glass Bottle");

            for (int slot = 0; slot <= 2; slot++) {
                if (brewScreen.getMenu().getSlot(slot).getItem().isEmpty() && bottleSlotInInv != -1) {
                    mc.gameMode.handleContainerInput(containerId, bottleSlotInInv + 36, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, mc.player);
                    delayClock.schedule(250);
                    return;
                }
            }

            // Step 3: Insert Nether Wart / Ingredient into top slot 3
            if (brewScreen.getMenu().getSlot(3).getItem().isEmpty()) {
                int wartSlot = InventoryUtil.getSlotOfItemInMainInventory("Nether Wart");
                if (wartSlot != -1) {
                    mc.gameMode.handleContainerInput(containerId, wartSlot + 36, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, mc.player);
                    delayClock.schedule(300);
                    return;
                }

                int sugarSlot = InventoryUtil.getSlotOfItemInMainInventory("Enchanted Sugar");
                if (sugarSlot == -1) sugarSlot = InventoryUtil.getSlotOfItemInMainInventory("Enchanted Sugarcane");
                if (sugarSlot == -1) sugarSlot = InventoryUtil.getSlotOfItemInMainInventory("Enchanted Melon");

                if (sugarSlot != -1) {
                    mc.gameMode.handleContainerInput(containerId, sugarSlot + 36, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, mc.player);
                    delayClock.schedule(300);
                    return;
                }
            }
        }
    }
}
