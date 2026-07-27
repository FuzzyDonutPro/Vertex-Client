package com.vertexai.macro.impl.ForagingMacro;

import com.vertexai.macro.AbstractMacro;
import com.vertexai.macro.impl.ForagingMacro.states.StartingState;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;

public class ForagingMacro extends AbstractMacro {

    @Getter
    private static final ForagingMacro instance = new ForagingMacro();

    private ForagingMacroState currentState;

    @Getter @Setter
    private BlockPos targetBlockPos;

    @Getter @Setter
    private String currentForagingMode = "";

    @Override
    public void onEnable() {
        super.onEnable();
        log("Starting Foraging Macro...");
        this.targetBlockPos = null;
        this.currentState = new StartingState();
        if (this.currentState != null) {
            this.currentState.onStart(this);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        log("Stopping Foraging Macro...");
        if (this.currentState != null) {
            this.currentState.onEnd(this);
        }
        this.currentState = null;
        this.targetBlockPos = null;
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        
        // Wait for player to exist
        if (net.minecraft.client.Minecraft.getInstance().player == null || net.minecraft.client.Minecraft.getInstance().level == null) return;
        
        if (this.currentState != null) {
            ForagingMacroState nextState = this.currentState.onTick(this);
            if (nextState != null && nextState != this.currentState) {
                this.currentState.onEnd(this);
                this.currentState = nextState;
                this.currentState.onStart(this);
            }
        }
    }

    @Override
    public java.util.List<String> getNecessaryItems() {
        return java.util.Collections.emptyList();
    }

    @Override
    public String getName() {
        return "Foraging Macro";
    }
}
