package com.vertexai.macro.impl.CommissionMacro.states;

import com.vertexai.Vertex;
import com.vertexai.macro.impl.CommissionMacro.CommissionMacro;
import com.vertexai.util.InventoryUtil;

import java.util.Objects;

public class StartingState implements CommissionMacroState {

    @Override
    public void onStart(CommissionMacro macro) {
        log("Entering starting state");
    }

    @Override
    public CommissionMacroState onTick(CommissionMacro macro) {
        String miningTool = Vertex.config().general.miningTool;
        int miningToolSlot = Vertex.config().general.miningToolSlot;
        if ((miningTool == null || miningTool.trim().isEmpty()) && miningToolSlot <= 0) {
            macro.disable("Please set a Mining Tool (name or slot 1-9) in the config");
            return null;
        }
        String slayerWeapon = Vertex.config().commission.dwarvenCommission.slayerWeapon;
        int slayerWeaponSlot = Vertex.config().commission.dwarvenCommission.slayerWeaponSlot;
        if ((slayerWeapon == null || slayerWeapon.trim().isEmpty()) && slayerWeaponSlot <= 0) {
            macro.disable("Please set a Slayer Weapon (name or slot 1-9) in the config");
            return null;
        }
        if (!InventoryUtil.areItemsInHotbar(macro.getNecessaryItems())) {
            macro.disable("Please put the following items in hotbar: " + InventoryUtil.getMissingItemsInHotbar(macro.getNecessaryItems()));
            return null;
        }
        return macro.getMiningSpeed() == 0 ? new GettingStatsState() : new PathingState();
    }

    @Override
    public void onEnd(CommissionMacro macro) {
        log("Exiting starting state");
    }
}
