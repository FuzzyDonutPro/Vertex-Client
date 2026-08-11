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
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class PathfindingState implements ForagingMacroState {

    private final Minecraft mc = Minecraft.getInstance();
    private BlockPos targetBlock;
    private final com.vertexai.util.helper.Clock pathTimeout = new com.vertexai.util.helper.Clock();
    private final java.util.Set<BlockPos> blacklistedBlocks = new java.util.HashSet<>();

    private final com.vertexai.util.helper.Clock blacklistClearClock = new com.vertexai.util.helper.Clock();

    @Override
    public void onStart(ForagingMacro macro) {
        log("Searching for closest log block...");
        blacklistedBlocks.clear();
        blacklistClearClock.schedule(15000L);
        startPathfindingToNewTarget(macro);
    }

    @Override
    public ForagingMacroState onTick(ForagingMacro macro) {
        if (mc.player == null || mc.level == null) return this;

        // If no target, try finding one
        if (this.targetBlock == null || mc.level.isEmptyBlock(this.targetBlock)) {
            startPathfindingToNewTarget(macro);
            if (this.targetBlock == null) {
                if (!blacklistedBlocks.isEmpty() && blacklistClearClock.passed()) {
                    log("No logs found. Clearing blacklist to retry...");
                    blacklistedBlocks.clear();
                    blacklistClearClock.schedule(15000L);
                }

                if (!Pathfinder.getInstance().isRunning()) {
                    BlockPos randomWander = mc.player.blockPosition().offset(
                            (int)(Math.random() * 10 - 5),
                            0,
                            (int)(Math.random() * 10 - 5)
                    );
                    Pathfinder.getInstance().stopAndRequeue(randomWander);
                    Pathfinder.getInstance().start();
                }
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
        List<BlockPos> validBlocks = new java.util.ArrayList<>();
        int searchRadius = 30;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -8; y <= 20; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    if (blacklistedBlocks.contains(pos)) continue;

                    net.minecraft.world.level.block.Block block = mc.level.getBlockState(pos).getBlock();
                    if (isLogBlock(block, mode) && isTreeCluster(pos, mode)) {
                        validBlocks.add(pos);
                    }
                }
            }
        }

        BlockPos closest = validBlocks.stream()
                .min(java.util.Comparator.<BlockPos>comparingDouble(pos -> Vec3.atCenterOf(pos).distanceToSqr(eyePos)))
                .orElse(null);

        if (closest != null) {
            // Traverse down to find the bottom-most log of this tree
            BlockPos current = closest;
            while (true) {
                BlockPos below = current.below();
                if (blacklistedBlocks.contains(below)) break;
                if (!isLogBlock(mc.level.getBlockState(below).getBlock(), mode)) {
                    break;
                }
                current = below;
            }
            return current;
        }

        return null;
    }

    private boolean isTreeCluster(BlockPos startPos, String mode) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        queue.add(startPos);
        visited.add(startPos);

        int count = 0;

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            count++;

            if (count >= 10) {
                return true; // Cluster has at least 10 connected logs (real tree!)
            }

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos neighbor = current.offset(dx, dy, dz);
                        if (!visited.contains(neighbor) && !blacklistedBlocks.contains(neighbor)) {
                            Block neighborBlock = mc.level.getBlockState(neighbor).getBlock();
                            if (isLogBlock(neighborBlock, mode)) {
                                visited.add(neighbor);
                                queue.add(neighbor);
                            }
                        }
                    }
                }
            }
        }

        return count >= 10;
    }

    private boolean isLogBlock(Block block, String mode) {
        if (block == null) return false;
        String id = block.getDescriptionId().toLowerCase(java.util.Locale.ROOT);
        
        // Exclude stripped logs and non-log wooden structures (planks, fences, stairs, slabs, trapdoors, doors, signs, etc.)
        if (id.contains("stripped") || id.contains("planks") || id.contains("fence") || 
            id.contains("stairs") || id.contains("slab") || id.contains("door") || 
            id.contains("sign") || id.contains("plate") || id.contains("button") || 
            id.contains("gate") || id.contains("table") || id.contains("chest") || 
            id.contains("barrel") || id.contains("composter") || id.contains("boat")) {
            return false;
        }

        // Must explicitly be a log or wood block
        boolean isLogOrWood = id.contains("log") || id.contains("wood");
        if (!isLogOrWood) return false;

        String m = mode != null ? mode.toLowerCase(java.util.Locale.ROOT) : "";

        if (m.contains("dark")) {
            return id.contains("dark_oak");
        } else if (m.contains("acacia")) {
            return id.contains("acacia");
        } else if (m.contains("jungle") || m.contains("mangrove")) {
            return id.contains("jungle") || id.contains("mangrove");
        } else if (m.contains("spruce")) {
            return id.contains("spruce");
        } else if (m.contains("oak")) {
            return id.contains("oak") && !id.contains("dark");
        } else if (m.contains("birch")) {
            return id.contains("birch");
        }
        return true;
    }
}
