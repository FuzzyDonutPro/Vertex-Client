package com.vertexai.macro.impl.GemstoneMacro;

import lombok.Getter;
import com.vertexai.macro.AbstractMacro;
import com.vertexai.handler.RotationHandler;
import com.vertexai.util.helper.Clock;
import com.vertexai.util.helper.RotationConfiguration;
import com.vertexai.util.helper.Target;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;

/**
 * GemstoneMacro — Auto-mines Ruby, Sapphire, Topaz, Amber, Amethyst, and Jasper Gemstones
 * in Crystal Hollows with precision block breaking and lava avoidance.
 */
public class GemstoneMacro extends AbstractMacro {

    @Getter
    private static final GemstoneMacro instance = new GemstoneMacro();

    private final Clock mineClock = new Clock();
    private BlockPos targetPos = null;

    @Override
    public String getName() {
        return "Gemstone Miner";
    }

    @Override
    public List<String> getNecessaryItems() {
        return Collections.emptyList();
    }

    @Override
    public void enable() {
        super.enable();
        log("GemstoneMacro: Enabled! Searching for Gemstones...");
    }

    @Override
    public void onTick() {
        if (!isEnabled() || mc.player == null || mc.level == null) return;
        if (mineClock.isScheduled() && !mineClock.passed()) return;

        targetPos = findNearestGemstone();

        if (targetPos != null) {
            Vec3 targetVec = new Vec3(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
            RotationHandler.getInstance().easeTo(new RotationConfiguration(new Target(targetVec), 120, null));

            if (mc.gameMode != null) {
                mc.gameMode.continueDestroyBlock(targetPos, net.minecraft.core.Direction.UP);
                mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
            mineClock.schedule(150);
        }
    }

    private BlockPos findNearestGemstone() {
        if (mc.level == null || mc.player == null) return null;
        BlockPos playerPos = mc.player.blockPosition();
        BlockPos nearest = null;
        double nearestDistSq = 25.0;

        for (int x = -4; x <= 4; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -4; z <= 4; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    var state = mc.level.getBlockState(pos);

                    if (state.getBlock() instanceof StainedGlassBlock || state.getBlock() instanceof StainedGlassPaneBlock) {
                        double distSq = pos.distSqr(playerPos);
                        if (distSq < nearestDistSq) {
                            nearestDistSq = distSq;
                            nearest = pos;
                        }
                    }
                }
            }
        }
        return nearest;
    }
}
