package com.vertexai.macro.impl.DungeonMacro.states;

import com.vertexai.macro.impl.DungeonMacro.DungeonMacro;
import com.vertexai.dungeons.RoomTracker;
import com.vertexai.handler.RotationHandler;
import com.vertexai.util.helper.RotationConfiguration;
import com.vertexai.util.helper.Target;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

public class AutoClearState implements DungeonMacroState {

    private Entity currentTarget;
    private int attackCooldown = 0;

    @Override
    public void onEnable(DungeonMacro macro) {
        currentTarget = null;
        attackCooldown = 0;
    }

    @Override
    public void onTick(DungeonMacro macro) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        RoomTracker.DungeonRoom room = RoomTracker.getInstance().getCurrentRoom();
        if (room == null || room.isCleared) {
            // Nothing to clear, or already cleared
            return;
        }

        if (attackCooldown > 0) {
            attackCooldown--;
        }

        // Check if current target is dead or invalid
        if (currentTarget != null && (!currentTarget.isAlive() || currentTarget.isRemoved())) {
            currentTarget = null;
            RotationHandler.getInstance().stop();
        }

        if (currentTarget == null) {
            // Find nearest hostile mob in the room
            // A DungeonRoom is 32x32 blocks, from startX, startZ
            double minX = room.startX;
            double minZ = room.startZ;
            double maxX = room.startX + 32;
            double maxZ = room.startZ + 32;
            
            AABB roomBounds = new AABB(minX, 0, minZ, maxX, 256, maxZ);
            
            List<Monster> monsters = mc.level.getEntitiesOfClass(Monster.class, roomBounds, Entity::isAlive);
            if (monsters.isEmpty()) {
                room.isCleared = true;
                macro.setState(new ClearRoomState());
                return;
            }
            
            // Sort by distance
            monsters.sort(Comparator.comparingDouble(m -> m.distanceToSqr(mc.player)));
            currentTarget = monsters.get(0);
        }

        if (currentTarget != null) {
            // Pathfind to target if too far? 
            // For now, assume player handles pathing or we just rotate and attack if in range
            double distSq = mc.player.distanceToSqr(currentTarget);
            
            // Look at target
            if (!RotationHandler.getInstance().isEnabled()) {
                RotationHandler.getInstance().queueRotation(
                    new RotationConfiguration(new Target(currentTarget), 150, RotationConfiguration.RotationType.CLIENT, () -> {})
                ).start();
            }

            // Attack if in range
            if (distSq <= 16.0 && attackCooldown <= 0) { // 4 block range squared = 16
                // Verify crosshair is actually on it before clicking?
                // For simplicity, just simulate click
                mc.gameMode.attack(mc.player, currentTarget);
                mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                attackCooldown = 10; // 0.5 sec cooldown
            }
        }
    }

    @Override
    public void onDisable(DungeonMacro macro) {
        RotationHandler.getInstance().stop();
        currentTarget = null;
    }

    @Override
    public String getName() {
        return "Auto Clearing";
    }
}
