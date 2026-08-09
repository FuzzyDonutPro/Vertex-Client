package com.vertexai.macro.impl.ForagingMacro.states;

import com.vertexai.macro.impl.ForagingMacro.ForagingMacro;
import com.vertexai.macro.impl.ForagingMacro.ForagingMacroState;
import com.vertexai.handler.RotationHandler;
import com.vertexai.util.InventoryUtil;
import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.helper.Clock;
import com.vertexai.util.helper.RotationConfiguration;
import com.vertexai.util.helper.Target;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class BreakingState implements ForagingMacroState {

    private final Minecraft mc = Minecraft.getInstance();
    private final Clock breakDelay = new Clock();
    private final Clock postBreakTimer = new Clock();
    private final Clock preMiningTimer = new Clock();
    private boolean logBroken = false;
    private boolean preMiningScheduled = false;

    @Override
    public void onStart(ForagingMacro macro) {
        log("Aiming and breaking target tree...");
        breakDelay.reset();
        postBreakTimer.reset();
        preMiningTimer.reset();
        logBroken = false;
        preMiningScheduled = false;

        if (macro.getTargetBlockPos() != null && mc.player != null) {
            Vec3 centerVec = Vec3.atCenterOf(macro.getTargetBlockPos());
            com.vertexai.util.helper.Angle targetAngle = com.vertexai.util.AngleUtil.getRotation(centerVec);
            mc.player.setYRot(targetAngle.getYaw());
            mc.player.setXRot(targetAngle.getPitch());
        }
    }

    @Override
    public ForagingMacroState onTick(ForagingMacro macro) {
        if (mc.player == null || mc.level == null || macro.getTargetBlockPos() == null) {
            return new PathfindingState();
        }

        BlockPos targetPos = macro.getTargetBlockPos();

        // Check if block turned to air
        if (mc.level.isEmptyBlock(targetPos)) {
            if (!logBroken) {
                logBroken = true;
                postBreakTimer.schedule(150L); // 150ms post-break pause for Treecapitator/Jungle Axe server packet sync
            }
            if (postBreakTimer.passed()) {
                log("Tree log broken!");
                return new PathfindingState();
            }
            // Stop attacking while waiting for packet sync
            KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);
            return this;
        }

        // Verify reach limit (4.5 blocks max)
        Vec3 centerVec = Vec3.atCenterOf(targetPos);
        double distanceSq = mc.player.getEyePosition().distanceToSqr(centerVec);
        if (distanceSq > 20.25) { // Out of 4.5-block reach
            log("Target log out of reach (" + String.format("%.1f", Math.sqrt(distanceSq)) + " blocks), pathfinding closer...");
            return new PathfindingState();
        }

        // Keep camera aligned to center of target log block
        com.vertexai.util.helper.Angle targetAngle = com.vertexai.util.AngleUtil.getRotation(centerVec);
        mc.player.setYRot(targetAngle.getYaw());
        mc.player.setXRot(targetAngle.getPitch());

        // Auto-swap to Treecapitator / Jungle Axe / Axe in hotbar
        int axeSlot = InventoryUtil.getHotbarSlotOfItem("Treecapitator");
        if (axeSlot == -1) axeSlot = InventoryUtil.getHotbarSlotOfItem("Jungle Axe");
        if (axeSlot == -1) axeSlot = InventoryUtil.getHotbarSlotOfItem("Axe");
        if (axeSlot != -1 && mc.player.getInventory().getSelectedSlot() != axeSlot) {
            mc.player.getInventory().setSelectedSlot(axeSlot);
        }

        // Enforce user-configured Pre-Mining Delay before attacking (convert seconds to ms)
        float preMiningDelaySec = com.vertexai.Vertex.config().foraging.logBreakDelay;
        long preMiningDelayMs = (long) (preMiningDelaySec * 1000L);
        if (preMiningDelayMs > 0) {
            if (!preMiningScheduled) {
                preMiningTimer.schedule(preMiningDelayMs);
                preMiningScheduled = true;
            }
            if (!preMiningTimer.passed()) {
                // Wait during configured pre-mining delay
                KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);
                return this;
            }
        }

        // Regular Mining / Breaking logic (Hold left click)
        KeyBindUtil.setKeyBindState(mc.options.keyAttack, true);

        // Fail-safe timeout if block doesn't break in 5s
        if (!breakDelay.isScheduled()) {
            breakDelay.schedule(5000L);
        } else if (breakDelay.passed()) {
            log("Log taking too long to break, switching target...");
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
