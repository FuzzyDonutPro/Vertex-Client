package com.vertexai.feature.impl.BlockMiner.states;

import com.vertexai.feature.impl.BlockMiner.BlockMiner;
import com.vertexai.util.BlockUtil;
import com.vertexai.util.helper.Clock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * ChoosingBlockState
 * <p>
 * State responsible for finding the next block to mine.
 * Uses priority settings to determine the best block to target.
 * Includes wait logic if no blocks are immediately available.
 */
public class ChoosingBlockState implements BlockMinerState {
    private final Clock timer = new Clock();

    @Override
    public void onStart(BlockMiner blockMiner) {
        log("Entering Choosing Block State");
        timer.reset();
    }

    @Override
    public BlockMinerState onTick(BlockMiner blockMiner) {
        // Try to find mineable blocks around the player based on priorities
        List<BlockPos> blocks = BlockUtil.findMineableBlocksFromAccessiblePositions(
                blockMiner.getBlockPriority(),
                blockMiner.getTargetBlockPos(),
                blockMiner.getMiningSpeed()
        );

        // Handle case where no blocks are found
        if (blocks.isEmpty()) {
            if (!timer.isScheduled()) {
                log("No blocks found, fast re-scanning every 100ms...");
                timer.schedule(3000L); // 3 second total search window before declaring area empty
            }

            // Retry scan every tick, if timer passed stop mining
            if (timer.isScheduled() && timer.passed()) {
                logError("No blocks found after 3000ms scan window, stopping miner");
                blockMiner.stop();
                blockMiner.setError(BlockMiner.BlockMinerError.NOT_ENOUGH_BLOCKS);
                return null;
            }

            return this;
        }

        // Found blocks - select the best one (first in list) and transition to breaking
        blockMiner.setTargetBlockPos(blocks.get(0));
        blockMiner.setTargetBlockType(Minecraft.getInstance().level.getBlockState(blocks.get(0)).getBlock());
        blockMiner.setBlockChanged(false);
        log("Found " + blocks.size() + " blocks, selecting " + blocks.get(0) + " (" + blockMiner.getTargetBlockType() + ")");
        return new BreakingState();
    }

    @Override
    public void onEnd(BlockMiner blockMiner) {
        log("Exiting Choosing Block State");
    }
}
