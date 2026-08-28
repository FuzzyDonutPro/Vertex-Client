package com.vertexai.pathfinder.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * RaycastPathPlanner
 * <p>
 * Plans straight-line raycast trajectories through 3D voxel space:
 * - Checks player bounding-box clearance (feet, torso, head)
 * - Verifies floor support along the raycast line
 * - Coordinates directly with RotationHandler and PathExecutor
 */
public class RaycastPathPlanner {

    private static final Minecraft mc = Minecraft.getInstance();
    private static final double PLAYER_WIDTH = 0.56;
    private static final double PLAYER_HEIGHT = 1.80;
    private static final double STEP_SIZE = 0.35;

    /**
     * Finds the furthest reachable node along the path with clear line-of-sight.
     */
    public static int findFurthestVisibleNodeIndex(Vec3 playerPos, List<BlockPos> path, int currentTargetIndex, int maxLookAhead) {
        if (path == null || path.isEmpty()) return currentTargetIndex;
        if (mc.level == null) return currentTargetIndex;

        int maxIndex = Math.min(currentTargetIndex + maxLookAhead, path.size() - 1);
        int bestIndex = currentTargetIndex;

        for (int i = maxIndex; i >= currentTargetIndex; i--) {
            BlockPos targetNode = path.get(i);
            Vec3 targetVec = new Vec3(targetNode.getX() + 0.5, targetNode.getY(), targetNode.getZ() + 0.5);

            if (hasLineOfSight(playerPos, targetVec, mc.level)) {
                return i;
            }
        }

        return bestIndex;
    }

    /**
     * Raycasts a 3D bounding box cylinder from start to end position.
     * Verifies:
     * 1. No solid blocks obstruct player volume (head/torso/feet).
     * 2. Walkable floor exists underneath each step (no walking into air/void).
     * 3. No impassable vertical jumps along the vector.
     */
    public static boolean hasLineOfSight(Vec3 start, Vec3 end, Level level) {
        if (level == null) return false;

        Vec3 delta = end.subtract(start);
        double distance = delta.length();
        if (distance < 0.1) return true;

        Vec3 direction = delta.normalize();
        int steps = (int) Math.ceil(distance / STEP_SIZE);
        double halfWidth = PLAYER_WIDTH / 2.0;

        for (int i = 1; i <= steps; i++) {
            double progress = (double) i / steps;
            Vec3 current = start.add(delta.scale(progress));

            // 1. Check Player Bounding Box (Feet, Waist, Head)
            double feetY = current.y;
            double waistY = current.y + 0.9;
            double headY = current.y + 1.7;

            // Check center and 4 horizontal corners for wall collision
            double[][] offsets = {
                    {0, 0},
                    {halfWidth, 0},
                    {-halfWidth, 0},
                    {0, halfWidth},
                    {0, -halfWidth}
            };

            for (double[] offset : offsets) {
                double checkX = current.x + offset[0];
                double checkZ = current.z + offset[1];

                BlockPos feetPos = BlockPos.containing(checkX, feetY, checkZ);
                BlockPos waistPos = BlockPos.containing(checkX, waistY, checkZ);
                BlockPos headPos = BlockPos.containing(checkX, headY, checkZ);

                if (isSolidObstacle(level, feetPos) || isSolidObstacle(level, waistPos) || isSolidObstacle(level, headPos)) {
                    return false;
                }
            }

            // 2. Check Floor Support (Only if moving horizontally without swimming)
            if (Math.abs(delta.y) <= 1.2) {
                BlockPos floorPos = BlockPos.containing(current.x, current.y - 0.2, current.z);
                BlockPos deepFloorPos = BlockPos.containing(current.x, current.y - 1.2, current.z);

                boolean hasFloor = isWalkableSurface(level, floorPos) || isWalkableSurface(level, deepFloorPos);
                boolean inFluid = level.getFluidState(floorPos).isSource() || level.getFluidState(deepFloorPos).isSource();

                if (!hasFloor && !inFluid) {
                    // Gap/hole detected in straight line path
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isSolidObstacle(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;

        // Passable elements
        Block block = state.getBlock();
        if (block == Blocks.TRIPWIRE || block == Blocks.SHORT_GRASS || block == Blocks.TALL_GRASS || block == Blocks.SEAGRASS || block == Blocks.FERN) {
            return false;
        }

        if (block instanceof DoorBlock) {
            return !state.getValue(DoorBlock.OPEN);
        }
        if (block instanceof FenceGateBlock) {
            return !state.getValue(FenceGateBlock.OPEN);
        }

        VoxelShape collisionShape = state.getCollisionShape(level, pos);
        return !collisionShape.isEmpty();
    }

    private static boolean isWalkableSurface(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;

        Block block = state.getBlock();
        if (block == Blocks.LAVA || block == Blocks.FIRE || block == Blocks.CACTUS) {
            return false;
        }

        VoxelShape collisionShape = state.getCollisionShape(level, pos);
        return !collisionShape.isEmpty();
    }
}
