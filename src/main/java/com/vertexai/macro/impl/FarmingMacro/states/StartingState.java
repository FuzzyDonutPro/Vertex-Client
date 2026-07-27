package com.vertexai.macro.impl.FarmingMacro.states;

import com.vertexai.Vertex;
import com.vertexai.handler.RotationHandler;
import com.vertexai.macro.impl.FarmingMacro.FarmingMacro;
import com.vertexai.macro.impl.FarmingMacro.FarmingMacroState;
import com.vertexai.util.InventoryUtil;
import com.vertexai.util.helper.RotationConfiguration;
import com.vertexai.util.helper.Target;
import com.vertexai.macro.impl.FarmingMacro.util.CropEnum;
import com.vertexai.macro.impl.FarmingMacro.util.FarmingUtils;
import net.minecraft.client.Minecraft;

public class StartingState implements FarmingMacroState {

    private boolean rotating = false;
    private CropEnum currentCrop = CropEnum.NONE;

    @Override
    public void onStart(FarmingMacro macro) {
        log("Initializing S-Shape Farming...");
        
        String tool = Vertex.config().farming.farmingTool;
        if (tool == null || tool.trim().isEmpty()) {
            macro.disable("Please set a Farming Tool in the config!");
            return;
        }

        if (!InventoryUtil.holdItem(tool)) {
            macro.disable("Farming tool not found in hotbar!");
            return;
        }
    }

    @Override
    public FarmingMacroState onTick(FarmingMacro macro) {
        if (!rotating) {
            currentCrop = FarmingUtils.getFarmingCrop();
            
            float targetYaw;
            float targetPitch;
            
            if (currentCrop != CropEnum.NONE) {
                log("Vertex AI: Detected " + currentCrop.name() + " crop!");
                targetYaw = FarmingUtils.getOptimalYaw(currentCrop, Minecraft.getInstance().player.getYRot());
                targetPitch = FarmingUtils.getOptimalPitch(currentCrop);
                log("Vertex AI: Auto-calculated optimal rotation: Yaw=" + targetYaw + ", Pitch=" + targetPitch);
            } else {
                log("Vertex AI: No crop detected! Falling back to current rotation.");
                targetYaw = Minecraft.getInstance().player.getYRot();
                targetPitch = Minecraft.getInstance().player.getXRot();
            }
            
            // Align camera
            RotationHandler.getInstance().easeTo(new RotationConfiguration(
                    new Target(new com.vertexai.util.helper.Angle(targetYaw, targetPitch)),
                    500, // 500ms rotation time
                    null
            ));
            rotating = true;
            return this;
        }

        // Wait for rotation to finish
        if (RotationHandler.getInstance().isEnabled()) {
            return this;
        }

        // Rotation complete, start farming. Default to moving left (true) initially.
        return new FarmingState(true, currentCrop);
    }

    @Override
    public void onEnd(FarmingMacro macro) {
    }
}
