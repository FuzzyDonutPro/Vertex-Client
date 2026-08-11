package com.vertexai.failsafe.impl;

import com.vertexai.Vertex;
import com.vertexai.failsafe.AbstractFailsafe;
import com.vertexai.macro.MacroManager;
import com.vertexai.util.Logger;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec2;

public class IdleFailsafe extends AbstractFailsafe {

    private static final IdleFailsafe instance = new IdleFailsafe();
    public static IdleFailsafe getInstance() { return instance; }

    private BlockPos lastPos;
    private Vec2 lastRot;
    private long lastMovementTime;
    
    @Override
    public String getName() {
        return "Idle Failsafe";
    }

    @Override
    public Failsafe getFailsafeType() {
        return Failsafe.IDLE;
    }

    @Override
    public int getPriority() {
        return 7;
    }

    @Override
    public boolean react() {
        Logger.sendWarning("You have been idle for too long while a macro was active!");
        MacroManager.getInstance().disable();
        return true;
    }

    @Override
    public boolean onTick() {
        if (!Vertex.config().failsafe.enableIdleFailsafe) {
            resetStates();
            return false;
        }

        // Only track idle time if a macro is actually running
        if (!MacroManager.getInstance().isEnabled()) {
            resetStates();
            return false;
        }

        if (mc.player == null) {
            resetStates();
            return false;
        }

        BlockPos currentPos = mc.player.blockPosition();
        Vec2 currentRot = new Vec2(mc.player.getYRot(), mc.player.getXRot());

        if (lastPos == null || lastRot == null) {
            lastPos = currentPos;
            lastRot = currentRot;
            lastMovementTime = System.currentTimeMillis();
            return false;
        }

        // Check if player has moved or rotated
        boolean moved = !currentPos.equals(lastPos);
        boolean rotated = Math.abs(currentRot.x - lastRot.x) > 1.0f || Math.abs(currentRot.y - lastRot.y) > 1.0f;

        if (moved || rotated) {
            lastPos = currentPos;
            lastRot = currentRot;
            lastMovementTime = System.currentTimeMillis();
            return false;
        }

        // Check if idle time exceeds config threshold
        long idleTime = System.currentTimeMillis() - lastMovementTime;
        long maxIdleTime = Vertex.config().failsafe.idleFailsafeTime * 1000L;

        if (idleTime > maxIdleTime) {
            return true;
        }

        return false;
    }

    @Override
    public void resetStates() {
        lastPos = null;
        lastRot = null;
        lastMovementTime = System.currentTimeMillis();
    }
}
