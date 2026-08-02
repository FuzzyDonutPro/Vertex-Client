package com.vertexai.failsafe.impl;

import com.vertexai.Vertex;
import com.vertexai.failsafe.AbstractFailsafe;
import com.vertexai.failsafe.reaction.RecordedReactionManager;
import com.vertexai.macro.MacroManager;
import com.vertexai.macro.impl.FarmingMacro.FarmingMacro;
import com.vertexai.macro.impl.FarmingMacro.util.CropEnum;
import com.vertexai.macro.impl.FarmingMacro.util.FarmingUtils;
import com.vertexai.util.Logger;
import com.vertexai.util.helper.Clock;

public class CropChangeFailsafe extends AbstractFailsafe {

    private static final CropChangeFailsafe instance = new CropChangeFailsafe();
    public static CropChangeFailsafe getInstance() { return instance; }

    private CropEnum initialCrop = CropEnum.NONE;
    private final Clock delayTimer = new Clock();

    @Override
    public String getName() {
        return "CropChangeFailsafe";
    }

    @Override
    public Failsafe getFailsafeType() {
        return Failsafe.CROP_CHANGE;
    }

    @Override
    public int getPriority() {
        return 5;
    }

    @Override
    public boolean onTick() {
        if (!Vertex.config().failsafe.enableCropChangeFailsafe) {
            this.resetStates();
            return false;
        }

        boolean isFarming = MacroManager.getInstance().getActiveMacro() instanceof FarmingMacro;
        if (!isFarming || !MacroManager.getInstance().isRunning()) {
            this.resetStates();
            return false;
        }

        CropEnum currentCrop = FarmingUtils.getFarmingCrop();
        if (currentCrop == CropEnum.NONE) return false;

        if (initialCrop == CropEnum.NONE) {
            initialCrop = currentCrop;
            return false;
        }

        if (currentCrop != initialCrop) {
            int delayMs = Vertex.config().failsafe.cropChangeReactionDelay;
            if (delayMs <= 0) delayMs = 2000;

            if (!delayTimer.isScheduled()) {
                delayTimer.schedule(delayMs);
                Logger.sendWarning(String.format("[Failsafe] Crop change detected (%s -> %s)! Delaying reaction by %.1fs...", 
                        initialCrop, currentCrop, delayMs / 1000.0f));
            } else if (delayTimer.passed()) {
                Logger.sendWarning("[Failsafe] Delay expired. Triggering crop change reaction!");
                return true;
            }
        } else {
            if (delayTimer.isScheduled()) {
                delayTimer.reset();
            }
        }

        return false;
    }

    @Override
    public boolean react() {
        MacroManager.getInstance().disable();

        String reactionPreset = Vertex.config().failsafe.cropChangeReactionName;
        if (reactionPreset == null || reactionPreset.trim().isEmpty()) {
            reactionPreset = "default";
        }

        Logger.sendWarning("[Failsafe] Executing reaction preset: " + reactionPreset);
        RecordedReactionManager.getInstance().playReaction(reactionPreset);
        this.resetStates();
        return true;
    }

    @Override
    public void resetStates() {
        this.initialCrop = CropEnum.NONE;
        this.delayTimer.reset();
    }
}
