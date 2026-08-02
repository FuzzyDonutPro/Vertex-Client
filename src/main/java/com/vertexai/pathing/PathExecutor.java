package com.vertexai.pathing;

import com.vertexai.bypass.SprintBypass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class PathExecutor {
    private final Minecraft mc = Minecraft.getInstance();
    private List<BlockPos> currentPath;
    private int pathIndex = 0;

    public void setPath(List<BlockPos> path) {
        this.currentPath = path;
        this.pathIndex = 0;
    }

    public void tick() {
        if (currentPath == null || pathIndex >= currentPath.size()) {
            stopMovement();
            return;
        }

        LocalPlayer player = mc.player;
        if (player == null) return;

        BlockPos target = currentPath.get(pathIndex);
        Vec3 targetVec = new Vec3(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);

        // Advance to the next node if we're close enough
        if (player.position().distanceTo(targetVec) < 0.6) {
            pathIndex++;
            if (pathIndex >= currentPath.size()) {
                stopMovement();
                return;
            }
            target = currentPath.get(pathIndex);
            targetVec = new Vec3(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
        }

        // Calculate needed rotations
        double dX = targetVec.x - player.getX();
        double dY = targetVec.y - player.getY();
        double dZ = targetVec.z - player.getZ();

        double horizDist = Math.sqrt(dX * dX + dZ * dZ);
        float targetYaw = (float) (Mth.atan2(dZ, dX) * (180D / Math.PI)) - 90.0F;
        float targetPitch = (float) -(Mth.atan2(dY, horizDist) * (180D / Math.PI));

        com.vertexai.pathing.aim.HumanAimSimulator.loadProfile(); // Ensure it's loaded
        float[] nextAngles = com.vertexai.pathing.aim.HumanAimSimulator.getNextAnglePathfinding(
            mc.player.getYRot(), mc.player.getXRot(), targetYaw, targetPitch
        );
        mc.player.setYRot(nextAngles[0]);
        mc.player.setXRot(nextAngles[1]);

        // Simulate forward movement
        if (mc.options != null) {
            mc.options.keyUp.setDown(true);

            // Sprint bypass — only hold sprint if all Grim sprint checks pass
            mc.options.keySprint.setDown(SprintBypass.canSprint());

            // Simulate jump if needed (next block is higher and player is on ground)
            double maxJumpHeight = com.vertexai.pathfinder.calculate.FluidAndFlyingPathfinder.getInstance().getMaxJumpClearanceHeight();
            boolean needsJump = (dX * dX + dZ * dZ > 0.01) && (target.getY() - player.getBlockY() >= 1) && ((target.getY() - player.getBlockY()) <= maxJumpHeight) && player.onGround();
            com.vertexai.util.KeyBindUtil.setKeyBindState(mc.options.keyJump, needsJump);
        }
    }

    private float interpolateRotation(float current, float target, float maxChange) {
        float diff = Mth.wrapDegrees(target - current);
        if (diff > maxChange) diff = maxChange;
        if (diff < -maxChange) diff = -maxChange;
        return current + diff;
    }

    public void stopMovement() {
        if (mc.options != null) {
            mc.options.keyUp.setDown(false);
            mc.options.keyJump.setDown(false);
            mc.options.keySprint.setDown(false);
        }
        currentPath = null;
    }
}
