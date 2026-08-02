package com.vertexai.pathing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PartialBlockHelper {

    /**
     * Calculates high-precision sub-pixel Vec3 coordinate for a given BlockPos
     * considering partial block heights (Slabs, Carpet, Snow, Fences, Trapdoors).
     */
    public static Vec3 getSubPixelCenter(Level world, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        double y = pos.getY() + getStandingHeightOffset(world, pos);

        return new Vec3(x, y, z);
    }

    public static double getStandingHeightOffset(Level world, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState state = world.getBlockState(below);

        if (state.isAir()) return 0.0;

        VoxelShape shape = state.getCollisionShape(world, below);
        if (shape.isEmpty()) return 0.0;

        // Sub-pixel top Y height of the standing block
        return shape.max(net.minecraft.core.Direction.Axis.Y);
    }

    /**
     * Checks if a block is a non-solid / partial block that can be walked through.
     */
    public static boolean isPassablePartialBlock(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        // Cocoa Beans are partial blocks attached to trunks — non-collidable or tiny box
        if (block instanceof CocoaBlock) return true;

        // Open Trapdoors / Open Fence Gates
        if (state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN)) {
            return true;
        }

        // Non-obstructing decorative / utility blocks
        if (block instanceof LadderBlock || block instanceof VineBlock ||
            block instanceof FlowerBlock || block instanceof TallGrassBlock ||
            block instanceof BushBlock || state.is(Blocks.TRIPWIRE)) {
            return true;
        }

        VoxelShape shape = state.getCollisionShape(world, pos);
        return shape.isEmpty();
    }
}
