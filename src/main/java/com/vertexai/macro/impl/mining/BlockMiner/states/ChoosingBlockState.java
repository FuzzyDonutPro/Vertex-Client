package com.vertexai.macro.impl.mining.BlockMiner.states;

import com.vertexai.macro.impl.mining.BlockMiner.BlockMiner;
import com.vertexai.util.BlockUtil;
import com.vertexai.util.helper.Clock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * ChoosingBlockState
 * <p>
 * Scans for accessible mineable blocks around the player based on block priorities.
 * Transitions directly to AimState as soon as a target is selected.
 */
public class ChoosingBlockState implements BlockMinerState {

    private final Clock searchTimer = new Clock();

    @Override
    public void onStart(BlockMiner miner) {
        log("Entering ChoosingBlockState");
        searchTimer.reset();
        miner.setTargetBlockPos(null);
        miner.setTargetPoint(null);
        miner.setMiningDirection(null);
        miner.setBlockChanged(false);
    }

    @Override
    public BlockMinerState onTick(BlockMiner miner) {
        if (miner.getBlockPriority().isEmpty()) {
            logError("No block priorities defined, stopping miner");
            miner.stop();
            miner.setError(BlockMiner.BlockMinerError.NO_TARGET_BLOCKS);
            return null;
        }

        List<BlockPos> blocks = BlockUtil.findMineableBlocksFromAccessiblePositions(
                miner.getBlockPriority(),
                miner.getTargetBlockPos(),
                miner.getMiningSpeed()
        );

        if (blocks.isEmpty()) {
            if (!searchTimer.isScheduled()) {
                log("Scanning area for target blocks...");
                searchTimer.schedule(3000L);
            }

            if (searchTimer.isScheduled() && searchTimer.passed()) {
                logError("No target blocks found in 3000ms window, stopping miner");
                miner.stop();
                miner.setError(BlockMiner.BlockMinerError.NOT_ENOUGH_BLOCKS);
                return null;
            }
            return this;
        }

        BlockPos selected = blocks.get(0);
        miner.setTargetBlockPos(selected);
        if (Minecraft.getInstance().level != null) {
            miner.setTargetBlockType(Minecraft.getInstance().level.getBlockState(selected).getBlock());
        }
        miner.setBlockChanged(false);

        log("Selected target block: " + selected + " (" + miner.getTargetBlockType() + ")");
        return new AimState();
    }

    @Override
    public void onEnd(BlockMiner miner) {
        log("Exiting ChoosingBlockState");
    }
}
