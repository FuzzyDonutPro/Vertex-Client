package com.vertexai.macro.impl.ForagingMacro.states;

import com.vertexai.macro.impl.ForagingMacro.ForagingMacro;
import com.vertexai.macro.impl.ForagingMacro.ForagingMacroState;
import com.vertexai.handler.RotationHandler;
import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.helper.Clock;
import com.vertexai.util.helper.RotationConfiguration;
import com.vertexai.util.helper.Target;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public class BreakingState implements ForagingMacroState {

    private final Minecraft mc = Minecraft.getInstance();
    private final Clock breakDelay = new Clock();
    private final Clock throwDelay = new Clock();
    private boolean isAiming = false;

    @Override
    public void onStart(ForagingMacro macro) {
        log("Aiming at tree...");
        isAiming = true;
        Vec3 targetVec = new Vec3(
                macro.getTargetBlockPos().getX() + 0.5,
                macro.getTargetBlockPos().getY() + 0.5,
                macro.getTargetBlockPos().getZ() + 0.5
        );
        RotationHandler.getInstance().easeTo(new RotationConfiguration(
                new Target(targetVec),
                150L,
                () -> isAiming = false
        ));
    }

    @Override
    public ForagingMacroState onTick(ForagingMacro macro) {
        if (isAiming) {
            return this;
        }

        // Check if block was broken
        if (mc.level.isEmptyBlock(macro.getTargetBlockPos())) {
            log("Tree broken!");
            return new PathfindingState();
        }

        // Axe Throwing logic (Right click)
        if (!throwDelay.isScheduled() || throwDelay.passed()) {
            KeyBindUtil.rightClick();
            throwDelay.schedule(300L); // Axe throw cooldown
        }

        // Regular Breaking logic (Left click hold)
        KeyBindUtil.setKeyBindState(mc.options.keyAttack, true);

        // Fail-safe to avoid getting stuck
        if (!breakDelay.isScheduled()) {
            breakDelay.schedule(3000L); // 3 seconds max break time
        } else if (breakDelay.passed()) {
            log("Tree taking too long to break, skipping...");
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
