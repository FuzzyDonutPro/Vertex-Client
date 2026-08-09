package com.vertexai.failsafe.impl;

import lombok.Getter;
import com.vertexai.failsafe.AbstractFailsafe;
import com.vertexai.macro.impl.misc.LagDetector;
import com.vertexai.macro.MacroManager;
import com.vertexai.util.Logger;
import com.vertexai.util.helper.Clock;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

public class TeleportFailsafe extends AbstractFailsafe {

    public static final TeleportFailsafe instance = new TeleportFailsafe();
    public static TeleportFailsafe getInstance() { return instance; }
    private final LagDetector lagDetector = LagDetector.getInstance();
    private final Minecraft mc = Minecraft.getInstance();
    private final Clock triggerCheck = new Clock();
    private Vec3 originalPosition = null;
    private boolean potentialTeleportDetected = false;

    @Override
    public String getName() {
        return "TeleportFailsafe";
    }

    @Override
    public Failsafe getFailsafeType() {
        return Failsafe.TELEPORT;
    }

    @Override
    public int getPriority() {
        return 5;
    }

    @Override
    public boolean react() {
        Logger.sendWarning("You have been teleported");
        MacroManager.getInstance().disable();
        reset();
        return true;
    }

    @Override
    public boolean onPacketReceive(Packet<?> packet) {
        if (!(packet instanceof ClientboundPlayerPositionPacket p)) {
            return false;
        }

        Vec3 currentPlayerPos = mc.player.position();
        Vec3 packetPlayerPos = new Vec3(
                p.change().position().x + (0),
                p.change().position().y + (0),
                p.change().position().z + (0)
        );

        double distance = currentPlayerPos.distanceTo(packetPlayerPos);

        if (distance >= 1) {
            final double lastReceivedPacketDistance = currentPlayerPos.distanceTo(lagDetector.getLastPacketPosition());
            final double playerMovementSpeed = mc.player.getAttributes().getValue(Attributes.MOVEMENT_SPEED);
            final int ticksSinceLastPacket = (int) Math.ceil(lagDetector.getTimeSinceLastTick() / 50D);
            final double estimatedMovement = playerMovementSpeed * ticksSinceLastPacket;

            if (lastReceivedPacketDistance > 7.5D && Math.abs(lastReceivedPacketDistance - estimatedMovement) < 2) {
                return false;
            }

            if (originalPosition == null) {
                originalPosition = currentPlayerPos;
            }

            potentialTeleportDetected = true;
            triggerCheck.schedule(500);
            return false;
        }

        return false;
    }

    @Override
    public boolean onTick() {
        if (potentialTeleportDetected && triggerCheck.isScheduled() && triggerCheck.passed()) {
            triggerCheck.reset();
            potentialTeleportDetected = false;

            Vec3 currentPosition = mc.player.position();
            if (originalPosition != null) {
                double totalDisplacement = currentPosition.distanceTo(originalPosition);
                originalPosition = null;
                return totalDisplacement > 1;
            }
        }

        return false;
    }

    public void reset() {
        originalPosition = null;
        potentialTeleportDetected = false;
        triggerCheck.reset();
    }

}
