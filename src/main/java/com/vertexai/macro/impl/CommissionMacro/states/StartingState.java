package com.vertexai.macro.impl.CommissionMacro.states;

import com.vertexai.Vertex;
import com.vertexai.macro.impl.CommissionMacro.Commission;
import com.vertexai.macro.impl.CommissionMacro.CommissionMacro;
import com.vertexai.util.CommissionUtil;
import com.vertexai.util.InventoryUtil;

import java.util.List;
import java.util.Objects;

public class StartingState implements CommissionMacroState {

    @Override
    public void onStart(CommissionMacro macro) {
        log("Entering starting state");
    }

    @Override
    public CommissionMacroState onTick(CommissionMacro macro) {
        if (Objects.equals(Vertex.config().general.miningTool, "")) {
            macro.disable("Please set a Mining Tool in the config");
            return null;
        }
        if (!InventoryUtil.areItemsInHotbar(macro.getNecessaryItems())) {
            macro.disable("Please put the following items in hotbar: " + InventoryUtil.getMissingItemsInHotbar(macro.getNecessaryItems()));
            return null;
        }

        // Try reading commission immediately on startup
        List<Commission> comms = CommissionUtil.getCurrentCommissionsFromTablist();
        if (!comms.isEmpty()) {
            macro.setCurrentCommission(comms.get(0));
            log("Discovered active commission from tablist: " + comms.get(0).getName());
        }

        return macro.getMiningSpeed() == 0 ? new GettingStatsState() : new PathingState();
    }

    @Override
    public void onEnd(CommissionMacro macro) {
        log("Exiting starting state");
    }
}
