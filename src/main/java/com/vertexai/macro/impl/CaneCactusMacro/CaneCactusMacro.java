package com.vertexai.macro.impl.CaneCactusMacro;

import lombok.Getter;
import com.vertexai.handler.RotationHandler;
import com.vertexai.macro.AbstractMacro;
import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.helper.Angle;
import com.vertexai.util.helper.Clock;
import com.vertexai.util.helper.RotationConfiguration;
import com.vertexai.util.helper.Target;

import java.util.Collections;
import java.util.List;

/**
 * CaneCactusMacro — Dedicated 45° diagonal strafe macro for Sugar Cane and Cactus farming.
 */
public class CaneCactusMacro extends AbstractMacro {

    @Getter
    public static final CaneCactusMacro instance = new CaneCactusMacro();

    private boolean movingLeft = true;
    private final Clock rowSwitchClock = new Clock();
    private boolean initialized = false;

    @Override
    public String getName() {
        return "Cane/Cactus 45°";
    }

    @Override
    public List<String> getNecessaryItems() {
        return Collections.emptyList();
    }

    @Override
    public void enable() {
        super.enable();
        log("Enabling dedicated Cane/Cactus 45° Macro...");
        initialized = false;
        movingLeft = true;

        if (mc.player != null) {
            // Snap camera to 45.0° yaw, 0.0° pitch for optimal diagonal wall-hug
            float targetYaw = Math.round(mc.player.getYRot() / 90.0f) * 90.0f + 45.0f;
            RotationHandler.getInstance().easeTo(new RotationConfiguration(
                    new Target(new Angle(targetYaw, 0.0f)),
                    400,
                    null
            ));
        }
    }

    @Override
    public void disable() {
        super.disable();
        log("Disabling Cane/Cactus 45° Macro...");
        KeyBindUtil.stopMovement();
    }

    @Override
    public void onTick() {
        if (!isEnabled() || mc.player == null) return;

        // Maintain attack (break cane/cactus)
        KeyBindUtil.setKeyBindState(mc.options.keyAttack, true);

        // Always hold Forward (W)
        KeyBindUtil.setKeyBindState(mc.options.keyUp, true);

        // Toggle Strafe Left (A) or Right (D)
        if (movingLeft) {
            KeyBindUtil.setKeyBindState(mc.options.keyLeft, true);
            KeyBindUtil.setKeyBindState(mc.options.keyRight, false);
        } else {
            KeyBindUtil.setKeyBindState(mc.options.keyRight, true);
            KeyBindUtil.setKeyBindState(mc.options.keyLeft, false);
        }

        // Wall hit detection: if horizontal speed drops, switch row direction
        double vx = mc.player.getDeltaMovement().x;
        double vz = mc.player.getDeltaMovement().z;
        double speedSq = vx * vx + vz * vz;

        if (speedSq < 0.001) {
            if (!rowSwitchClock.isScheduled() || rowSwitchClock.passed()) {
                movingLeft = !movingLeft;
                log("Cane/Cactus 45°: End of row hit! Switching direction to " + (movingLeft ? "LEFT" : "RIGHT"));
                rowSwitchClock.schedule(800); // 800ms cooldown to clear wall
            }
        }
    }
}
