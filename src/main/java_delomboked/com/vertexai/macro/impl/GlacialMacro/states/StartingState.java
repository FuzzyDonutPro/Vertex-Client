package com.vertexai.macro.impl.GlacialMacro.states;

import com.vertexai.Vertex;
import com.vertexai.handler.GameStateHandler;
import com.vertexai.macro.impl.GlacialMacro.GlacialMacro;
import com.vertexai.util.InventoryUtil;
import com.vertexai.util.helper.location.SubLocation;

import java.util.Objects;

/**
 * The initial state of the Glacial Macro.
 * This state checks if the player is in the correct location to start the macro.
 * If not, it will attempt to teleport the player to the Dwarven Base Camp.
 */
public class StartingState implements GlacialMacroState {
    @Override
    public void onStart(GlacialMacro macro) {
        log("Entering starting state");

    }

    @Override
    public GlacialMacroState onTick(GlacialMacro macro) {

        if (Objects.equals(Vertex.config().general.miningTool, "")) {
            macro.disable("Mining tool is not set in the Vertex config");
            return null;
        }

        SubLocation subLocation = GameStateHandler.getInstance().getCurrentSubLocation();
        if (subLocation == SubLocation.DWARVEN_BASE_CAMP) {
            if (!InventoryUtil.areItemsInHotbar(macro.getNecessaryItems())) {
                macro.disable("Please put the following items in hotbar: " + InventoryUtil.getMissingItemsInHotbar(macro.getNecessaryItems()));
                return null;
            }

            log("Player is in a valid location. Initialising stats");
            return new GettingStatsState();
        } else {
            log("Player is not at Dwarven Base Camp. Teleporting...");
            return new TeleportingState(new StartingState());
        }
    }

    @Override
    public void onEnd(GlacialMacro macro) {
        log("Exiting starting state");
    }
}
