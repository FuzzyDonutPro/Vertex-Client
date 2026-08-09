package com.vertexai.macro.impl.ForagingMacro.states;

import com.vertexai.macro.impl.ForagingMacro.ForagingMacro;
import com.vertexai.macro.impl.ForagingMacro.ForagingMacroState;
import com.vertexai.handler.BlockBreakingEngine;
import com.vertexai.util.InventoryUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public class BreakingState implements ForagingMacroState {

    private final Minecraft mc = Minecraft.getInstance();

    @Override
    public void onStart(ForagingMacro macro) {
        log("Aiming and breaking target tree...");
        if (macro.getTargetBlockPos() != null) {
            BlockBreakingEngine.getInstance().breakBlock(macro.getTargetBlockPos());
        }
    }

    @Override
    public ForagingMacroState onTick(ForagingMacro macro) {
        if (mc.player == null || mc.level == null || macro.getTargetBlockPos() == null) {
            BlockBreakingEngine.getInstance().stopBreaking();
            return new PathfindingState();
        }

        BlockPos targetPos = macro.getTargetBlockPos();

        // Auto-swap to Treecapitator / Jungle Axe / Axe in hotbar
        int axeSlot = InventoryUtil.getHotbarSlotOfItem("Treecapitator");
        if (axeSlot == -1) axeSlot = InventoryUtil.getHotbarSlotOfItem("Jungle Axe");
        if (axeSlot == -1) axeSlot = InventoryUtil.getHotbarSlotOfItem("Axe");
        if (axeSlot != -1 && mc.player.getInventory().getSelectedSlot() != axeSlot) {
            mc.player.getInventory().setSelectedSlot(axeSlot);
        }

        boolean stillMining = BlockBreakingEngine.getInstance().breakBlock(targetPos);
        if (!stillMining) {
            log("Tree log broken!");
            return new PathfindingState();
        }

        if (BlockBreakingEngine.getInstance().getBreakDurationMs() > 5000L) {
            log("Log taking too long to break, switching target...");
            BlockBreakingEngine.getInstance().stopBreaking();
            return new PathfindingState();
        }

        return this;
    }

    @Override
    public void onEnd(ForagingMacro macro) {
        BlockBreakingEngine.getInstance().stopBreaking();
    }
}
