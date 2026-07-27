package com.vertexai.macro.impl.ForagingMacro.states;

import com.vertexai.Vertex;
import com.vertexai.macro.impl.ForagingMacro.ForagingMacro;
import com.vertexai.macro.impl.ForagingMacro.ForagingMacroState;
import com.vertexai.feature.impl.Pathfinder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PathfindingState implements ForagingMacroState {

    private final Minecraft mc = Minecraft.getInstance();
    private BlockPos targetBlock;

    @Override
    public void onStart(ForagingMacro macro) {
        log("Searching for closest log block...");
        this.targetBlock = findClosestLogBlock(macro.getCurrentForagingMode());
        if (this.targetBlock != null) {
            macro.setTargetBlockPos(this.targetBlock);
            log("Found target log at " + this.targetBlock.toShortString());
            Pathfinder.getInstance().stopAndRequeue(this.targetBlock);
            Pathfinder.getInstance().start();
        } else {
            log("No log blocks found nearby.");
        }
    }

    @Override
    public ForagingMacroState onTick(ForagingMacro macro) {
        if (this.targetBlock == null) {
            // Search again next tick if nothing found
            this.targetBlock = findClosestLogBlock(macro.getCurrentForagingMode());
            if (this.targetBlock != null) {
                macro.setTargetBlockPos(this.targetBlock);
                Pathfinder.getInstance().stopAndRequeue(this.targetBlock);
                Pathfinder.getInstance().start();
            }
            return this;
        }

        if (!Pathfinder.getInstance().isRunning() && Pathfinder.getInstance().failed()) {
            log("Pathfinding failed, searching for a new block...");
            this.targetBlock = null;
            return this;
        }

        // Check if we are close enough to break or throw axe
        double distanceSq = mc.player.distanceToSqr(
                this.targetBlock.getX() + 0.5,
                this.targetBlock.getY() + 0.5,
                this.targetBlock.getZ() + 0.5
        );

        if (distanceSq <= 25.0) { // 5 blocks range for axe throwing / breaking
            Pathfinder.getInstance().stop();
            return new BreakingState();
        }

        return this;
    }

    @Override
    public void onEnd(ForagingMacro macro) {
        Pathfinder.getInstance().stop();
    }

    private BlockPos findClosestLogBlock(String mode) {
        BlockPos playerPos = mc.player.blockPosition();
        List<BlockPos> validBlocks = new ArrayList<>();
        int searchRadius = 30;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -10; y <= 20; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    Block block = mc.level.getBlockState(pos).getBlock();
                    
                    boolean isValid = false;
                    if (mode.equals("Mangrove")) {
                        isValid = (block == Blocks.JUNGLE_LOG || block == Blocks.MANGROVE_LOG); 
                    } else if (mode.equals("Lushlilac")) {
                        isValid = (block == Blocks.FLOWERING_AZALEA);
                    } else {
                        isValid = (block == Blocks.OAK_LOG);
                    }
                    
                    if (isValid) {
                        validBlocks.add(pos);
                    }
                }
            }
        }

        return validBlocks.stream()
                .min(Comparator.comparingDouble(pos -> pos.distSqr(playerPos)))
                .orElse(null);
    }
}
