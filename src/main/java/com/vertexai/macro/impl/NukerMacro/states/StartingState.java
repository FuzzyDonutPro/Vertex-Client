package com.vertexai.macro.impl.NukerMacro.states;

import com.vertexai.macro.impl.misc.AutoGetStats.AutoGetStats;
import com.vertexai.macro.impl.misc.AutoGetStats.tasks.impl.MiningSpeedRetrievalTask;
import com.vertexai.macro.impl.misc.AutoGetStats.tasks.impl.PickaxeAbilityRetrievalTask;
import com.vertexai.macro.impl.mining.BlockMiner.BlockMiner;
import com.vertexai.macro.impl.NukerMacro.NukerMacro;
import com.vertexai.macro.impl.NukerMacro.NukerMacroState;

public class StartingState implements NukerMacroState {

    private MiningSpeedRetrievalTask miningSpeedRetrievalTask;
    private PickaxeAbilityRetrievalTask pickaxeAbilityRetrievalTask;

    @Override
    public void onStart(NukerMacro macro) {
        log("Starting Nuker Macro. Retrieving stats...");
        miningSpeedRetrievalTask = new MiningSpeedRetrievalTask();
        pickaxeAbilityRetrievalTask = new PickaxeAbilityRetrievalTask();
        
        AutoGetStats.getInstance().startTask(miningSpeedRetrievalTask);
        AutoGetStats.getInstance().startTask(pickaxeAbilityRetrievalTask);
    }

    @Override
    public NukerMacroState onTick(NukerMacro macro) {
        if (!AutoGetStats.getInstance().hasFinishedAllTasks()) {
            return this;
        }

        if (miningSpeedRetrievalTask.getError() != null) {
            macro.disable("Failed to get mining speed: " + miningSpeedRetrievalTask.getError());
            return null;
        }

        if (pickaxeAbilityRetrievalTask.getError() != null) {
            macro.disable("Failed to get pickaxe ability: " + pickaxeAbilityRetrievalTask.getError());
            return null;
        }

        macro.setMiningSpeed(miningSpeedRetrievalTask.getResult());
        macro.setPickaxeAbility(pickaxeAbilityRetrievalTask.getResult());

        return new NukingState();
    }

    @Override
    public void onEnd(NukerMacro macro) {
        log("Stats retrieved successfully.");
    }
}
