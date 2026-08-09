package com.vertexai.macro.impl.SlayerMacro;

import lombok.Getter;
import com.vertexai.Vertex;
import com.vertexai.macro.impl.combat.AutoMobKiller.AutoMobKiller;
import com.vertexai.macro.AbstractMacro;
import com.vertexai.macro.impl.SlayerMacro.states.StartingState;

import java.util.ArrayList;
import java.util.List;

public class SlayerMacro extends AbstractMacro {

    private static final SlayerMacro instance = new SlayerMacro();
    public static SlayerMacro getInstance() { return instance; }

    private SlayerMacroState currentState;
    private final List<String> necessaryItems = new ArrayList<>();

    @Override
    public String getName() {
        return "Slayer Macro";
    }

    @Override
    public void onEnable() {
        log("Enabling Slayer Macro");
        this.currentState = new StartingState();
        this.currentState.onStart(this);
    }

    @Override
    public void onDisable() {
        log("Disabling Slayer Macro");
        try {
            if (currentState != null) currentState.onEnd(this);
        } catch (Exception ignored) {}
        try {
            AutoMobKiller.getInstance().stop();
        } catch (Exception ignored) {}
        this.currentState = null;
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        if (currentState == null) return;
        
        SlayerMacroState nextState = this.currentState.onTick(this);
        transitionTo(nextState);
    }
    
    private void transitionTo(SlayerMacroState nextState) {
        if (nextState == null || nextState == currentState) return;
        currentState.onEnd(this);
        currentState = nextState;
        currentState.onStart(this);
    }

    @Override
    public List<String> getNecessaryItems() {
        if (necessaryItems.isEmpty()) {
            necessaryItems.add(Vertex.config().general.miningTool); // Uses miningTool as the weapon
        }
        return necessaryItems;
    }
}
