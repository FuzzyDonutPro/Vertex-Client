package com.vertexai.macro.impl.FarmingMacro.states;

import com.vertexai.Vertex;
import com.vertexai.macro.impl.FarmingMacro.FarmingMacro;
import com.vertexai.macro.impl.FarmingMacro.FarmingMacroState;
import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.helper.Clock;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import com.vertexai.macro.impl.FarmingMacro.util.FarmingUtils;
import com.vertexai.macro.impl.FarmingMacro.util.CropEnum;

public class FarmingState implements FarmingMacroState {

    private final boolean movingLeft;
    private final CropEnum crop;
    private final Clock stuckTimer = new Clock();
    private Vec3 lastPosition;

    public FarmingState(boolean movingLeft, CropEnum crop) {
        this.movingLeft = movingLeft;
        this.crop = crop;
    }

    @Override
    public void onStart(FarmingMacro macro) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        lastPosition = mc.player.position();
        stuckTimer.schedule(250); // Need to be stuck for 250ms to switch rows
        
        // Start holding attack
        KeyBindUtil.setKeyBindState(mc.options.keyAttack, true);
    }

    @Override
    public FarmingMacroState onTick(FarmingMacro macro) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;

        // Maintain attack
        KeyBindUtil.setKeyBindState(mc.options.keyAttack, true);

        // Apply strafe direction based on Crop
        if (crop == CropEnum.SUGAR_CANE || crop == CropEnum.MUSHROOM) {
            // FarmHelper cane/mushroom logic: hold Forward (W) to advance and Strafe (A/D) to hug the wall at 45 degrees
            KeyBindUtil.setKeyBindState(mc.options.keyUp, true);
            if (movingLeft) {
                KeyBindUtil.setKeyBindState(mc.options.keyLeft, true);
                KeyBindUtil.setKeyBindState(mc.options.keyRight, false);
            } else {
                KeyBindUtil.setKeyBindState(mc.options.keyRight, true);
                KeyBindUtil.setKeyBindState(mc.options.keyLeft, false);
            }
        } else {
            // Standard S-Shape (Wheat, NetherWart, etc)
            if (movingLeft) {
                KeyBindUtil.setKeyBindState(mc.options.keyLeft, true);
                KeyBindUtil.setKeyBindState(mc.options.keyRight, false);
            } else {
                KeyBindUtil.setKeyBindState(mc.options.keyRight, true);
                KeyBindUtil.setKeyBindState(mc.options.keyLeft, false);
            }
        }

        // Proactive Edge Detection (FarmHelper style)
        Direction lookDir = mc.player.getDirection();
        Direction strafeDir = movingLeft ? lookDir.getCounterClockWise() : lookDir.getClockWise();
        
        BlockPos nextPos = mc.player.blockPosition().relative(strafeDir);
        BlockState nextState = mc.level.getBlockState(nextPos);
        BlockPos nextNextPos = nextPos.relative(strafeDir);
        BlockState nextNextState = mc.level.getBlockState(nextNextPos);
        
        CropEnum currentCrop = FarmingUtils.getFarmingCrop();
        boolean isCropAhead = isCrop(nextState) || isCrop(nextNextState);
        boolean isSolidAhead = nextState.isSolid() || nextNextState.isSolid();
        
        if (!isCropAhead || isSolidAhead) {
            log("Vertex AI: Edge detected! Switching lanes proactively...");
            return new RowSwitchState(!movingLeft, crop);
        }

        // Anti-Stuck Failsafe
        Vec3 currentPos = mc.player.position();
        double horizontalDistanceMoved = Math.sqrt(
                Math.pow(currentPos.x - lastPosition.x, 2) + 
                Math.pow(currentPos.z - lastPosition.z, 2)
        );

        if (horizontalDistanceMoved > 0.05) {
            lastPosition = currentPos;
            stuckTimer.schedule(500); // Increased buffer to avoid false positives
        } else if (stuckTimer.passed()) {
            log("Vertex AI: Anti-Stuck triggered! Force switching rows...");
            return new RowSwitchState(!movingLeft, crop);
        }

        return this;
    }
    
    private boolean isCrop(BlockState state) {
        net.minecraft.world.level.block.Block b = state.getBlock();
        return b == net.minecraft.world.level.block.Blocks.WHEAT ||
               b == net.minecraft.world.level.block.Blocks.CARROTS ||
               b == net.minecraft.world.level.block.Blocks.POTATOES ||
               b == net.minecraft.world.level.block.Blocks.NETHER_WART ||
               b == net.minecraft.world.level.block.Blocks.SUGAR_CANE ||
               b == net.minecraft.world.level.block.Blocks.MELON ||
               b == net.minecraft.world.level.block.Blocks.PUMPKIN ||
               b == net.minecraft.world.level.block.Blocks.RED_MUSHROOM ||
               b == net.minecraft.world.level.block.Blocks.BROWN_MUSHROOM ||
               b == net.minecraft.world.level.block.Blocks.CACTUS ||
               b == net.minecraft.world.level.block.Blocks.COCOA;
    }

    @Override
    public void onEnd(FarmingMacro macro) {
        // Release strafe keys, but don't release attack yet (RowSwitch might want to keep swinging)
        Minecraft mc = Minecraft.getInstance();
        KeyBindUtil.setKeyBindState(mc.options.keyLeft, false);
        KeyBindUtil.setKeyBindState(mc.options.keyRight, false);
        KeyBindUtil.setKeyBindState(mc.options.keyUp, false);
    }
}
