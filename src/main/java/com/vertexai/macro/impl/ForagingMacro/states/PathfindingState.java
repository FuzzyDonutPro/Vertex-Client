package com.vertexai.macro.impl.ForagingMacro.states;

import com.vertexai.Vertex;
import com.vertexai.macro.impl.ForagingMacro.ForagingMacro;
import com.vertexai.macro.impl.ForagingMacro.ForagingMacroState;
import com.vertexai.feature.impl.Pathfinder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PathfindingState implements ForagingMacroState {

    private final Minecraft mc = Minecraft.getInstance();
    private BlockPos targetBlock;
    private final com.vertexai.util.helper.Clock pathTimeout = new com.vertexai.util.helper.Clock();
    private final java.util.Set<BlockPos> blacklistedBlocks = new java.util.HashSet<>();

    @Override
    public void onStart(ForagingMacro macro) {
        log("Searching for closest log block...");
        blacklistedBlocks.clear();
        startPathfindingToNewTarget(macro);
    }

    @Override
    public ForagingMacroState onTick(ForagingMacro macro) {
        if (mc.player == null || mc.level == null) return this;

        // If no target, try finding one
        if (this.targetBlock == null || mc.level.isEmptyBlock(this.targetBlock)) {
            startPathfindingToNewTarget(macro);
            if (this.targetBlock == null) {
                return this; // No logs in area
            }
        }

        // Check if pathfinder failed or timed out
        if ((!Pathfinder.getInstance().isRunning() && Pathfinder.getInstance().failed()) || (pathTimeout.isScheduled() && pathTimeout.passed())) {
            log("Pathfinding to log " + this.targetBlock.toShortString() + " failed/timed out, blacklisting...");
            blacklistedBlocks.add(this.targetBlock);
            this.targetBlock = null;
            Pathfinder.getInstance().stop();
            return this;
        }

        // Measure distance from eye position to target log center
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 targetCenter = Vec3.atCenterOf(this.targetBlock);
        double distanceSq = eyePos.distanceToSqr(targetCenter);

        // Vanilla block reach limit is 4.5 blocks (4.5^2 = 20.25 sq blocks)
        if (distanceSq <= 20.25) {
            Pathfinder.getInstance().stop();
            return new BreakingState();
        }

        return this;
    }

    @Override
    public void onEnd(ForagingMacro macro) {
        Pathfinder.getInstance().stop();
    }

    private void startPathfindingToNewTarget(ForagingMacro macro) {
        this.targetBlock = findClosestLogBlock(macro.getCurrentForagingMode());
        if (this.targetBlock != null) {
            macro.setTargetBlockPos(this.targetBlock);
            log("Found target log at " + this.targetBlock.toShortString());

            // Pathfind to adjacent standable ground position instead of inside solid log
            BlockPos standablePos = findStandablePosNear(this.targetBlock);
            Pathfinder.getInstance().stopAndRequeue(standablePos);
            Pathfinder.getInstance().start();
            pathTimeout.schedule(10000L); // 10 second max pathfinding timeout
        } else {
            log("No log blocks found nearby.");
        }
    }

    private BlockPos findStandablePosNear(BlockPos logPos) {
        if (mc.level == null || mc.player == null) return logPos;
        BlockPos playerPos = mc.player.blockPosition();

        BlockPos bestPos = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos pos = logPos.offset(dx, dy, dz);
                    if (mc.level.getBlockState(pos).isAir() &&
                        mc.level.getBlockState(pos.above()).isAir() &&
                        !mc.level.getBlockState(pos.below()).isAir()) {

                        double d = pos.distSqr(playerPos);
                        if (d < bestDist) {
                            bestDist = d;
                            bestPos = pos;
                        }
                    }
                }
            }
        }
        return bestPos != null ? bestPos : logPos;
    }

    private BlockPos findClosestLogBlock(String mode) {
        if (mc.player == null || mc.level == null) return null;
        Vec3 eyePos = mc.player.getEyePosition();
        BlockPos playerPos = mc.player.blockPosition();
        List<BlockPos> validBlocks = new ArrayList<>();
        int searchRadius = 30;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -8; y <= 20; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    if (blacklistedBlocks.contains(pos)) continue;

                    Block block = mc.level.getBlockState(pos).getBlock();
                    if (isLogBlock(block, mode)) {
                        validBlocks.add(pos);
                    }
                }
            }
        }

        return validBlocks.stream()
                .min(Comparator.comparingDouble(pos -> Vec3.atCenterOf(pos).distanceToSqr(eyePos)))
                .orElse(null);
    }

    private boolean isLogBlock(Block block, String mode) {
        if (block == null) return false;
        String name = block.getDescriptionId().toLowerCase(java.util.Locale.ROOT);
        if (name.contains("stripped")) return false; // Exclude stripped logs as requested
        String m = mode != null ? mode.toLowerCase(java.util.Locale.ROOT) : "";

        if (m.contains("dark")) {
            return name.contains("dark_oak") && (name.contains("log") || name.contains("wood"));
        } else if (m.contains("acacia")) {
            return name.contains("acacia") && (name.contains("log") || name.contains("wood"));
        } else if (m.contains("jungle") || m.contains("mangrove")) {
            return (name.contains("jungle") || name.contains("mangrove")) && (name.contains("log") || name.contains("wood"));
        } else if (m.contains("spruce")) {
            return name.contains("spruce") && (name.contains("log") || name.contains("wood"));
        } else if (m.contains("oak")) {
            return name.contains("oak") && !name.contains("dark") && (name.contains("log") || name.contains("wood"));
        } else if (m.contains("birch")) {
            return name.contains("birch") && (name.contains("log") || name.contains("wood"));
        }
        return name.contains("log") || name.contains("wood");
    }
}
