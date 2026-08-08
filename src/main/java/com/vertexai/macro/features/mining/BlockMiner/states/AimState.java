package com.vertexai.macro.features.mining.BlockMiner.states;

import com.vertexai.Vertex;
import com.vertexai.macro.features.mining.BlockMiner.BlockMiner;
import com.vertexai.handler.RotationHandler;
import com.vertexai.util.BlockUtil;
import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.helper.RotationConfiguration;
import com.vertexai.util.helper.Target;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * AimState
 * <p>
 * Rotates camera smoothly toward the selected target block.
 * Captures the un-occluded Direction face and transitions to BreakingState.
 */
public class AimState implements BlockMinerState {

    private final Minecraft mc = Minecraft.getInstance();
    private int aimTicks = 0;

    @Override
    public void onStart(BlockMiner miner) {
        log("Entering AimState");
        aimTicks = 0;

        BlockPos targetPos = miner.getTargetBlockPos();
        if (targetPos == null) {
            logError("No target position set in AimState");
            return;
        }

        KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);

        List<Vec3> points = BlockUtil.bestPointsOnBestSide(targetPos);
        Vec3 targetPoint;
        if (!points.isEmpty()) {
            targetPoint = points.get(0);
        } else {
            targetPoint = Vec3.atCenterOf(targetPos);
        }
        miner.setTargetPoint(targetPoint);

        RotationHandler.getInstance().stop();
        RotationHandler.getInstance().queueRotation(
                new RotationConfiguration(
                        new Target(targetPoint),
                        Vertex.config().getRandomRotationTime(),
                        null
                )
        );
        RotationHandler.getInstance().start();
    }

    @Override
    public BlockMinerState onTick(BlockMiner miner) {
        BlockPos targetPos = miner.getTargetBlockPos();
        if (targetPos == null || mc.level == null) {
            return new StartingState();
        }

        KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);
        aimTicks++;

        mc.gameRenderer.pick(1.0f);

        BlockHitResult bhr = (mc.hitResult instanceof BlockHitResult b) ? b : null;
        boolean arrivedOnTarget = (bhr != null && targetPos.equals(bhr.getBlockPos()));
        boolean rotationDone = !RotationHandler.getInstance().isEnabled();

        // Lock Direction face ONCE
        Direction face = (arrivedOnTarget && bhr != null)
                ? bhr.getDirection()
                : BlockUtil.getClosestVisibleSide(targetPos);
        if (face == null) face = Direction.UP;
        miner.setMiningDirection(face);

        // Safety timeout (max 3s aiming)
        if (aimTicks > 60) {
            logError("Aiming timeout exceeded, restarting state machine");
            return new StartingState();
        }

        if (arrivedOnTarget || rotationDone || aimTicks >= 5) {
            log("Aim aligned on " + targetPos + " face " + face + ", transitioning to BreakingState");
            return new BreakingState();
        }

        return this;
    }

    @Override
    public void onEnd(BlockMiner miner) {
        log("Exiting AimState");
    }
}
