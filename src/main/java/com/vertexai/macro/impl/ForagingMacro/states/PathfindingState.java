package com.vertexai.macro.impl.ForagingMacro.states;

import com.vertexai.macro.impl.ForagingMacro.ForagingMacro;
import com.vertexai.macro.impl.ForagingMacro.ForagingMacroState;
import com.vertexai.feature.impl.Pathfinder;
import com.vertexai.pathfinder.helper.BlockStateAccessor;
import com.vertexai.pathfinder.movement.MovementHelper;
import com.vertexai.util.BlockUtil;
import com.vertexai.util.helper.Clock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class PathfindingState implements ForagingMacroState {

    private final Minecraft mc = Minecraft.getInstance();
    private BlockPos targetBlock;
    private final Clock pathTimeout = new Clock();
    private final Clock wanderCooldown = new Clock();

    @Override
    public void onStart(ForagingMacro macro) {
        log("Searching for closest unmined tree at head height...");
        startPathfindingToNewTarget(macro);
    }

    @Override
    public ForagingMacroState onTick(ForagingMacro macro) {
        if (mc.player == null || mc.level == null) return this;

        // Check if ANY valid unmined tree log of target mode is already within reach (<= 4.2 blocks)
        BlockPos immediateLog = findImmediateReachableLog(macro, macro.getCurrentForagingMode());
        if (immediateLog != null) {
            macro.setTargetBlockPos(immediateLog);
            this.targetBlock = immediateLog;
            Pathfinder.getInstance().stop();
            return new BreakingState();
        }

        // If no target or target already broken/blacklisted, find a new one
        if (this.targetBlock == null || mc.level.isEmptyBlock(this.targetBlock) || macro.isBlockBlacklisted(this.targetBlock)) {
            startPathfindingToNewTarget(macro);
            if (this.targetBlock == null) {
                if (!macro.isBlacklistEmpty() && macro.blacklistClearClock.passed()) {
                    log("No fresh trees found. Clearing blacklist to scan for respawned trees...");
                    macro.clearBlacklist();
                    macro.blacklistClearClock.schedule(10000L);
                }

                // Throttle wander requests to prevent thread starvation and path spam
                if (!Pathfinder.getInstance().isRunning() && (!wanderCooldown.isScheduled() || wanderCooldown.passed())) {
                    wanderCooldown.schedule(2500L);
                    BlockPos randomWander = mc.player.blockPosition().offset(
                            (int)(Math.random() * 14 - 7),
                            0,
                            (int)(Math.random() * 14 - 7)
                    );
                    BlockPos standable = findStandablePosNear(randomWander);
                    Pathfinder.getInstance().stopAndRequeue(standable);
                    Pathfinder.getInstance().start();
                }
                return this;
            }
        }

        // Measure distance from eye position to target log center
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 targetCenter = Vec3.atCenterOf(this.targetBlock);
        double distanceSq = eyePos.distanceToSqr(targetCenter);

        // Within reach limit (4.5 blocks = 20.25 sq blocks)
        if (distanceSq <= 20.25) {
            Pathfinder.getInstance().stop();
            return new BreakingState();
        }

        // Check if pathfinder stopped or failed
        if (!Pathfinder.getInstance().isRunning()) {
            if (Pathfinder.getInstance().failed() || (pathTimeout.isScheduled() && pathTimeout.passed()) || distanceSq > 20.25) {
                log("Pathfinding to log " + this.targetBlock.toShortString() + " finished but out of reach / failed, blacklisting tree...");
                macro.blacklistTreeCluster(this.targetBlock);
                this.targetBlock = null;
                Pathfinder.getInstance().stop();
                return this;
            }
        }

        // Timeout check while running
        if (pathTimeout.isScheduled() && pathTimeout.passed()) {
            log("Pathfinding to log " + this.targetBlock.toShortString() + " timed out, blacklisting tree...");
            macro.blacklistTreeCluster(this.targetBlock);
            this.targetBlock = null;
            Pathfinder.getInstance().stop();
            return this;
        }

        return this;
    }

    @Override
    public void onEnd(ForagingMacro macro) {
        Pathfinder.getInstance().stop();
    }

    private void startPathfindingToNewTarget(ForagingMacro macro) {
        this.targetBlock = findClosestLogBlock(macro, macro.getCurrentForagingMode());
        if (this.targetBlock != null) {
            macro.setTargetBlockPos(this.targetBlock);
            log("Found target log at " + this.targetBlock.toShortString());

            Vec3 eyePos = mc.player.getEyePosition();
            Vec3 targetCenter = Vec3.atCenterOf(this.targetBlock);
            double distanceSq = eyePos.distanceToSqr(targetCenter);

            if (distanceSq <= 20.25) {
                // Already in reach, no need to pathfind
                return;
            }

            // Pathfind to adjacent standable ground position
            BlockPos standablePos = findStandablePosNear(this.targetBlock);
            Pathfinder.getInstance().stopAndRequeue(standablePos);
            Pathfinder.getInstance().start();
            pathTimeout.schedule(8000L); // 8 second max pathfinding timeout
        } else {
            log("No unmined tree logs found nearby.");
        }
    }

    private BlockPos findImmediateReachableLog(ForagingMacro macro, String mode) {
        if (mc.player == null || mc.level == null) return null;
        Vec3 eyePos = mc.player.getEyePosition();
        BlockPos playerPos = mc.player.blockPosition();

        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -2; dy <= 4; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    BlockPos pos = playerPos.offset(dx, dy, dz);
                    if (macro.isBlockBlacklisted(pos)) continue;

                    Block block = mc.level.getBlockState(pos).getBlock();
                    if (ForagingMacro.isLogBlock(block, mode) && ForagingMacro.isFullTree(mc.level, pos, mode)) {
                        double d = Vec3.atCenterOf(pos).distanceToSqr(eyePos);
                        if (d <= 18.0) { // <= 4.2 blocks reach
                            double heightPenalty = Math.abs((pos.getY() + 0.5) - eyePos.y) * 2.0;
                            double score = d + heightPenalty;
                            if (score < bestDist) {
                                bestDist = score;
                                best = pos;
                            }
                        }
                    }
                }
            }
        }
        return best;
    }

    private BlockPos findStandablePosNear(BlockPos logPos) {
        if (mc.level == null || mc.player == null) return logPos;
        BlockPos playerPos = mc.player.blockPosition();
        BlockStateAccessor bsa = new BlockStateAccessor(mc.level);

        BlockPos bestPos = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    int x = logPos.getX() + dx;
                    int y = logPos.getY() + dy;
                    int z = logPos.getZ() + dz;

                    if (MovementHelper.INSTANCE.canStandOn(bsa, x, y, z, bsa.get(x, y, z)) &&
                        MovementHelper.INSTANCE.canWalkThrough(bsa, x, y + 1, z, bsa.get(x, y + 1, z)) &&
                        MovementHelper.INSTANCE.canWalkThrough(bsa, x, y + 2, z, bsa.get(x, y + 2, z))) {

                        BlockPos standPos = new BlockPos(x, y + 1, z);
                        double distFromPlayer = standPos.distSqr(playerPos);
                        double distToLog = standPos.distSqr(logPos);

                        if (distToLog <= 16 && distFromPlayer < bestDist) {
                            bestDist = distFromPlayer;
                            bestPos = standPos;
                        }
                    }
                }
            }
        }
        return bestPos != null ? bestPos : logPos;
    }

    private BlockPos findClosestLogBlock(ForagingMacro macro, String mode) {
        if (mc.player == null || mc.level == null) return null;
        Vec3 eyePos = mc.player.getEyePosition();
        BlockPos playerPos = mc.player.blockPosition();
        List<BlockPos> validBlocks = new ArrayList<>();
        int searchRadius = 32;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -10; y <= 24; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    if (macro.isBlockBlacklisted(pos)) continue;

                    Block block = mc.level.getBlockState(pos).getBlock();
                    if (ForagingMacro.isLogBlock(block, mode) && ForagingMacro.isFullTree(mc.level, pos, mode)) {
                        validBlocks.add(pos);
                    }
                }
            }
        }

        // Target the closest tree, prioritizing logs directly at eye / head height
        return validBlocks.stream()
                .min(Comparator.comparingDouble(pos -> {
                    double dist2D = Math.hypot(pos.getX() + 0.5 - eyePos.x, pos.getZ() + 0.5 - eyePos.z);
                    double headHeightDiff = Math.abs((pos.getY() + 0.5) - eyePos.y);
                    return dist2D + (headHeightDiff * 1.5);
                }))
                .orElse(null);
    }
}
