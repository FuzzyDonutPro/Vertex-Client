package com.vertexai.macro.impl.RouteMiner.states;

import com.vertexai.Vertex;
import com.vertexai.macro.features.misc.AutoGetStats.AutoGetStats;
import com.vertexai.macro.features.misc.AutoGetStats.tasks.impl.MiningSpeedRetrievalTask;
import com.vertexai.macro.features.misc.AutoGetStats.tasks.impl.PickaxeAbilityRetrievalTask;
import com.vertexai.macro.features.mining.BlockMiner.BlockMiner;
import com.vertexai.macro.impl.RouteMiner.RouteMinerMacro;

/**
 * This state is responsible for retrieving the mining speed and pickaxe ability
 * before proceeding to the mining state in the Route Miner Macro.
 */
public class GettingStatsState implements RouteMinerMacroState {

    private final AutoGetStats autoInventory = AutoGetStats.getInstance();
    private MiningSpeedRetrievalTask miningSpeedTask;
    private PickaxeAbilityRetrievalTask pickaxeAbilityTask;

    @Override
    public void onStart(RouteMinerMacro macro) {
        log("Entering getting stats state");
        miningSpeedTask = new MiningSpeedRetrievalTask();
        pickaxeAbilityTask = new PickaxeAbilityRetrievalTask();
        AutoGetStats.getInstance().startTask(pickaxeAbilityTask);
        AutoGetStats.getInstance().startTask(miningSpeedTask);
    }

    @Override
    public RouteMinerMacroState onTick(RouteMinerMacro macro) {
        if (AutoGetStats.getInstance().hasFinishedAllTasks()) {
            if (miningSpeedTask.getError() != null) {
                macro.disable("Failed to get stats with the following error: " + miningSpeedTask.getError());
                return null;
            }

            if (pickaxeAbilityTask.getError() != null) {
                macro.disable("Failed to get pickaxe ability with the following error: " + pickaxeAbilityTask.getError());
                return null;
            }

            macro.setMiningSpeed(miningSpeedTask.getResult());
            macro.setPickaxeAbility(Vertex.config().general.usePickaxeAbility ? pickaxeAbilityTask.getResult() : BlockMiner.PickaxeAbility.NONE);
            return new MovingState();
        } else {
            return this;
        }

    }

    @Override
    public void onEnd(RouteMinerMacro macro) {
        autoInventory.stop();
        log("Exiting getting stats state");
    }

}
