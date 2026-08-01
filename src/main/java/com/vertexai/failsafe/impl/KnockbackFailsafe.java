package com.vertexai.failsafe.impl;

import lombok.Getter;
import com.vertexai.Vertex;
import com.vertexai.failsafe.AbstractFailsafe;
import com.vertexai.macro.MacroManager;
import com.vertexai.util.Logger;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;

public class KnockbackFailsafe extends AbstractFailsafe {

    public static final KnockbackFailsafe instance = new KnockbackFailsafe();
    public static KnockbackFailsafe getInstance() { return instance; }

    public int getPriority() {
        return 8;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public Failsafe getFailsafeType() {
        return Failsafe.KNOCKBACK;
    }


    @Override
    public boolean onPacketReceive(Packet<?> packet) {
        if (!(packet instanceof ClientboundSetEntityMotionPacket)) return false;
ClientboundSetEntityMotionPacket motionPacket = (ClientboundSetEntityMotionPacket) packet;
int id = motionPacket.getId();
net.minecraft.world.phys.Vec3 movement = new net.minecraft.world.phys.Vec3(motionPacket.getMovement().x, motionPacket.getMovement().y, motionPacket.getMovement().z);
        if (id != mc.player.getId()) return false;
        return movement.y >= Vertex.config().failsafe.verticalKnockbackThreshold;
    }

    @Override
    public boolean react() {
        MacroManager.getInstance().disable();
        Logger.sendWarning("Knockback has been detected! Disabling macro.");
        return true;
    }
}
