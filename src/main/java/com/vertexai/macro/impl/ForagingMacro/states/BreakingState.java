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

        currentTargetPos = macro.getTargetBlockPos();
        if (currentTargetPos != null && mc.player != null) {
            aimAtBlock(currentTargetPos);
        }
    }

    private void aimAtBlock(BlockPos pos) {
        List<Vec3> points = BlockUtil.bestPointsOnBestSide(pos);
        Vec3 aimPoint = (points != null && !points.isEmpty()) ? points.get(0) : Vec3.atCenterOf(pos);
        RotationHandler.getInstance().easeTo(new RotationConfiguration(
                new Target(aimPoint),
                65L,
                null
        ));
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

        // Check if crosshair is pointing at target log or obstructing leaf block
        HitResult hit = mc.hitResult;
        boolean canBreak = false;

        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = blockHit.getBlockPos();
            Block hitBlock = mc.level.getBlockState(hitPos).getBlock();

            if (hitPos.equals(currentTargetPos)) {
                canBreak = true;
            } else if (ForagingMacro.isLogBlock(hitBlock, macro.getCurrentForagingMode()) && !macro.isBlockBlacklisted(hitPos) && hitPos.distSqr(currentTargetPos) <= 12) {
                // Pointing at valid log in same tree cluster, lock on to it!
                macro.setTargetBlockPos(hitPos);
                this.currentTargetPos = hitPos;
                canBreak = true;
            } else if (ForagingMacro.isLeafBlock(hitBlock) && hitPos.distSqr(currentTargetPos) <= 9) {
                // Pointing at a leaf block blocking our line of sight to the tree, break the leaf!
                canBreak = true;
            }
        }

        if (!canBreak) {
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
                // Hold left click to mine
                KeyBindUtil.setKeyBindState(mc.options.keyAttack, true);
            }
        }

        // Fail-safe timeout if block doesn't break in 3.5s
        if (breakDelay.passed()) {
            log("Log taking too long to break, blacklisting tree and switching target...");
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
