package com.vertexai.macro.impl.DungeonMacro.states;

import com.vertexai.macro.impl.DungeonMacro.DungeonMacro;
import com.vertexai.dungeons.RoomTracker;
import com.vertexai.pathing.PathExecutor;
import com.vertexai.pathing.PathFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class SecretFinderState implements DungeonMacroState {

    private final PathExecutor pathExecutor = new PathExecutor();
    private BlockPos currentTargetPos = null;
    private Entity currentTargetEntity = null;
    private final List<BlockPos> clickedSecrets = new ArrayList<>();
    private int clickCooldown = 0;

    @Override
    public void onEnable(DungeonMacro macro) {
        currentTargetPos = null;
        currentTargetEntity = null;
        clickedSecrets.clear();
        pathExecutor.stopMovement();
        clickCooldown = 0;
    }

    @Override
    public void onTick(DungeonMacro macro) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        RoomTracker.DungeonRoom room = RoomTracker.getInstance().getCurrentRoom();
        if (room == null || room.secretsFound) {
            return;
        }

        if (clickCooldown > 0) {
            clickCooldown--;
            return;
        }

        if (currentTargetPos == null && currentTargetEntity == null) {
            findNextSecret(mc, room);
            if (currentTargetPos == null && currentTargetEntity == null) {
                // No more secrets found
                room.secretsFound = true;
                macro.setState(new ExploreState());
                return;
            }
        }

        // We have a target, tick path executor
        pathExecutor.tick();

        // Check distance to target to interact
        if (currentTargetPos != null) {
            Vec3 targetVec = new Vec3(currentTargetPos.getX() + 0.5, currentTargetPos.getY() + 0.5, currentTargetPos.getZ() + 0.5);
            if (mc.player.position().distanceTo(targetVec) < 4.0) {
                // Interact with block
                mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, 
                    new net.minecraft.world.phys.BlockHitResult(targetVec, net.minecraft.core.Direction.UP, currentTargetPos, false));
                mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                
                clickedSecrets.add(currentTargetPos);
                currentTargetPos = null;
                pathExecutor.stopMovement();
                clickCooldown = 20; // 1 second
            }
        } else if (currentTargetEntity != null) {
            if (!currentTargetEntity.isAlive()) {
                currentTargetEntity = null;
                pathExecutor.stopMovement();
            } else if (mc.player.distanceToSqr(currentTargetEntity) < 16.0) {
                // Attack Bat
                mc.gameMode.attack(mc.player, currentTargetEntity);
                mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                clickCooldown = 10;
            } else {
                // Update bat position dynamically?
                // For simplicity, re-calc path if needed, but bats move erratically.
                // In Dungeons, secrets are usually stationary except bats.
            }
        }
    }

    private void findNextSecret(Minecraft mc, RoomTracker.DungeonRoom room) {
        double minX = room.startX;
        double minZ = room.startZ;
        double maxX = room.startX + 32;
        double maxZ = room.startZ + 32;

        AABB roomBounds = new AABB(minX, 0, minZ, maxX, 256, maxZ);

        // 1. Scan for unclicked Chests / Trapped Chests
        for (int cx = (int)minX >> 4; cx <= (int)maxX >> 4; cx++) {
            for (int cz = (int)minZ >> 4; cz <= (int)maxZ >> 4; cz++) {
                net.minecraft.world.level.chunk.LevelChunk chunk = mc.level.getChunk(cx, cz);
                if (chunk != null) {
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        if (be instanceof ChestBlockEntity || be instanceof TrappedChestBlockEntity) {
                            BlockPos pos = be.getBlockPos();
                            if (roomBounds.contains(pos.getX(), pos.getY(), pos.getZ()) && !clickedSecrets.contains(pos)) {
                                currentTargetPos = pos;
                                List<BlockPos> path = PathFinder.findPath(mc.level, mc.player.blockPosition(), currentTargetPos, 5000);
                                if (path != null && !path.isEmpty()) {
                                    pathExecutor.setPath(path);
                                    return;
                                } else {
                                    // Unreachable, add to clicked so we skip it
                                    clickedSecrets.add(pos);
                                    currentTargetPos = null;
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Scan for Bats
        List<Bat> bats = mc.level.getEntitiesOfClass(Bat.class, roomBounds, Entity::isAlive);
        if (!bats.isEmpty()) {
            currentTargetEntity = bats.get(0);
            List<BlockPos> path = PathFinder.findPath(mc.level, mc.player.blockPosition(), currentTargetEntity.blockPosition(), 5000);
            if (path != null && !path.isEmpty()) {
                pathExecutor.setPath(path);
                return;
            } else {
                currentTargetEntity = null;
            }
        }
    }

    @Override
    public void onDisable(DungeonMacro macro) {
        pathExecutor.stopMovement();
        currentTargetPos = null;
        currentTargetEntity = null;
    }

    @Override
    public String getName() {
        return "Finding Secrets";
    }
}
