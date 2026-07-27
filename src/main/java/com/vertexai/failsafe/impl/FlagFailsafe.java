package com.vertexai.failsafe.impl;

import lombok.Getter;
import com.vertexai.Vertex;
import com.vertexai.failsafe.AbstractFailsafe;
import com.vertexai.macro.MacroManager;
import com.vertexai.util.Logger;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;

import java.util.ArrayList;
import java.util.List;

public class FlagFailsafe extends AbstractFailsafe {

    @Getter
    private static final FlagFailsafe instance = new FlagFailsafe();

    private final List<Long> flagTimestamps = new ArrayList<>();

    @Override
    public String getName() {
        return "FlagFailsafe";
    }

    @Override
    public Failsafe getFailsafeType() {
        return Failsafe.FLAG;
    }

    @Override
    public int getPriority() {
        return 8;
    }

    @Override
    public boolean onPacketReceive(Packet<?> packet) {
        if (!MacroManager.getInstance().isRunning()) return false;
        if (!Vertex.config().failsafe.enableFlagFailsafe) return false;

        if (packet instanceof ClientboundPlayerPositionPacket) {
            long now = System.currentTimeMillis();
            flagTimestamps.add(now);

            long windowMs = Vertex.config().failsafe.flagTimeWindow * 1000L;
            flagTimestamps.removeIf(t -> (now - t) > windowMs);

            int threshold = Vertex.config().failsafe.flagThreshold;
            if (flagTimestamps.size() >= threshold) {
                flagTimestamps.clear();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean onTick() {
        if (!MacroManager.getInstance().isRunning()) {
            flagTimestamps.clear();
            return false;
        }

        long now = System.currentTimeMillis();
        long windowMs = Vertex.config().failsafe.flagTimeWindow * 1000L;
        flagTimestamps.removeIf(t -> (now - t) > windowMs);
        return false;
    }

    @Override
    public boolean react() {
        Logger.sendWarning("Repeated rubberbanding / flagging detected! Disabling macro.");
        MacroManager.getInstance().disable();
        flagTimestamps.clear();
        return true;
    }

    public void reset() {
        flagTimestamps.clear();
    }
}
