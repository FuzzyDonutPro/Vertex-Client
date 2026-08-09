package com.vertexai.handler;

import com.vertexai.util.AngleUtil;
import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.helper.Angle;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Universal Block Breaking Engine for Vertex Client.
 * Manages zero-latency crosshair alignment, left-click input, and Minecraft MultiPlayerGameMode interaction.
 */
public class BlockBreakingEngine {

    private static final Minecraft mc = Minecraft.getInstance();
    private static BlockBreakingEngine instance;

    private BlockPos currentTargetPos = null;
    private Block initialTargetBlock = null;
    private boolean isBreaking = false;
    private long breakStartTime = 0L;

    public static BlockBreakingEngine getInstance() {
        if (instance == null) {
            instance = new BlockBreakingEngine();
        }
        return instance;
    }

    /**
     * Start or continue mining the specified block position.
     * @param targetPos Target block position to mine.
     * @return true if target is valid and actively being broken, false if broken or invalid.
     */
    public boolean breakBlock(BlockPos targetPos) {
        if (mc.player == null || mc.level == null || targetPos == null) {
            stopBreaking();
            return false;
        }

        Block currentBlock = mc.level.getBlockState(targetPos).getBlock();

        // Check if block turned to air or changed block type
        if (mc.level.isEmptyBlock(targetPos) || (initialTargetBlock != null && !currentBlock.equals(initialTargetBlock))) {
            stopBreaking();
            return false;
        }

        Vec3 centerVec = Vec3.atCenterOf(targetPos);

        // Instant direct camera angle lock on block center
        Angle targetAngle = AngleUtil.getRotation(centerVec);
        mc.player.setYRot(targetAngle.getYaw());
        mc.player.setXRot(targetAngle.getPitch());

        // Perform instantaneous crosshair raycast pick to verify crosshair is directly touching block
        boolean crosshairOnTarget = false;
        Direction side = Direction.UP;

        if (mc.hitResult instanceof BlockHitResult blockHit && blockHit.getBlockPos().equals(targetPos)) {
            crosshairOnTarget = true;
            side = blockHit.getDirection();
        } else {
            // Force raycast pick from current eye position with new yaw/pitch angles (strict 3.0 block reach)
            net.minecraft.world.phys.HitResult hit = mc.player.pick(3.0, 0.0f, false);
            if (hit instanceof BlockHitResult blockHit && blockHit.getBlockPos().equals(targetPos)) {
                crosshairOnTarget = true;
                side = blockHit.getDirection();
            }
        }

        // Initialize breaking state on first tick of new block target
        if (currentTargetPos == null || !currentTargetPos.equals(targetPos)) {
            currentTargetPos = targetPos;
            initialTargetBlock = currentBlock;
            isBreaking = true;
            breakStartTime = System.currentTimeMillis();

            if (mc.gameMode != null) {
                mc.gameMode.startDestroyBlock(targetPos, side);
            }
        }

        // Hold attack key (left click)
        KeyBindUtil.setKeyBindState(mc.options.keyAttack, true);

        return true;
    }

    /**
     * Stop breaking current block and release attack key.
     */
    public void stopBreaking() {
        if (isBreaking) {
            isBreaking = false;
            currentTargetPos = null;
            initialTargetBlock = null;
            breakStartTime = 0L;
            if (mc.options != null && mc.options.keyAttack != null) {
                KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);
            }
            if (mc.gameMode != null) {
                mc.gameMode.stopDestroyBlock();
            }
        }
    }

    public boolean isBreaking() {
        return isBreaking;
    }

    public BlockPos getCurrentTargetPos() {
        return currentTargetPos;
    }

    public long getBreakDurationMs() {
        return isBreaking ? (System.currentTimeMillis() - breakStartTime) : 0L;
    }
}
