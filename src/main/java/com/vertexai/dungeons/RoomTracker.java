package com.vertexai.dungeons;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks the player's position within a procedurally generated dungeon.
 * Maps coordinates to specific room bounds and states.
 */
public class RoomTracker {

    private static final RoomTracker instance = new RoomTracker();
    private final Map<String, DungeonRoom> discoveredRooms = new HashMap<>();
    private DungeonRoom currentRoom = null;

    public static RoomTracker getInstance() {
        return instance;
    }

    public void update(Minecraft mc) {
        if (mc.player == null) return;
        BlockPos pos = mc.player.blockPosition();

        // Basic 32x32 chunk snapping (Hypixel dungeon rooms are strictly 32x32 blocks)
        int gridX = (pos.getX() / 32) * 32;
        int gridZ = (pos.getZ() / 32) * 32;
        String roomId = gridX + "," + gridZ;

        if (!discoveredRooms.containsKey(roomId)) {
            DungeonRoom newRoom = new DungeonRoom(gridX, gridZ);
            discoveredRooms.put(roomId, newRoom);
            this.currentRoom = newRoom;
        } else {
            this.currentRoom = discoveredRooms.get(roomId);
        }
    }

    public DungeonRoom getCurrentRoom() {
        return currentRoom;
    }

    public void reset() {
        discoveredRooms.clear();
        currentRoom = null;
    }

    public static class DungeonRoom {
        public int startX, startZ;
        public boolean isCleared = false;
        public boolean secretsFound = false;
        
        public DungeonRoom(int x, int z) {
            this.startX = x;
            this.startZ = z;
        }
    }
}
