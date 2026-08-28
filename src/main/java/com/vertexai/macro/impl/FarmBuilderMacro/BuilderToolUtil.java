package com.vertexai.macro.impl.FarmBuilderMacro;

import com.vertexai.util.InventoryUtil;
import net.minecraft.client.Minecraft;

public class BuilderToolUtil {

    private static final Minecraft mc = Minecraft.getInstance();

    /**
     * Checks if the player has the required Builder Tool in their inventory.
     * @param toolName The internal name of the tool (e.g., "InfiniDirt Wand", "Builder's Ruler")
     * @return true if the tool is in the inventory.
     */
    public static boolean hasTool(String toolName) {
        if (mc.player == null) return false;
        
        int hotbarSlot = InventoryUtil.getHotbarSlotOfItem(toolName);
        if (hotbarSlot != -1) return true;
        
        int invSlot = InventoryUtil.getSlotOfItemInMainInventory(toolName);
        return invSlot != -1;
    }

    /**
     * Equips the requested Builder Tool. Swaps it into the hotbar if necessary.
     * @param toolName The internal name of the tool.
     * @return true if successfully equipped, false if not found.
     */
    public static boolean equipTool(String toolName) {
        if (mc.player == null) return false;

        // Already holding it?
        if (mc.player.getMainHandItem().getHoverName().getString().contains(toolName)) {
            return true;
        }

        // In hotbar?
        int hotbarSlot = InventoryUtil.getHotbarSlotOfItem(toolName);
        if (hotbarSlot != -1) {
            mc.player.getInventory().setSelectedSlot(hotbarSlot);
            return true;
        }

        // In inventory? Need to swap it to hotbar.
        int invSlot = InventoryUtil.getSlotOfItemInMainInventory(toolName);
        if (invSlot != -1 && mc.gameMode != null && mc.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen) {
            // Swap to hotbar slot 0 for builder tools
            mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId, invSlot, 0, net.minecraft.world.inventory.ContainerInput.SWAP, mc.player);
            mc.player.getInventory().setSelectedSlot(0);
            return true;
        } else if (invSlot != -1) {
            // Inventory not open, need to open it first
            com.vertexai.util.InventoryUtil.openInventory();
            return false; // Will return true on next tick when inventory is open
        }

        return false;
    }
}
