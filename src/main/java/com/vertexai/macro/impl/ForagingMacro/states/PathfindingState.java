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

        // If pathfinder is actively executing a path, let it finish pathfinding completely before rotating to mine
        if (Pathfinder.getInstance().isRunning()) {
            // If target log was broken or blacklisted while moving, abort and replan
            if (this.targetBlock != null && (mc.level.isEmptyBlock(this.targetBlock) || macro.isBlockBlacklisted(this.targetBlock))) {
                Pathfinder.getInstance().stop();
                this.targetBlock = null;
                return this;
            }

            // Timeout check while running
            if (pathTimeout.isScheduled() && pathTimeout.passed()) {
                if (this.targetBlock != null) {
                    log("Pathfinding to log " + this.targetBlock.toShortString() + " timed out, blacklisting tree...");
                    macro.blacklistTreeCluster(this.targetBlock);
                    this.targetBlock = null;
                }
                Pathfinder.getInstance().stop();
                return this;
            }

            // Do not interrupt pathfinding prematurely; complete the route first
            return this;
        }

        // Pathfinder has finished / is not running.
        // Check if our target log is now within mining reach
        Vec3 eyePos = mc.player.getEyePosition();
        if (this.targetBlock != null && !mc.level.isEmptyBlock(this.targetBlock) && !macro.isBlockBlacklisted(this.targetBlock)) {
            Vec3 targetCenter = Vec3.atCenterOf(this.targetBlock);
            double distanceSq = eyePos.distanceToSqr(targetCenter);

            if (distanceSq <= 20.25) {
                // Pathfinding complete and target is in reach (<= 4.5 blocks)! Transition to BreakingState to rotate and mine.
                macro.setTargetBlockPos(this.targetBlock);
                return new BreakingState();
            } else {
                // Path ended but player is out of reach or path failed
                log("Pathfinding to log " + this.targetBlock.toShortString() + " finished but out of reach (" + String.format("%.1f", Math.sqrt(distanceSq)) + " blocks), blacklisting tree...");
                macro.blacklistTreeCluster(this.targetBlock);
                this.targetBlock = null;
            }
        }

        // Check if ANY valid unmined tree log of target mode is already within reach (<= 4.2 blocks) before launching a new path
        BlockPos immediateLog = findImmediateReachableLog(macro, macro.getCurrentForagingMode());
        if (immediateLog != null) {
            macro.setTargetBlockPos(immediateLog);
            this.targetBlock = immediateLog;
            return new BreakingState();
        }

        // If no target or target broken/blacklisted, search for a new tree and start pathfinding
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
                    BlockPos standable = findStandablePosNear(macro, randomWander, macro.getCurrentForagingMode());
                    Pathfinder.getInstance().stopAndRequeue(standable);
                    Pathfinder.getInstance().start();
                }
                return this;
            }
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

            // Pathfind to adjacent standable ground position with direct, unobstructed sightline
            BlockPos standablePos = findStandablePosNear(macro, this.targetBlock, macro.getCurrentForagingMode());
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
                            if (BlockUtil.hasVisibleSide(eyePos, pos)) {
                                double heightPenalty = Math.abs((pos.getY() + 0.5) - eyePos.y) * 1.5;
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
        }
        return best;
    }

    private BlockPos findStandablePosNear(ForagingMacro macro, BlockPos logPos, String mode) {
        if (mc.level == null || mc.player == null) return logPos;
        BlockPos playerPos = mc.player.blockPosition();
        BlockStateAccessor bsa = new BlockStateAccessor(mc.level);

        BlockPos bestVisiblePos = null;
        double bestVisibleScore = Double.MAX_VALUE;

        BlockPos fallbackPos = null;
        double fallbackDist = Double.MAX_VALUE;

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

                        if (distToLog <= 18) {
                            Vec3 simulatedEye = new Vec3(standPos.getX() + 0.5, standPos.getY() + 1.62, standPos.getZ() + 0.5);

                            // Check if ANY log in this tree cluster has direct, unobstructed line-of-sight from this angle
                            boolean hasLineOfSight = false;
                            for (int lx = -2; lx <= 2; lx++) {
                                for (int ly = -1; ly <= 4; ly++) {
                                    for (int lz = -2; lz <= 2; lz++) {
                                        BlockPos candidate = logPos.offset(lx, ly, lz);
                                        if (macro.isBlockBlacklisted(candidate)) continue;

                                        Block b = mc.level.getBlockState(candidate).getBlock();
                                        if (ForagingMacro.isLogBlock(b, mode) && BlockUtil.hasVisibleSide(simulatedEye, candidate)) {
                                            hasLineOfSight = true;
                                            break;
                                        }
                                    }
                                    if (hasLineOfSight) break;
                                }
                                if (hasLineOfSight) break;
                            }

                            if (hasLineOfSight) {
                                double score = distFromPlayer + (distToLog * 0.5);
                                if (score < bestVisibleScore) {
                                    bestVisibleScore = score;
                                    bestVisiblePos = standPos;
                                }
                            } else {
                                if (distFromPlayer < fallbackDist) {
                                    fallbackDist = distFromPlayer;
                                    fallbackPos = standPos;
                                }
                            }
                        }
                    }
                }
            }
        }
        return bestVisiblePos != null ? bestVisiblePos : (fallbackPos != null ? fallbackPos : logPos);
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
