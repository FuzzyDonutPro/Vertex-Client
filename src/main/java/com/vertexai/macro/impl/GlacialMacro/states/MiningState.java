package com.vertexai.macro.impl.GlacialMacro.states;

import akka.japi.Pair;
import com.vertexai.Vertex;
import com.vertexai.feature.impl.BlockMiner.BlockMiner;
import com.vertexai.macro.impl.GlacialMacro.GlacialMacro;
import com.vertexai.macro.impl.GlacialMacro.GlaciteVeins;
import com.vertexai.util.InventoryUtil;
import com.vertexai.util.ScoreboardUtil;
import com.vertexai.util.TablistUtil;
import com.vertexai.util.helper.Clock;
import com.vertexai.util.helper.MineableBlock;
import com.vertexai.util.helper.route.RouteWaypoint;

/**
 * Represents the mining state of the Glacial Macro.
 * Handles the mining process, retries, and transitions to other states as needed.
 */
public class MiningState implements GlacialMacroState {

    private final BlockMiner miner = BlockMiner.getInstance();
    private final int MAX_RETRIES = 3;
    private final Clock retryClock = new Clock();
    private int miningRetries = 0;

    @Override
    public void onStart(GlacialMacro macro) {
        log("Starting to mine at vein: " + (macro.getCurrentVein() != null ? macro.getCurrentVein().first() : "Unknown"));
        int slot = Vertex.config().general.miningToolSlot;
        String tool = (slot >= 1 && slot <= 9) ? String.valueOf(slot) : Vertex.config().general.miningTool;
        InventoryUtil.holdItem(tool);
        this.miningRetries = 0;
        startMining(macro);
    }

    @Override
    public GlacialMacroState onTick(GlacialMacro macro) {
        if (ScoreboardUtil.cold >= Vertex.config().commission.glacialCommission.coldThreshold) {
            send("Player is too cold. Warping to base to reset.");
            return new TeleportingState(new PathfindingState());
        }

        // Check for completed commissions
        boolean hasCompletedComm = TablistUtil.getGlaciteComs().values().stream().anyMatch(v -> v >= 100.0);
        if (hasCompletedComm) {
            log("Completed commission detected. Claiming...");
            return new ClaimingCommissionState();
        }

        if (miner.getError() == BlockMiner.BlockMinerError.NOT_ENOUGH_BLOCKS) {
            miner.stop();
            if (++miningRetries > MAX_RETRIES) {
                log("No more blocks to mine after " + MAX_RETRIES + " attempts. Finding new vein.");

                Pair<GlaciteVeins, RouteWaypoint> failedVein = macro.getCurrentVein();
                if (failedVein != null) {
                    log("Blacklisting current vein: " + failedVein.first());
                    macro.getPreviousVeins().put(failedVein, System.currentTimeMillis());
                }

                return new PathfindingState();
            }
            log("Not enough blocks. Retrying in 5 seconds... (" + miningRetries + "/" + MAX_RETRIES + ")");
            retryClock.schedule(5000);
            return this;
        }

        if (!miner.isRunning() && retryClock.passed()) {
            log("Miner is not running. Restarting.");
            startMining(macro);
        }

        return this; // Stay in mining state
    }

    private void startMining(GlacialMacro macro) {
        MineableBlock[] blocksToMine = macro.getBlocksToMine();
        if (blocksToMine.length == 0) {
            log("No blocks to mine for current commissions.");
            return;
        }
        int[] blockPriorities = macro.getBlockPriority();
        miner.start(
                blocksToMine,
                macro.getMiningSpeed(),
                macro.getPickaxeAbility(),
                blockPriorities,
                Vertex.config().general.miningTool
        );
    }

    @Override
    public void onEnd(GlacialMacro macro) {
        log("Stopping miner.");
        miner.stop();
    }
}
