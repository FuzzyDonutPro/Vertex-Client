package com.vertexai.macro.impl.CommissionMacro.states;

import com.vertexai.feature.impl.AutoWarp;
import com.vertexai.handler.GameStateHandler;
import com.vertexai.macro.impl.CommissionMacro.CommissionMacro;
import com.vertexai.util.helper.location.Location;
import com.vertexai.util.helper.location.SubLocation;

public class WarpingState implements CommissionMacroState {

    private final AutoWarp autoWarp = AutoWarp.getInstance();
    private boolean triedForge = false;
    private boolean triedMines = false;

    @Override
    public void onStart(CommissionMacro macro) {
        log("Starting warping state");
        if (GameStateHandler.getInstance().inDwarvenMines() || GameStateHandler.getInstance().getCurrentLocation() == Location.DWARVEN_MINES) {
            log("Already in Dwarven Mines, skipping warp.");
            return;
        }
        this.triedForge = true;
        autoWarp.start(null, SubLocation.THE_FORGE);
    }

    @Override
    public CommissionMacroState onTick(CommissionMacro macro) {
        // If already in Dwarven Mines, we succeeded!
        if (GameStateHandler.getInstance().inDwarvenMines() || GameStateHandler.getInstance().getCurrentLocation() == Location.DWARVEN_MINES) {
            log("Confirmed in Dwarven Mines, transitioning to StartingState.");
            if (autoWarp.isRunning()) {
                autoWarp.stop();
            }
            return new StartingState();
        }

        if (AutoWarp.getInstance().isRunning()) {
            return this;
        }

        if (AutoWarp.getInstance().hasSucceeded()) {
            log("Auto Warp Completed");
            return new StartingState();
        }

        switch (AutoWarp.getInstance().getFailReason()) {
            case NONE:
                if (!triedMines) {
                    log("Attempting fallback warp to Dwarven Mines (/warp mines)...");
                    triedMines = true;
                    autoWarp.start(Location.DWARVEN_MINES, null);
                    return this;
                }
                macro.disable("Auto Warp failed to reach Dwarven Mines. Please warp to Dwarven Mines manually.");
                break;
            case FAILED_TO_WARP:
                if (!triedMines) {
                    log("Forge warp failed, trying /warp mines fallback...");
                    triedMines = true;
                    autoWarp.start(Location.DWARVEN_MINES, null);
                    return this;
                }
                log("Retrying Auto Warp to Mines");
                autoWarp.start(Location.DWARVEN_MINES, null);
                break;
            case NO_SCROLL:
                if (!triedMines) {
                    log("No Forge scroll, falling back to /warp mines...");
                    triedMines = true;
                    autoWarp.start(Location.DWARVEN_MINES, null);
                    return this;
                }
                macro.disable("Please unlock /warp mines or /warp forge scroll!");
                break;
        }
        return null;
    }

    @Override
    public void onEnd(CommissionMacro macro) {
        autoWarp.stop();
        log("Ending warping state");
    }
}
