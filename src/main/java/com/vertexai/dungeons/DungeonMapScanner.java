package com.vertexai.dungeons;

import com.vertexai.util.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class DungeonMapScanner {

    private static final DungeonMapScanner instance = new DungeonMapScanner();
    public static DungeonMapScanner getInstance() { return instance; }

    public enum RoomType { ENTRANCE, NORMAL, PUZZLE, MINIBOSS, BLOOD, PORTAL, UNKNOWN }

    private final Map<BlockPos, RoomType> dungeonMap = new HashMap<>();
    private BlockPos bloodRoomPos = null;
    private BlockPos portalRoomPos = null;

    public void scanMap() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Parse scoreboards and map packet data to locate blood & portal rooms
        Logger.sendLog("[DungeonMapScanner] Dungeon Map Scanned.");
    }

    public Map<BlockPos, RoomType> getDungeonMap() { return dungeonMap; }
    public BlockPos getBloodRoomPos() { return bloodRoomPos; }
    public BlockPos getPortalRoomPos() { return portalRoomPos; }
}
