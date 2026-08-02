package com.vertexai.feature.impl;

import lombok.Getter;
import com.vertexai.feature.AbstractFeature;
import com.vertexai.util.Logger;
import com.vertexai.util.helper.Clock;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ExperimentationSolver extends AbstractFeature {

    @Getter
    public static final ExperimentationSolver instance = new ExperimentationSolver();

    public enum GameType { NONE, CHRONOMATRON, ULTRASEQUENCE, SUPERPAIRS }

    private GameType currentGame = GameType.NONE;
    private final List<Integer> sequence = new ArrayList<>();
    private int currentClickIndex = 0;
    private final Clock clickClock = new Clock();
    private boolean autoClickEnabled = true;

    public ExperimentationSolver() {
        this.enabled = true;
    }

    @Override
    public String getName() {
        return "ExperimentationSolver";
    }

    @Override
    protected void onTick() {
        if (mc.player == null || mc.level == null) return;
        if (!(mc.screen instanceof AbstractContainerScreen<?> containerScreen)) {
            reset();
            return;
        }

        String title = containerScreen.getTitle().getString();
        detectGameType(title);

        if (currentGame == GameType.NONE) return;

        if (autoClickEnabled && !sequence.isEmpty() && currentClickIndex < sequence.size()) {
            if (clickClock.isScheduled() && !clickClock.passed()) return;

            int targetSlotId = sequence.get(currentClickIndex);
            Slot targetSlot = containerScreen.getMenu().getSlot(targetSlotId);

            if (targetSlot != null && !targetSlot.getItem().isEmpty()) {
                mc.gameMode.handleInventoryMouseClick(
                        containerScreen.getMenu().containerId,
                        targetSlotId,
                        0,
                        ClickType.PICKUP,
                        mc.player
                );
                currentClickIndex++;
                clickClock.schedule(250 + (int)(Math.random() * 150)); // Humanized delay 250-400ms
            }
        }
    }

    private void detectGameType(String title) {
        if (title.contains("Chronomatron")) {
            if (currentGame != GameType.CHRONOMATRON) {
                currentGame = GameType.CHRONOMATRON;
                Logger.sendMessage("§a[Solver] Chronomatron Solver Active!");
            }
        } else if (title.contains("Ultrasequence")) {
            if (currentGame != GameType.ULTRASEQUENCE) {
                currentGame = GameType.ULTRASEQUENCE;
                Logger.sendMessage("§a[Solver] Ultrasequence Solver Active!");
            }
        } else if (title.contains("Superpairs")) {
            if (currentGame != GameType.SUPERPAIRS) {
                currentGame = GameType.SUPERPAIRS;
                Logger.sendMessage("§a[Solver] Superpairs Solver Active!");
            }
        }
    }

    public void addSequenceSlot(int slotId) {
        sequence.add(slotId);
    }

    public void reset() {
        currentGame = GameType.NONE;
        sequence.clear();
        currentClickIndex = 0;
        clickClock.reset();
    }
}
