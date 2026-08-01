package com.vertexai.failsafe.impl;

import lombok.Getter;
import com.vertexai.failsafe.AbstractFailsafe;
import com.vertexai.macro.MacroManager;
import com.vertexai.util.Logger;
import com.vertexai.util.helper.Angle;
import com.vertexai.util.helper.Clock;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;

public class RotationFailsafe extends AbstractFailsafe {

    public static final RotationFailsafe instance = new RotationFailsafe();
    public static RotationFailsafe getInstance() { return instance; }
    private final Clock triggerCheck = new Clock();
    private Angle rotationBeforeReacting = null;

    public int getPriority() {
        return 5;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public Failsafe getFailsafeType() {
        return Failsafe.ROTATION;
    }

    @Override
    public boolean onPacketReceive(Packet<?> packet) {
        if (!MacroManager.getInstance().isEnabled()) return false;

        if (packet instanceof ClientboundPlayerPositionPacket p) {
            double packetYaw = p.change().yRot();
            double packetPitch = p.change().xRot();
            double playerYaw = mc.player.getYRot();
            double playerPitch = mc.player.getXRot();

            float yawDifference = Math.abs(mc.player.getYRot() - p.change().yRot());
            float pitchDifference = Math.abs(mc.player.getXRot() - p.change().xRot());

            if (yawDifference == 360F && pitchDifference == 0F) return false;

            if (shouldTriggerCheck(packetYaw, packetPitch)) {
                if (rotationBeforeReacting == null)
                    rotationBeforeReacting = new Angle((float) playerYaw, (float) playerPitch);
            }

            triggerCheck.schedule(500);
        }

        return false;
    }

    @Override
    public boolean onTick() {
        if (!MacroManager.getInstance().isEnabled()) {
            rotationBeforeReacting = null;
            return false;
        }

        if (triggerCheck.passed() && triggerCheck.isScheduled()) {
            if (rotationBeforeReacting == null) return false;

            if (shouldTriggerCheck(rotationBeforeReacting.getYaw(), rotationBeforeReacting.getPitch())) {
                return true;
            }

            rotationBeforeReacting = null;
            triggerCheck.reset();
        }

        return false;
    }

    private boolean shouldTriggerCheck(double newYaw, double newPitch) {
        if (!com.vertexai.Vertex.config().failsafe.enableRotationFailsafe) return false;
        double yawDiff = Math.abs(newYaw - mc.player.getYRot()) % 360;
        double pitchDiff = Math.abs(newPitch - mc.player.getXRot()) % 360;
        int threshold = com.vertexai.Vertex.config().failsafe.rotationThreshold;
        return yawDiff >= threshold || pitchDiff >= threshold;
    }

    @Override
    public boolean react() {
        MacroManager.getInstance().disable();
        Logger.sendWarning("You`ve got rotated! Disabeling macro.");
        return true;
    }

}
