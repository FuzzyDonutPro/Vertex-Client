package com.vertexai.macro;

import com.vertexai.config.VertexAIConfig;
import com.vertexai.macro.states.FishingMacroState;
import com.vertexai.macro.states.WarpingState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class FishingMacro {
    private static final long SAFETY_WARNING_COOLDOWN_MS = 5_000L;

    private static final FishingMacro instance = new FishingMacro();
    private final List<String> necessaryItems = new ArrayList<>();
    
    private FishingMacroState currentState;
    private long nextConfigWarningAtMs = 0L;
    private long nextSafetyWarningAtMs = 0L;
    private boolean enabled = false;

    private FishingMacro() { }

    public static FishingMacro getInstance() {
        return instance;
    }

    public static void init() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            getInstance().onTick();
        });
    }

    public String getName() {
        return "Galatea Macro";
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    private void onEnable() {
        this.necessaryItems.clear();
        this.nextConfigWarningAtMs = 0L;
        this.nextSafetyWarningAtMs = 0L;
        
        // Reset inputs
        Minecraft client = Minecraft.getInstance();
        if (Minecraft.getInstance().options != null) {
            Minecraft.getInstance().options.keyUp.setDown(false);
            Minecraft.getInstance().options.keyDown.setDown(false);
            Minecraft.getInstance().options.keyLeft.setDown(false);
            Minecraft.getInstance().options.keyRight.setDown(false);
        }

        this.currentState = new WarpingState();
        this.currentState.onStart(this);
        log(getName() + " enabled");
    }

    private void onDisable() {
        if (this.currentState != null) {
            this.currentState.onEnd(this);
        }
        this.currentState = null;
        
        Minecraft client = Minecraft.getInstance();
        if (Minecraft.getInstance().options != null) {
            Minecraft.getInstance().options.keyUp.setDown(false);
            Minecraft.getInstance().options.keyDown.setDown(false);
            Minecraft.getInstance().options.keyLeft.setDown(false);
            Minecraft.getInstance().options.keyRight.setDown(false);
        }
        log(getName() + " disabled");
    }

    public List<String> getNecessaryItems() {
        this.necessaryItems.clear();
        VertexAIConfig config = VertexAIConfig.getInstance();
        
        String rodName = config.fishingRod;
        if (rodName != null && !rodName.trim().isEmpty()) {
            this.necessaryItems.add(rodName);
        }

        String axeName = config.galateaAxe;
        if (axeName != null && !axeName.trim().isEmpty()) {
            this.necessaryItems.add(axeName);
        }

        String weaponName = config.galateaFishingWeapon;
        if (usesSlayerWeaponMode() && weaponName != null && !weaponName.trim().isEmpty()) {
            this.necessaryItems.add(weaponName);
        }
        return this.necessaryItems;
    }

    public void onTick() {
        if (!this.isEnabled() || this.currentState == null) {
            return;
        }

        if (!validateConfigForCurrentMode()) {
            return;
        }

        FishingMacroState nextState = this.currentState.onTick(this);
        transitionTo(nextState);
    }

    // Always returns true as per user request to bypass location check
    public boolean isInGalatea() {
        return true;
    }

    private void transitionTo(FishingMacroState nextState) {
        if (this.currentState == nextState || nextState == null) {
            return;
        }

        this.currentState.onEnd(this);
        this.currentState = nextState;
        this.currentState.onStart(this);
    }

    public boolean usesSlayerWeaponMode() {
        return VertexAIConfig.getInstance().galateaKillMode == 1;
    }

    private boolean validateConfigForCurrentMode() {
        VertexAIConfig config = VertexAIConfig.getInstance();
        
        String rodName = config.fishingRod;
        if (rodName == null || rodName.trim().isEmpty()) {
            warnConfig("Set Fishing Rod in config first.");
            return false;
        }

        String axeName = config.galateaAxe;
        if (axeName == null || axeName.trim().isEmpty()) {
            warnConfig("Set Galatea Axe in config first.");
            return false;
        }

        if (usesSlayerWeaponMode()) {
            String weaponName = config.galateaFishingWeapon;
            if (weaponName == null || weaponName.trim().isEmpty()) {
                warnConfig("Kill Mode is Slayer Weapon, but no secondary weapon is configured.");
                return false;
            }
        }

        return true;
    }

    private void warnConfig(String message) {
        long now = System.currentTimeMillis();
        if (now < nextConfigWarningAtMs) {
            return;
        }
        nextConfigWarningAtMs = now + 5_000L;
        warn(message);
    }

    // Failsafe is handled externally by ChatAIHandler.
    // If we receive a name mention request, we disable the macro.
    public void triggerSafetyStop(String reason) {
        warnSafety(reason);
        setEnabled(false);
    }

    private void warnSafety(String message) {
        long now = System.currentTimeMillis();
        if (now < nextSafetyWarningAtMs) {
            return;
        }
        nextSafetyWarningAtMs = now + SAFETY_WARNING_COOLDOWN_MS;
        warn(message);
    }
    
    private void log(String message) {
        System.out.println("[" + getName() + "] " + message);
    }

    private void warn(String message) {
        Minecraft client = Minecraft.getInstance();
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("§c[" + getName() + "] " + message));
        }
        System.err.println("[" + getName() + "] WARNING: " + message);
    }
}
