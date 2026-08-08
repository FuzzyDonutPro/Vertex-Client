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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * AimState
 * <p>
 * Step 2 of 4-step mining lifecycle: Smooth Aim.
 * Rotates camera toward target block point while keeping left click released.
 * Prevents sending destruction packets or starting attack while gliding camera.
 */
public class AimState implements BlockMinerState {

    private final Minecraft mc = Minecraft.getInstance();
    private int aimTicks = 0;

    @Override
    public void onStart(BlockMiner miner) {
        log("Entering Aim State");
        aimTicks = 0;

        BlockPos targetPos = miner.getTargetBlockPos();
        if (targetPos == null) {
            logError("No target block pos in AimState");
            return;
        }

        // Release left click during aiming phase
        KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);

        // Find best target point on block face
        List<Vec3> points = BlockUtil.bestPointsOnBestSide(targetPos);
        if (points.isEmpty()) {
            logError("Cannot find points to look at. Returning to STARTING state.");
            miner.setError(BlockMiner.BlockMinerError.NO_POINTS_FOUND);
            miner.stop();
            return;
        }

        Vec3 targetPoint = points.get(0);
        miner.setTargetPoint(targetPoint);

        // Queue camera rotation
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
        if (miner.getTargetBlockPos() == null || mc.level == null) {
            return new StartingState();
        }

        // Keep left click strictly released while aiming
        KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);
        aimTicks++;

        // Pick raycast with current camera angles
        mc.gameRenderer.pick(1.0f);

        boolean arrivedOnTarget = (mc.hitResult instanceof BlockHitResult bhr && bhr.getBlockPos().equals(miner.getTargetBlockPos()));
        boolean rotationFinished = !RotationHandler.getInstance().isEnabled();

        // Safety timeout if aiming takes over 3 seconds (60 ticks)
        if (aimTicks > 60) {
            logError("AimState timeout, returning to StartingState");
            return new StartingState();
        }

        // Once crosshair hits target block or rotation finishes, transition to ArriveState to capture Direction face
        if (arrivedOnTarget || rotationFinished || aimTicks >= 10) {
            log("Aim complete, transitioning to ArriveState");
            return new ArriveState();
        }

        return this;
    }

    @Override
    public void onEnd(BlockMiner miner) {
        log("Exiting Aim State");
    }
}
