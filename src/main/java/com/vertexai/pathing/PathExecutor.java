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

        // Dynamic nearest node search along current path
        int nearestIdx = pathIndex;
        double minDistanceSq = Double.MAX_VALUE;
        for (int i = 0; i < currentPath.size(); i++) {
            BlockPos n = currentPath.get(i);
            double distSq = player.position().distanceToSqr(n.getX() + 0.5, n.getY(), n.getZ() + 0.5);
            if (distSq < minDistanceSq) {
                minDistanceSq = distSq;
                nearestIdx = i;
            }
        }
        if (nearestIdx > pathIndex) {
            pathIndex = nearestIdx;
        }

        int lookIndex = Math.min(pathIndex, currentPath.size() - 1);
        BlockPos target = currentPath.get(lookIndex);
        Vec3 targetVec = new Vec3(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);

        // Advance to next node if close enough (strict horizontal distance, generous vertical distance for slabs/stairs)
        double hDistSq = (targetVec.x - player.getX()) * (targetVec.x - player.getX()) + (targetVec.z - player.getZ()) * (targetVec.z - player.getZ());
        double vDist = Math.abs(targetVec.y - player.getY());
        
        if (hDistSq < (0.35 * 0.35) && vDist < 1.2) {
            pathIndex++;
            if (pathIndex >= currentPath.size()) {
                stopMovement();
                return;
            }
            lookIndex = Math.min(pathIndex, currentPath.size() - 1);
            target = currentPath.get(lookIndex);
            targetVec = new Vec3(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
        }

        // Calculate needed rotations
        double dX = targetVec.x - player.getX();
        double dY = targetVec.y - player.getY();
        double dZ = targetVec.z - player.getZ();

        double horizDist = Math.sqrt(dX * dX + dZ * dZ);
        float targetYaw = (float) (Mth.atan2(dZ, dX) * (180D / Math.PI)) - 90.0F;
        float rawPitch = (float) -(Mth.atan2(dY, horizDist) * (180D / Math.PI));
        
        // Soften vertical camera motion by dividing by 1.4 instead of hard capping
        float targetPitch = (float) (rawPitch / 1.4);

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
