package com.vertexai.dungeons;

import com.vertexai.util.AngleUtil;
import com.vertexai.util.Logger;
import com.vertexai.util.helper.Angle;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * EtherwarpHelper — Precision 61-block Etherwarp raycasting and teleport engine.
 * Calculates exact rotation vectors, validates target landing surfaces, and executes AOTV/AOTE teleports.
 */
public class EtherwarpHelper {

    private static final Minecraft mc = Minecraft.getInstance();
    public static final double MAX_ETHERWARP_RANGE = 61.0;

    /**
     * Raycasts along the player's view vector up to 61 blocks to find the Etherwarp target block.
     * @return Target BlockPos if valid landing spot exists, otherwise null.
     */
    public static BlockPos getEtherwarpTarget(double maxDistance) {
        if (mc.player == null || mc.level == null) return null;

        Vec3 eyePos = mc.player.getEyePosition(1.0f);
        Vec3 lookVec = mc.player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.x * maxDistance, lookVec.y * maxDistance, lookVec.z * maxDistance);

        HitResult hit = mc.level.clip(new net.minecraft.world.level.ClipContext(
                eyePos,
                endPos,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                mc.player
        ));

        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            BlockPos pos = blockHit.getBlockPos();
            if (isValidEtherwarpLanding(mc.level, pos)) {
                return pos;
            }
        }
        return null;
    }

    /**
     * Checks if a target block is a valid Etherwarp landing surface.
     * Requires target block to be solid, and 2 air blocks above for player clearance.
     */
    public static boolean isValidEtherwarpLanding(Level level, BlockPos pos) {
        if (level == null || pos == null) return false;

        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;

        BlockPos above1 = pos.above();
        BlockPos above2 = pos.above(2);

        BlockState stateAbove1 = level.getBlockState(above1);
        BlockState stateAbove2 = level.getBlockState(above2);

        // Player needs 2 blocks clearance above target block
        return (stateAbove1.isAir() || !stateAbove1.isSolid()) &&
               (stateAbove2.isAir() || !stateAbove2.isSolid());
    }

    /**
     * Teleports to target BlockPos using Etherwarp (AOTV/AOTE).
     * Rotates camera to target top-center, sneaks, and uses held item.
     */
    public static boolean teleportTo(BlockPos target) {
        if (mc.player == null || mc.level == null || target == null) return false;

        Vec3 eyePos = mc.player.getEyePosition(1.0f);
        Vec3 targetCenter = new Vec3(target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5);

        if (eyePos.distanceTo(targetCenter) > MAX_ETHERWARP_RANGE) {
            Logger.sendWarning("Etherwarp target out of range (>61m): " + target.toShortString());
            return false;
        }

        // Aim camera at target top-center
        Angle angle = AngleUtil.getRotation(targetCenter);
        mc.player.setYRot(angle.getYaw());
        mc.player.setXRot(angle.getPitch());

        // Verify holding AOTV / AOTE or Etherwarp conduit
        ItemStack mainHand = mc.player.getMainHandItem();
        String itemName = mainHand.getHoverName().getString().toLowerCase();
        boolean isEtherwarpItem = itemName.contains("aspect of the void") ||
                                 itemName.contains("aspect of the end") ||
                                 itemName.contains("etherwarp") ||
                                 itemName.contains("aotv") ||
                                 itemName.contains("aote");

        if (!isEtherwarpItem) {
            Logger.sendWarning("Not holding Etherwarp item (AOTV/AOTE)!");
            return false;
        }

        // Sneak + Right Click
        mc.options.keyShift.setDown(true);
        if (mc.gameMode != null) {
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        }

        // Reset sneak state after brief delay
        com.vertexai.Vertex.executor().execute(() -> {
            try {
                Thread.sleep(100);
                mc.options.keyShift.setDown(false);
            } catch (InterruptedException ignored) {}
        });

        Logger.sendMessage("Etherwarp teleported to " + target.toShortString());
        return true;
    }
}
