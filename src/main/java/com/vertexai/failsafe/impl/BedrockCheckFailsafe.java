package com.vertexai.failsafe.impl;

import lombok.Getter;
import com.vertexai.failsafe.AbstractFailsafe;
import com.vertexai.macro.MacroManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class BedrockCheckFailsafe extends AbstractFailsafe {

    public static final BedrockCheckFailsafe instance = new BedrockCheckFailsafe();
    public static BedrockCheckFailsafe getInstance() { return instance; }
    private static final int CHECK_RADIUS = 5;
    private static final int BEDROCK_THRESHOLD = 10;

    @Override
    public String getName() {
        return "BedrockCheckFailsafe";
    }

    @Override
    public Failsafe getFailsafeType() {
        return Failsafe.BEDROCK_CHECK;
    }

    @Override
    public int getPriority() {
        return 6;
    }


    @Override
    public boolean onTick() {
        if (!com.vertexai.Vertex.config().failsafe.enableBedrockFailsafe) return false;
        if (mc.player == null) return false;
        return checkForEnclosure(mc.player.position());
    }

    public boolean checkForEnclosure(Vec3 playerPos) {
        int bedrockCount = 0;
        int dirtCount = 0;
        boolean isFarming = MacroManager.getInstance().getActiveMacro() instanceof com.vertexai.macro.impl.FarmingMacro.FarmingMacro;

        for (int x = -CHECK_RADIUS; x <= CHECK_RADIUS; x++) {
            for (int y = -CHECK_RADIUS; y <= CHECK_RADIUS; y++) {
                for (int z = -CHECK_RADIUS; z <= CHECK_RADIUS; z++) {
                    BlockPos blockPos = new BlockPos(
                            (int) (playerPos.x + x),
                            (int) (playerPos.y + y),
                            (int) (playerPos.z + z)
                    );
                    Block block = mc.level.getBlockState(blockPos).getBlock();

                    if (block == Blocks.BEDROCK) {
                        bedrockCount++;
                    }
                    if (bedrockCount >= BEDROCK_THRESHOLD) return true;

                    // Dirt box check for Farming (Admins use dirt boxes to check farmers)
                    // We only count dirt at or above player's feet (y >= 0 relative) to avoid counting the farm floor.
                    if (isFarming && block == Blocks.DIRT && y >= 0) {
                        dirtCount++;
                    }
                    // A full box around a player takes ~26 blocks. 15 is a safe threshold.
                    if (dirtCount >= 15) return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean react() {
        MacroManager.getInstance().disable();
        warn("Disabling macro due to enclosure (Bedrock/Dirt box).");
        return true;
    }

}

