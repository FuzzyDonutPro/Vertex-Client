package com.vertexai.macro.impl.PowderMacro.states;

import com.vertexai.Vertex;
import com.vertexai.macro.impl.PowderMacro.PowderMacro;
import com.vertexai.util.InventoryUtil;

public class StartingState implements PowderMacroState {

    @Override
    public void onStart(PowderMacro macro) {
        log("Entering starting state");
    }

    @Override
    public PowderMacroState onTick(PowderMacro macro) {
        if (!macro.validateEnvironment()) {
            return null;
        }

        if (!InventoryUtil.areItemsInHotbar(macro.getNecessaryItems())) {
            macro.disable("Please put these items in your hotbar: " + InventoryUtil.getMissingItemsInHotbar(macro.getNecessaryItems()));
            return null;
        }

        if (!macro.updateStatsIfReady()) {
            return this;
        }

        String mode = Vertex.config().powderMacro.powderType == 0 ? "Gemstone" : "Mithril";
        log("Powder mode selected: " + mode);
        return new GemstoneState();
    }

    @Override
    public void onEnd(PowderMacro macro) {
        log("Exiting starting state");
    }
}
