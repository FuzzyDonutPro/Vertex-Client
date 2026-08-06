package com.vertexai.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * FastMathUtil — High-performance math utility with pre-computed trigonometric lookup tables,
 * fast inverse square roots, bitwise packing, and zero-allocation vector math.
 */
public class FastMathUtil {

    private static final float[] SIN_TABLE = new float[65536];

    static {
        for (int i = 0; i < 65536; ++i) {
            SIN_TABLE[i] = (float) Math.sin((double) i * Math.PI * 2.0 / 65536.0);
        }
    }

    public static float sin(float value) {
        return SIN_TABLE[(int) (value * 10430.378F) & 65535];
    }

    public static float cos(float value) {
        return SIN_TABLE[(int) (value * 10430.378F + 16384.0F) & 65535];
    }

    public static double distanceSq(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }

    public static double distanceSq(Vec3 v1, Vec3 v2) {
        return distanceSq(v1.x, v1.y, v1.z, v2.x, v2.y, v2.z);
    }

    public static double distanceSq(BlockPos p1, BlockPos p2) {
        return distanceSq(p1.getX(), p1.getY(), p1.getZ(), p2.getX(), p2.getY(), p2.getZ());
    }

    public static float wrapDegrees(float value) {
        float wrapped = value % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        }
        if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }
        return wrapped;
    }

    public static double clamp(double num, double min, double max) {
        if (num < min) return min;
        return Math.min(num, max);
    }

    public static int clamp(int num, int min, int max) {
        if (num < min) return min;
        return Math.min(num, max);
    }

    public static long packBlockPos(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | (long) (z & 0x3FFFFFF);
    }
}
