package com.vertexai.macro.impl.CommissionMacro.states;

import com.vertexai.Vertex;
import com.vertexai.macro.features.misc.AutoGetStats.AutoGetStats;
import com.vertexai.macro.features.misc.AutoGetStats.tasks.impl.MiningSpeedRetrievalTask;
import com.vertexai.macro.features.misc.AutoGetStats.tasks.impl.PickaxeAbilityRetrievalTask;
import com.vertexai.macro.features.mining.BlockMiner.BlockMiner;
import com.vertexai.macro.impl.CommissionMacro.CommissionMacro;

public class GettingStatsState implements CommissionMacroState {

    private final AutoGetStats autoInventory = AutoGetStats.getInstance();
    private MiningSpeedRetrievalTask miningSpeedRetrievalTask;
    private PickaxeAbilityRetrievalTask pickaxeAbilityRetrievalTask;

    @Override
    public void onStart(CommissionMacro macro) {
        log("Entering getting stats state");
        miningSpeedRetrievalTask = new MiningSpeedRetrievalTask();
        pickaxeAbilityRetrievalTask = new PickaxeAbilityRetrievalTask();
        AutoGetStats.getInstance().startTask(miningSpeedRetrievalTask);
        AutoGetStats.getInstance().startTask(pickaxeAbilityRetrievalTask);
    }

    @Override
    public CommissionMacroState onTick(CommissionMacro macro) {
        if (!AutoGetStats.getInstance().hasFinishedAllTasks())
            return this;

        if (miningSpeedRetrievalTask.getError() != null) {
            macro.disable("Failed to get stats with the following error: " + miningSpeedRetrievalTask.getError());
            return null;
        }

        if (pickaxeAbilityRetrievalTask.getError() != null) {
            macro.disable("Failed to get pickaxe ability with the following error: " + pickaxeAbilityRetrievalTask.getError());
            return null;
        }

        macro.setMiningSpeed(miningSpeedRetrievalTask.getResult());
        macro.setPickaxeAbility(Vertex.config().general.usePickaxeAbility ? pickaxeAbilityRetrievalTask.getResult() : BlockMiner.PickaxeAbility.NONE);
        return new StartingState();
    }

    @Override
    public void onEnd(CommissionMacro macro) {
        autoInventory.stop();
        log("Exiting getting stats state");
    }
}
