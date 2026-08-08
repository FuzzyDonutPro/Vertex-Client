package com.vertexai.macro.features.mining.BlockMiner.states;

import com.vertexai.macro.features.mining.BlockMiner.BlockMiner;

/**
 * ArriveState
 * <p>
 * Legacy passthrough state for backward compatibility. Immediately transitions to BreakingState.
 */
public class ArriveState implements BlockMinerState {
    @Override
    public void onStart(BlockMiner miner) {}

    @Override
    public BlockMinerState onTick(BlockMiner miner) {
        return new BreakingState();
    }

    @Override
    public void onEnd(BlockMiner miner) {}
}
