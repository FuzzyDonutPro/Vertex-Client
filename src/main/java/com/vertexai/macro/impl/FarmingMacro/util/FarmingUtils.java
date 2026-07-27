package com.vertexai.macro.impl.FarmingMacro.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.vertexai.Vertex;

import java.util.ArrayList;
import java.util.List;

public class FarmingUtils {

    /**
     * Scans a 5x5x5 area around the player to dynamically determine which crop they are standing in.
     */
    public static CropEnum getFarmingCrop() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return CropEnum.NONE;

        BlockPos playerPos = mc.player.blockPosition();
        List<BlockPos> scanPositions = new ArrayList<>();

        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 3; y++) {
                for (int z = -2; z <= 2; z++) {
                    scanPositions.add(playerPos.offset(x, y, z));
                }
            }
        }

        CropEnum closestCrop = CropEnum.NONE;
        double closestDist = Double.MAX_VALUE;

        for (BlockPos pos : scanPositions) {
            BlockState state = mc.level.getBlockState(pos);
            Block block = state.getBlock();
            CropEnum currentType = CropEnum.NONE;

            if (block == Blocks.WHEAT) currentType = CropEnum.WHEAT;
            else if (block == Blocks.CARROTS) currentType = CropEnum.CARROT;
            else if (block == Blocks.POTATOES) currentType = CropEnum.POTATO;
            else if (block == Blocks.NETHER_WART) currentType = CropEnum.NETHER_WART;
            else if (block == Blocks.SUGAR_CANE) currentType = CropEnum.SUGAR_CANE;
            else if (block == Blocks.MELON) currentType = CropEnum.MELON;
            else if (block == Blocks.PUMPKIN) currentType = CropEnum.PUMPKIN;
            else if (block == Blocks.RED_MUSHROOM || block == Blocks.BROWN_MUSHROOM) currentType = CropEnum.MUSHROOM;
            else if (block == Blocks.CACTUS) currentType = CropEnum.CACTUS;
            else if (block == Blocks.COCOA) currentType = CropEnum.COCOA_BEANS;

            if (currentType != CropEnum.NONE) {
                double dist = pos.distToCenterSqr(mc.player.position());
                if (dist < closestDist) {
                    closestDist = dist;
                    closestCrop = currentType;
                }
            }
        }

        return closestCrop;
    }

    /**
     * Determines the optimal Yaw angle based on the crop type and the player's closest cardinal direction.
     */
    public static float getOptimalYaw(CropEnum crop, float currentYaw) {
        // Snap to nearest 90 degrees or 45 degrees depending on the crop
        float normalizedYaw = currentYaw % 360;
        if (normalizedYaw < 0) normalizedYaw += 360;

        if (crop == CropEnum.MUSHROOM || crop == CropEnum.SUGAR_CANE) {
            // FarmHelper uses diagonal for sugarcane/mushrooms
            float[] diagonals = {45, 135, 225, 315};
            return getClosestAngle(normalizedYaw, diagonals);
        } else {
            // Default 90 degree snap
            float[] cardinals = {0, 90, 180, 270};
            return getClosestAngle(normalizedYaw, cardinals);
        }
    }

    /**
     * Determines the optimal Pitch angle based on the crop type.
     */
    public static float getOptimalPitch(CropEnum crop) {
        switch (crop) {
            case NETHER_WART:
            case CARROT:
            case POTATO:
            case WHEAT:
                return 0f; // FarmHelper typically farms these perfectly flat
            case SUGAR_CANE:
            case MUSHROOM:
                return 0f; 
            case MELON:
            case PUMPKIN:
                return (float) Vertex.config().melonPumpkin.customPitch; // Look down based on custom override
            case CACTUS:
                return -20f; // Look up slightly
            case COCOA_BEANS:
                return 0f;
            default:
                return 0f;
        }
    }

    private static float getClosestAngle(float target, float[] angles) {
        float closest = angles[0];
        float minDiff = Math.abs(getAngleDifference(target, angles[0]));

        for (int i = 1; i < angles.length; i++) {
            float diff = Math.abs(getAngleDifference(target, angles[i]));
            if (diff < minDiff) {
                minDiff = diff;
                closest = angles[i];
            }
        }
        return closest;
    }

    private static float getAngleDifference(float a, float b) {
        float diff = (a - b + 180) % 360 - 180;
        return diff < -180 ? diff + 360 : diff;
    }
}
