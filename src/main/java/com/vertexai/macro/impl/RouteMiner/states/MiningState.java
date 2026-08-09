package com.vertexai.macro.impl.RouteMiner.states;

import com.vertexai.Vertex;
import com.vertexai.macro.impl.mining.BlockMiner.BlockMiner;
import com.vertexai.macro.impl.RouteMiner.RouteMinerMacro;
import com.vertexai.util.InventoryUtil;
import com.vertexai.util.helper.MineableBlock;

/**
 * This state is responsible for starting BlockMiner and detecting when to move to next waypoint
 * before proceeding to the moving state in the Route Miner Macro.
 */
public class MiningState implements RouteMinerMacroState {

    @Override
    public void onStart(RouteMinerMacro macro) {
        log("Entering Mining State");
        int slot = Vertex.config().general.miningToolSlot;
        String tool = (slot >= 1 && slot <= 9) ? String.valueOf(slot) : Vertex.config().general.miningTool;
        InventoryUtil.holdItem(tool);
        startMining(macro);
    }

    @Override
    public RouteMinerMacroState onTick(RouteMinerMacro macro) {
        if (BlockMiner.getInstance().getError() == BlockMiner.BlockMinerError.NOT_ENOUGH_BLOCKS) {
            BlockMiner.getInstance().stop();
            macro.setRouteIndex(macro.getRouteIndex() + 1);
            return new MovingState();
        }

        return this;
    }

    private void startMining(RouteMinerMacro macro) {
        MineableBlock[] blocksToMine = macro.getBlocksToMine();

        if (blocksToMine.length == 0) {
            macro.disable("No targets provided in configuration.");
            return;
        }

        BlockMiner.getInstance().start(
                blocksToMine,
                macro.getMiningSpeed(),
                macro.getPickaxeAbility(),
                macro.getBlockPriority(),
                Vertex.config().general.miningTool
        );
    }

    @Override
    public void onEnd(RouteMinerMacro macro) {
        log("Exiting Mining State");
    }

}
