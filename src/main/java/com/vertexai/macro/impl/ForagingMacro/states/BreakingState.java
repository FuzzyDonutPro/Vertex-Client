package com.vertexai.macro.impl.ForagingMacro.states;

import com.vertexai.macro.impl.ForagingMacro.ForagingMacro;
import com.vertexai.macro.impl.ForagingMacro.ForagingMacroState;
import com.vertexai.handler.RotationHandler;
import com.vertexai.feature.impl.Pathfinder;
import com.vertexai.util.BlockUtil;
import com.vertexai.util.InventoryUtil;
import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.helper.Clock;
import com.vertexai.util.helper.RotationConfiguration;
import com.vertexai.util.helper.Target;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public class BreakingState implements ForagingMacroState {

    private final Minecraft mc = Minecraft.getInstance();
    private final Clock breakDelay = new Clock();
    private final Clock postBreakTimer = new Clock();
    private boolean logBroken = false;
    private BlockPos currentTargetPos;

    @Override
    public void onStart(ForagingMacro macro) {
        log("Aiming and breaking tree log at head height...");
        Pathfinder.getInstance().stop();
        breakDelay.schedule(3500L);
        postBreakTimer.reset();
        logBroken = false;
        KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);

        currentTargetPos = findBestVisibleLog(macro.getTargetBlockPos(), macro.getCurrentForagingMode());
        macro.setTargetBlockPos(currentTargetPos);
        if (currentTargetPos != null && mc.player != null) {
            aimAtBlock(currentTargetPos);
        }
    }

    private void aimAtBlock(BlockPos pos) {
        List<Vec3> points = BlockUtil.bestPointsOnBestSide(pos);
        Vec3 aimPoint = (points != null && !points.isEmpty()) ? points.get(0) : Vec3.atCenterOf(pos);
        RotationHandler.getInstance().stop();
        RotationHandler.getInstance().easeTo(new RotationConfiguration(
                new Target(aimPoint),
                com.vertexai.Vertex.config().getRandomRotationTime(),
                null
        ));
    }

    private BlockPos findBestVisibleLog(BlockPos targetPos, String mode) {
        if (mc.level == null || mc.player == null) return targetPos;
        Vec3 eyePos = mc.player.getEyePosition();

        // 1. If targetPos is directly visible and not empty, prioritize it
        if (targetPos != null && !mc.level.isEmptyBlock(targetPos) && BlockUtil.hasVisibleSide(eyePos, targetPos)) {
            return targetPos;
        }

        // 2. Scan all logs in tree cluster within 4.5 blocks reach
        BlockPos playerPos = mc.player.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -2; dy <= 4; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    BlockPos pos = playerPos.offset(dx, dy, dz);
                    if (pos.distSqr(playerPos) > 20.25) continue;
                    if (ForagingMacro.getInstance().isBlockBlacklisted(pos)) continue;

                    Block b = mc.level.getBlockState(pos).getBlock();
                    if (ForagingMacro.isLogBlock(b, mode) && BlockUtil.hasVisibleSide(eyePos, pos)) {
                        double d = Vec3.atCenterOf(pos).distanceToSqr(eyePos);
                        if (d < bestDist) {
                            bestDist = d;
                            best = pos;
                        }
                    }
                }
            }
        }
        return best != null ? best : targetPos;
    }

    @Override
    public ForagingMacroState onTick(ForagingMacro macro) {
        if (mc.player == null || mc.level == null || currentTargetPos == null) {
            KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);
            return new PathfindingState();
        }

        // Check if block was broken (turned to air)
        if (mc.level.isEmptyBlock(currentTargetPos)) {
            if (!logBroken) {
                logBroken = true;
                macro.lastLogBreakTime = System.currentTimeMillis();
                // Blacklist the entire tree cluster so Treecapitator only strikes each tree once
                macro.blacklistTreeCluster(currentTargetPos);
                postBreakTimer.schedule(80L); // Brief sync
            }
            if (postBreakTimer.passed()) {
                log("Tree broken! Single-strike complete, pathfinding to next unmined tree...");
                KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);
                return new PathfindingState();
            }
            KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);
            return this;
        }

        // Verify reach limit (4.5 blocks max)
        Vec3 centerVec = Vec3.atCenterOf(currentTargetPos);
        double distanceSq = mc.player.getEyePosition().distanceToSqr(centerVec);
        if (distanceSq > 22.0) { // Out of reach
            log("Target log out of reach (" + String.format("%.1f", Math.sqrt(distanceSq)) + " blocks), pathfinding closer...");
            KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);
            return new PathfindingState();
        }

        // Auto-swap to Treecapitator / Jungle Axe / Axe in hotbar
        int axeSlot = InventoryUtil.getHotbarSlotOfItem("Treecapitator");
        if (axeSlot == -1) axeSlot = InventoryUtil.getHotbarSlotOfItem("Jungle Axe");
        if (axeSlot == -1) axeSlot = InventoryUtil.getHotbarSlotOfItem("Axe");
        if (axeSlot != -1 && mc.player.getInventory().getSelectedSlot() != axeSlot) {
            mc.player.getInventory().setSelectedSlot(axeSlot);
        }

        // Check if crosshair is pointing at target log or another valid log in tree
        HitResult hit = mc.hitResult;
        boolean canBreak = false;

        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = blockHit.getBlockPos();
            Block hitBlock = mc.level.getBlockState(hitPos).getBlock();

            if (hitPos.equals(currentTargetPos)) {
                canBreak = true;
            } else if (ForagingMacro.isLogBlock(hitBlock, macro.getCurrentForagingMode()) && !macro.isBlockBlacklisted(hitPos) && hitPos.distSqr(currentTargetPos) <= 16) {
                // Pointing at another valid log in same tree cluster, lock on to it!
                macro.setTargetBlockPos(hitPos);
                this.currentTargetPos = hitPos;
                canBreak = true;
            }
        }

        if (!canBreak) {
            // Check if current target is not visible from this angle, find alternative visible log on tree
            Vec3 eye = mc.player.getEyePosition();
            if (!BlockUtil.hasVisibleSide(eye, currentTargetPos)) {
                BlockPos alternate = findBestVisibleLog(currentTargetPos, macro.getCurrentForagingMode());
                if (alternate != null && !alternate.equals(currentTargetPos) && BlockUtil.hasVisibleSide(eye, alternate)) {
                    this.currentTargetPos = alternate;
                    macro.setTargetBlockPos(alternate);
                    aimAtBlock(alternate);
                }
            }

            // Smoothly ease crosshair to the visible face
            if (!RotationHandler.getInstance().isEnabled()) {
                aimAtBlock(currentTargetPos);
            }
            KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);
        } else {
            // Check delay before breaking (Treecapitator cooldown / configured delay)
            long currentDelay = com.vertexai.Vertex.config().foraging.foragingDelay;
            if (System.currentTimeMillis() - macro.lastLogBreakTime < currentDelay) {
                KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);
            } else {
                // Hold left click to mine log
                KeyBindUtil.setKeyBindState(mc.options.keyAttack, true);
            }
        }

        // Fail-safe timeout if block doesn't break in 3.5s
        if (breakDelay.passed()) {
            log("Log taking too long to break from this angle, blacklisting tree and switching target...");
            macro.blacklistTreeCluster(currentTargetPos);
            KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);
            return new PathfindingState();
        }

        return this;
    }

    @Override
    public void onEnd(ForagingMacro macro) {
        KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);
        RotationHandler.getInstance().stop();
    }
}
