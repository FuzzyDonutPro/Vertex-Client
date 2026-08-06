package com.vertexai.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;

/**
 * PacketUtil — Helper utility for zero-latency network packet dispatching, block destruction packets,
 * and connection status checks.
 */
public class PacketUtil {

    private static final Minecraft mc = Minecraft.getInstance();

    public static boolean sendPacket(Packet<?> packet) {
        if (mc.player == null || mc.player.connection == null || packet == null) {
            return false;
        }
        mc.player.connection.send(packet);
        return true;
    }

    public static void sendStartDestroyBlock(BlockPos pos, Direction direction) {
        sendPacket(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                pos,
                direction
        ));
    }

    public static void sendStopDestroyBlock(BlockPos pos, Direction direction) {
        sendPacket(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                pos,
                direction
        ));
    }

    public static void sendAbortDestroyBlock(BlockPos pos, Direction direction) {
        sendPacket(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                pos,
                direction
        ));
    }
}
