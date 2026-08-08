package com.vertexai.macro.impl.NukerMacro.states;

import com.vertexai.Vertex;
import com.vertexai.macro.features.mining.BlockMiner.BlockMiner;
import com.vertexai.macro.impl.NukerMacro.NukerMacro;
import com.vertexai.macro.impl.NukerMacro.NukerMacroState;
import com.vertexai.util.helper.MineableBlock;

public class NukingState implements NukerMacroState {

    private final BlockMiner miner = BlockMiner.getInstance();
    
    // Target Mithril and Titanium
    private final MineableBlock[] blocksToMine = {
        MineableBlock.GRAY_MITHRIL, 
        MineableBlock.GREEN_MITHRIL, 
        MineableBlock.BLUE_MITHRIL,
        MineableBlock.TITANIUM
    };
    
    // Priority for targeting
    private final int[] mithrilPriority = {10, 6, 3, 1};

    @Override
    public void onStart(NukerMacro macro) {
        log("Started Nuking");
        
        miner.start(
            blocksToMine,
            macro.getMiningSpeed(),
            macro.getPickaxeAbility(),
            mithrilPriority,
            Vertex.config().general.miningTool
        );
        
        // Lower the wait threshold so the macro doesn't hang if there are no blocks
        miner.setWaitThreshold(1000); 
    }

    @Override
    public NukerMacroState onTick(NukerMacro macro) {
        if (miner.isRunning()) {
            return this;
        }

        switch (miner.getError()) {
            case NONE:
                break;
            case NO_POINTS_FOUND:
                log("Block rejected by raytrace, picking a new one.");
                return new NukingState();
            case NOT_ENOUGH_BLOCKS:
                log("No mithril within range. Waiting for respawn...");
                // Just restart the state to scan again
                return new NukingState();
            case NO_PICKAXE_ABILITY:
                macro.disable("Cannot find messages for pickaxe ability! Disable in configs.");
                break;
            default:
                logError("Block miner error: " + miner.getError().name());
                macro.disable("Block miner failed unexpectedly!");
                break;
        }
        
        return null;
    }

    @Override
    public void onEnd(NukerMacro macro) {
        miner.stop();
        log("Stopped Nuking");
    }
}
