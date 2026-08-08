package com.vertexai.macro.impl.FarmBuilderMacro.states;

import com.vertexai.Vertex;
import com.vertexai.macro.features.navigation.Pathfinder;
import com.vertexai.macro.AbstractMacro;
import com.vertexai.macro.impl.FarmBuilderMacro.config.FarmPatternConfig;
import com.vertexai.macro.impl.FarmBuilderMacro.config.FarmPatternManager;
import com.vertexai.util.AngleUtil;
import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.Logger;
import com.vertexai.util.helper.Clock;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class CarveTurnsState extends FarmBuilderState {

    private final BlockPos startPos;
    private final int totalWidth;
    private final FarmPatternConfig pattern;
    
    private final List<BlockPos> blocksToBreak = new ArrayList<>();
    private int currentTargetIndex = 0;
    
    private final Clock delayClock = new Clock();
    private boolean isBreaking = false;
    private final Pathfinder pathfinder = Pathfinder.getInstance();

    public CarveTurnsState(AbstractMacro macro, BlockPos startPos, int totalWidth) {
        super(macro);
        this.startPos = startPos;
        this.totalWidth = totalWidth;
        
        String patternName = Vertex.config().farmBuilder.patternName;
        this.pattern = FarmPatternManager.patterns.values().stream()
            .filter(p -> p.name.equalsIgnoreCase(patternName) || (patternName.contains("cane") && p.name.toLowerCase().contains("cane")))
            .findFirst()
            .orElse(null);
    }

    @Override
    public String getName() {
        return "CarveTurns";
    }

    @Override
    public void onEnable() {
        Logger.sendMessage("Â§a[FarmBuilder] Commencing S-Shape carving phase...");
        
        if (pattern == null || pattern.columns == null) {
            Logger.sendError("Â§c[FarmBuilder] No valid pattern found for carving! Stopping macro.");
            macro.toggle();
            return;
        }

        // Calculate all target blocks
        for (int xOffset = 0; xOffset <= totalWidth; xOffset++) {
            int colIndex = xOffset % pattern.patternWidth;
            int rep = xOffset / pattern.patternWidth;
            
            FarmPatternConfig.ColumnConfig col = getColumnConfig(colIndex);
            if (col != null && !isWaterColumn(col)) {
                // It's a dirt block, determine if it needs breaking for the snake
                // Odd rep -> break at Z=95, Even rep -> break at Z=0
                int targetZ = (rep % 2 != 0) ? startPos.getZ() + 95 : startPos.getZ();
                blocksToBreak.add(new BlockPos(startPos.getX() + xOffset, startPos.getY() - 1, targetZ));
            }
        }
        
        Logger.sendMessage("Â§a[FarmBuilder] Calculated " + blocksToBreak.size() + " blocks to break.");
        currentTargetIndex = 0;
        delayClock.schedule(1000);
    }

    @Override
    public void onDisable() {
        KeyBindUtil.stopMovement();
        KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);
        if (pathfinder.isRunning()) {
            pathfinder.stop();
        }
    }

    @Override
    public void onTick() {
        if (!delayClock.passed()) return;

        if (currentTargetIndex >= blocksToBreak.size()) {
            Logger.sendMessage("Â§a[FarmBuilder] S-Shape carving complete! Farm is ready!");
            macro.toggle();
            return;
        }

        BlockPos target = blocksToBreak.get(currentTargetIndex);

        if (isBreaking) {
            // Looking at it and breaking
            if (mc.level.getBlockState(target).isAir()) {
                // Block is broken!
                KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);
                isBreaking = false;
                currentTargetIndex++;
                delayClock.schedule(200);
                return;
            }
            
            // Look at block and attack
            mc.player.setYRot(AngleUtil.getRotation(target).getYaw());
            mc.player.setXRot(AngleUtil.getRotation(target).getPitch());
            KeyBindUtil.setKeyBindState(mc.options.keyAttack, true);
            return;
        }

        // We need to pathfind to the target
        double dist = mc.player.blockPosition().distManhattan(target);
        
        if (dist <= 3) {
            // Close enough to reach it
            if (pathfinder.isRunning()) pathfinder.stop();
            isBreaking = true;
            return;
        }

        // Use pathfinder to get close
        if (!pathfinder.isRunning()) {
            // Find a block right next to the target so we don't try to stand inside the block we want to break
            BlockPos standPos = target.offset(0, 1, (target.getZ() > startPos.getZ() ? -1 : 1));
            pathfinder.queue(standPos);
            pathfinder.start();
        } else if (pathfinder.failed()) {
            Logger.sendError("Â§c[FarmBuilder] Pathfinder failed to reach end block. Skipping...");
            pathfinder.stop();
            currentTargetIndex++;
        }
    }
    
    private FarmPatternConfig.ColumnConfig getColumnConfig(int offset) {
        for (FarmPatternConfig.ColumnConfig c : pattern.columns) {
            if (c.offset == offset) return c;
        }
        return null;
    }
    
    private boolean isWaterColumn(FarmPatternConfig.ColumnConfig col) {
        for (String tool : col.tools) {
            if (tool.toLowerCase().contains("prismapump")) return true;
        }
        return false;
    }
}
