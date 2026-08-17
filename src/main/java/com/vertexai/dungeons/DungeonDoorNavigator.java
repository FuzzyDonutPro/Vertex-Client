package com.vertexai.dungeons;

import com.vertexai.Vertex;
import com.vertexai.feature.impl.PathExecutor;
import com.vertexai.feature.impl.Pathfinder;
import com.vertexai.handler.RotationHandler;
import com.vertexai.util.AngleUtil;
import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.Logger;
import com.vertexai.util.helper.Angle;
import com.vertexai.util.helper.Clock;
import com.vertexai.util.helper.RotationConfiguration;
import com.vertexai.util.helper.Target;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * DungeonDoorNavigator
 * <p>
 * Handles room-to-room navigation, door identification (Normal, Wither, Blood),
 * Wither/Blood key collection, door unlocking, and pathfinding into new rooms.
 */
public class DungeonDoorNavigator {

    private static final DungeonDoorNavigator instance = new DungeonDoorNavigator();
    public static DungeonDoorNavigator getInstance() { return instance; }

    private final Minecraft mc = Minecraft.getInstance();
    private final Clock actionClock = new Clock();
    private BlockPos targetDoorPos = null;
    private Entity targetKeyEntity = null;
    private boolean isNavigating = false;

    public void reset() {
        this.targetDoorPos = null;
        this.targetKeyEntity = null;
        this.isNavigating = false;
        this.actionClock.reset();
    }

    /**
     * Executes one tick of door navigation and key collection.
     * @return true if currently navigating or handling door/key
     */
    public boolean onTick() {
        if (mc.player == null || mc.level == null) return false;

        // 1. Key Collection: Check for dropped Wither or Blood keys in the room
        if (Vertex.config().dungeons.autoKeyCollector) {
            Entity key = findNearestKey();
            if (key != null) {
                this.targetKeyEntity = key;
                double distSq = mc.player.distanceToSqr(key);
                if (distSq > 4.0) {
                    if (!PathExecutor.getInstance().isEnabled() && actionClock.passed()) {
                        actionClock.schedule(800);
                        BlockPos keyPos = key.blockPosition();
                        Logger.sendMessage("§6[Dungeon Door] Moving to collect " + key.getDisplayName().getString() + "...");
                        Pathfinder.getInstance().stopAndRequeue(keyPos);
                        Pathfinder.getInstance().start();
                    }
                    return true;
                } else {
                    // In range, picked up key
                    this.targetKeyEntity = null;
                }
            }
        }

        // 2. Door Exploration & Navigation
        if (!Vertex.config().dungeons.autoDoorNavigation) return false;

        RoomTracker.DungeonRoom currentRoom = RoomTracker.getInstance().getCurrentRoom();
        if (currentRoom == null) return false;

        // Find potential door portals on the 4 walls of the 32x32 room
        if (this.targetDoorPos == null || !PathExecutor.getInstance().isEnabled()) {
            List<BlockPos> potentialDoors = findRoomDoorways(currentRoom);
            if (!potentialDoors.isEmpty()) {
                // Pick closest unvisited doorway
                potentialDoors.sort(Comparator.comparingDouble(p -> p.distToCenterSqr(mc.player.position())));
                this.targetDoorPos = potentialDoors.get(0);
            }
        }

        if (this.targetDoorPos != null) {
            double distSq = this.targetDoorPos.distToCenterSqr(mc.player.position());
            
            // Check if door is a locked Wither / Blood door that needs interaction
            if (distSq <= 16.0 && isDoorLocked(this.targetDoorPos)) {
                if (actionClock.passed()) {
                    actionClock.schedule(500);
                    Logger.sendMessage("§e[Dungeon Door] Unlocking Wither / Blood Door...");
                    // Aim at center and click
                    float yaw = AngleUtil.getRotationYaw(Vec3.atCenterOf(this.targetDoorPos));
                    float pitch = AngleUtil.getRotation(Vec3.atCenterOf(this.targetDoorPos)).getPitch();
                    RotationHandler.getInstance().easeTo(new RotationConfiguration(new Target(new Angle(yaw, pitch)), 120, RotationConfiguration.RotationType.CLIENT, () -> {
                        KeyBindUtil.rightClick();
                    }));
                }
                return true;
            }

            // Pathfind through the door into adjacent room center
            if (!PathExecutor.getInstance().isEnabled() && actionClock.passed()) {
                actionClock.schedule(1000);
                BlockPos targetRoomCenter = calculateAdjacentRoomCenter(currentRoom, this.targetDoorPos);
                Logger.sendMessage("§b[Dungeon Door] Navigating through doorway to next room (" + targetRoomCenter.getX() + ", " + targetRoomCenter.getZ() + ")...");
                Pathfinder.getInstance().stopAndRequeue(targetRoomCenter);
                Pathfinder.getInstance().start();
                this.isNavigating = true;
                return true;
            }
        }

        return isNavigating && PathExecutor.getInstance().isEnabled();
    }

    private Entity findNearestKey() {
        if (mc.level == null || mc.player == null) return null;
        AABB searchBox = mc.player.getBoundingBox().inflate(24.0, 8.0, 24.0);

        // Check for named ArmorStands or Item entities with Key names
        List<Entity> candidates = mc.level.getEntities(mc.player, searchBox, e -> {
            if (e instanceof ArmorStand || e instanceof ItemEntity) {
                String name = e.getDisplayName().getString().toLowerCase();
                return name.contains("wither key") || name.contains("blood key");
            }
            return false;
        });

        if (candidates.isEmpty()) return null;
        candidates.sort(Comparator.comparingDouble(e -> e.distanceToSqr(mc.player)));
        return candidates.get(0);
    }

    private List<BlockPos> findRoomDoorways(RoomTracker.DungeonRoom room) {
        List<BlockPos> doorways = new ArrayList<>();
        if (mc.level == null || mc.player == null) return doorways;

        int playerY = mc.player.getBlockY();
        int minX = room.startX;
        int minZ = room.startZ;

        // Check North, South, East, West center portals (at offset 15-16 along edges)
        BlockPos[] centerWallPoints = new BlockPos[] {
                new BlockPos(minX + 16, playerY, minZ),        // North Wall
                new BlockPos(minX + 16, playerY, minZ + 31),   // South Wall
                new BlockPos(minX, playerY, minZ + 16),        // West Wall
                new BlockPos(minX + 31, playerY, minZ + 16)    // East Wall
        };

        for (BlockPos wallPos : centerWallPoints) {
            // Check if there is an opening (air/door/passable corridor)
            if (isPassableDoorway(wallPos)) {
                doorways.add(wallPos);
            }
        }

        return doorways;
    }

    private boolean isPassableDoorway(BlockPos pos) {
        if (mc.level == null) return false;
        // Check for 3-block high opening or door material
        BlockState state1 = mc.level.getBlockState(pos);
        BlockState state2 = mc.level.getBlockState(pos.above());
        return state1.isAir() || state2.isAir() || isDoorBlock(state1) || isDoorBlock(state2);
    }

    private boolean isDoorBlock(BlockState state) {
        return state.is(Blocks.COAL_BLOCK) || state.is(Blocks.RED_TERRACOTTA) || state.is(Blocks.IRON_DOOR) || state.is(Blocks.OAK_DOOR);
    }

    private boolean isDoorLocked(BlockPos pos) {
        if (mc.level == null) return false;
        BlockState state = mc.level.getBlockState(pos);
        return state.is(Blocks.COAL_BLOCK) || state.is(Blocks.RED_TERRACOTTA);
    }

    private BlockPos calculateAdjacentRoomCenter(RoomTracker.DungeonRoom currentRoom, BlockPos doorPos) {
        int targetX = currentRoom.startX + 16;
        int targetZ = currentRoom.startZ + 16;

        if (doorPos.getX() >= currentRoom.startX + 28) {
            targetX = currentRoom.startX + 48; // East adjacent room
        } else if (doorPos.getX() <= currentRoom.startX + 4) {
            targetX = currentRoom.startX - 16; // West adjacent room
        } else if (doorPos.getZ() >= currentRoom.startZ + 28) {
            targetZ = currentRoom.startZ + 48; // South adjacent room
        } else if (doorPos.getZ() <= currentRoom.startZ + 4) {
            targetZ = currentRoom.startZ - 16; // North adjacent room
        }

        return new BlockPos(targetX, mc.player.getBlockY(), targetZ);
    }
}
