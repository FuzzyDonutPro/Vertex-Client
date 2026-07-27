package com.vertexai.macro.impl.FarmingMacro;

import lombok.Getter;
import com.vertexai.Vertex;
import com.vertexai.macro.AbstractMacro;
import com.vertexai.macro.impl.FarmingMacro.states.StartingState;
import com.vertexai.util.KeyBindUtil;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class FarmingMacro extends AbstractMacro {

    @Getter
    private static final FarmingMacro instance = new FarmingMacro();

    private FarmingMacroState currentState;
    private final List<String> necessaryItems = new ArrayList<>();

    @Override
    public String getName() {
        return "Crop/Wart S-Shape";
    }

    @Override
    public void onEnable() {
        log("Enabling Crop/Wart S-Shape");
        this.currentState = new StartingState();
        this.currentState.onStart(this);
    }

    @Override
    public void onDisable() {
        log("Disabling Crop/Wart S-Shape");
        if (currentState != null) currentState.onEnd(this);
        
        // Reset all keybinds
        Minecraft mc = Minecraft.getInstance();
        KeyBindUtil.setKeyBindState(mc.options.keyLeft, false);
        KeyBindUtil.setKeyBindState(mc.options.keyRight, false);
        KeyBindUtil.setKeyBindState(mc.options.keyUp, false);
        KeyBindUtil.setKeyBindState(mc.options.keyDown, false);
        KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        if (currentState == null) return;
        
        FarmingMacroState nextState = this.currentState.onTick(this);
        transitionTo(nextState);
    }
    
    private void transitionTo(FarmingMacroState nextState) {
        if (nextState == null || nextState == currentState) return;
        currentState.onEnd(this);
        currentState = nextState;
        currentState.onStart(this);
    }

    @Override
    public List<String> getNecessaryItems() {
        if (necessaryItems.isEmpty()) {
            necessaryItems.add(Vertex.config().farming.farmingTool);
        }
        return necessaryItems;
    }
}
