package com.vertexai.macro.impl.NukerMacro;

import lombok.Getter;
import lombok.Setter;
import com.vertexai.Vertex;
import com.vertexai.macro.impl.mining.BlockMiner.BlockMiner;
import com.vertexai.macro.AbstractMacro;
import com.vertexai.macro.impl.NukerMacro.states.StartingState;

import java.util.ArrayList;
import java.util.List;

public class NukerMacro extends AbstractMacro {

    private static final NukerMacro instance = new NukerMacro();
    private int miningSpeed = 0;
    private BlockMiner.PickaxeAbility pickaxeAbility = BlockMiner.PickaxeAbility.NONE;

    public static NukerMacro getInstance() { return instance; }
    public int getMiningSpeed() { return miningSpeed; }
    public void setMiningSpeed(int miningSpeed) { this.miningSpeed = miningSpeed; }
    public BlockMiner.PickaxeAbility getPickaxeAbility() { return pickaxeAbility; }
    public void setPickaxeAbility(BlockMiner.PickaxeAbility pickaxeAbility) { this.pickaxeAbility = pickaxeAbility; }

    private NukerMacroState currentState;
    private final List<String> necessaryItems = new ArrayList<>();

    @Override
    public String getName() {
        return "Mithril Nuker";
    }

    @Override
    public void onEnable() {
        log("Enabling Mithril Nuker");
        this.miningSpeed = 0;
        this.currentState = new StartingState();
        this.currentState.onStart(this);
    }

    @Override
    public void onDisable() {
        log("Disabling Mithril Nuker");
        if (currentState != null) currentState.onEnd(this);
        BlockMiner.getInstance().stop();
        this.miningSpeed = 0;
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        if (currentState == null) return;
        
        NukerMacroState nextState = this.currentState.onTick(this);
        transitionTo(nextState);
    }
    
    private void transitionTo(NukerMacroState nextState) {
        if (nextState == null || nextState == currentState) return;
        currentState.onEnd(this);
        currentState = nextState;
        currentState.onStart(this);
    }

    @Override
    public List<String> getNecessaryItems() {
        if (necessaryItems.isEmpty()) {
            necessaryItems.add(Vertex.config().general.miningTool);
        }
        return necessaryItems;
    }
}
