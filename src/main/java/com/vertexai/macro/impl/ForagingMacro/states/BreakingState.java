package com.vertexai.macro.impl.ForagingMacro.states;

import com.vertexai.macro.impl.ForagingMacro.ForagingMacro;
import com.vertexai.macro.impl.ForagingMacro.ForagingMacroState;
import com.vertexai.handler.RotationHandler;
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
    private final Clock throwDelay = new Clock();
    private final Clock reaimTimer = new Clock();

    @Override
    public void onStart(ForagingMacro macro) {
        log("Aiming and breaking target tree...");
        breakDelay.reset();
        throwDelay.reset();
        reaimTimer.reset();
    }

    @Override
    public ForagingMacroState onTick(ForagingMacro macro) {
        if (mc.player == null || mc.level == null || macro.getTargetBlockPos() == null) {
            return new PathfindingState();
        }

        BlockPos targetPos = macro.getTargetBlockPos();

        // Check if block turned to air
        if (mc.level.isEmptyBlock(targetPos)) {
            log("Tree log broken!");
            return new PathfindingState();
        }

        // Verify reach limit (4.5 blocks max)
        Vec3 centerVec = Vec3.atCenterOf(targetPos);
        double distanceSq = mc.player.getEyePosition().distanceToSqr(centerVec);
        if (distanceSq > 20.25) { // Out of 4.5-block reach
            log("Target log out of reach (" + String.format("%.1f", Math.sqrt(distanceSq)) + " blocks), pathfinding closer...");
            return new PathfindingState();
        }

        // Check if crosshair raycast is directly touching the target log block
        boolean crosshairOnTarget = mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult blockHit
                && blockHit.getBlockPos().equals(targetPos);

        // If crosshair is not touching target log block, smoothly rotate until locked on
        if (!crosshairOnTarget) {
            if (!RotationHandler.getInstance().isEnabled()) {
                RotationHandler.getInstance().easeTo(new RotationConfiguration(
                        new Target(centerVec),
                        80L,
                        null
                ));
            }
        }

        // Auto-swap to axe / Treecapitator in hotbar
        int axeSlot = com.vertexai.util.InventoryUtil.getHotbarSlotOfItem("Axe");
        if (axeSlot == -1) axeSlot = com.vertexai.util.InventoryUtil.getHotbarSlotOfItem("Treecapitator");
        if (axeSlot != -1) {
            mc.player.getInventory().setSelectedSlot(axeSlot);
        }

        // Axe Throwing logic for specialized SkyBlock axes (Right click)
        if (!throwDelay.isScheduled() || throwDelay.passed()) {
            KeyBindUtil.rightClick();
            throwDelay.schedule(350L);
        }

        // Regular Mining / Breaking logic (Hold left click)
        KeyBindUtil.setKeyBindState(mc.options.keyAttack, true);

        // Fail-safe timeout if block doesn't break in 2.5s
        if (!breakDelay.isScheduled()) {
            breakDelay.schedule(2500L);
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
