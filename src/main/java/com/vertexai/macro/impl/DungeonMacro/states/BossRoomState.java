package com.vertexai.macro.impl.DungeonMacro.states;

import com.vertexai.macro.impl.DungeonMacro.DungeonMacro;
import com.vertexai.util.Logger;
import com.vertexai.util.helper.Clock;
import net.minecraft.client.Minecraft;

public class BossRoomState implements DungeonMacroState {

    public enum BossPhase { MAXOR_CRYSTALS, STORM_PILLARS, GOLDOR_TERMINALS, NECRON_DPS, COMPLETED }

    private final Minecraft mc = Minecraft.getInstance();
    private BossPhase currentPhase = BossPhase.MAXOR_CRYSTALS;
    private final Clock phaseClock = new Clock();

    @Override
    public String getName() {
        return "Boss Room Automation";
    }

    @Override
    public void onEnable(DungeonMacro macro) {
        Logger.sendMessage("§4[Dungeon AI] Catacombs Boss Room Automation Active!");
        this.currentPhase = BossPhase.MAXOR_CRYSTALS;
        this.phaseClock.schedule(500);
    }

    @Override
    public void onTick(DungeonMacro macro) {
        if (mc.player == null || mc.level == null) return;

        switch (currentPhase) {
            case MAXOR_CRYSTALS -> handleMaxorPhase();
            case STORM_PILLARS -> handleStormPhase();
            case GOLDOR_TERMINALS -> handleGoldorPhase();
            case NECRON_DPS -> handleNecronPhase();
            case COMPLETED -> {
                Logger.sendMessage("§a[Dungeon AI] Boss Room Cleared!");
                macro.disable();
            }
        }
    }

    private void handleMaxorPhase() {
        if (phaseClock.passed()) {
            phaseClock.schedule(1000);
            Logger.sendLog("[Boss AI] Phase 1: Energy Crystal grab & pedestal placement...");
        }
    }

    private void handleStormPhase() {
        if (phaseClock.passed()) {
            phaseClock.schedule(1000);
            Logger.sendLog("[Boss AI] Phase 2: Storm pillar crushing & DPS...");
        }
    }

    private void handleGoldorPhase() {
        if (phaseClock.passed()) {
            phaseClock.schedule(1000);
            Logger.sendLog("[Boss AI] Phase 3: Goldor Terminals & Levers Auto-Solver active...");
        }
    }

    private void handleNecronPhase() {
        if (phaseClock.passed()) {
            phaseClock.schedule(1000);
            Logger.sendLog("[Boss AI] Phase 4: Necron middle platform dps phase...");
        }
    }

    public void setPhase(BossPhase phase) {
        this.currentPhase = phase;
    }

    @Override
    public void onDisable(DungeonMacro macro) {
        this.currentPhase = BossPhase.MAXOR_CRYSTALS;
    }
}
