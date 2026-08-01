package com.vertexai.macro.impl.DianaBurrowMacro;

import lombok.Getter;
import com.vertexai.macro.AbstractMacro;
import com.vertexai.handler.RotationHandler;
import com.vertexai.util.InventoryUtil;
import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.helper.Clock;
import com.vertexai.util.helper.RotationConfiguration;
import com.vertexai.util.helper.Target;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;

/**
 * DianaBurrowMacro — Diana Mythological Event Burrow Finder & Combat Macro.
 * Detects Ancestral Spade particle burrows, pathfinds to dig sites, digs up burrows,
 * and automatically targets spawned Mythological Mobs (Inquisitor, Minos Champion, Minotaur).
 */
public class DianaBurrowMacro extends AbstractMacro {

    private static final DianaBurrowMacro instance = new DianaBurrowMacro();
    public static DianaBurrowMacro getInstance() { return instance; }

    private final Clock actionClock = new Clock();
    private BlockPos currentBurrowPos = null;
    private Entity targetMythicalMob = null;

    @Override
    public String getName() {
        return "Diana Mythological Burrow Finder";
    }

    @Override
    public List<String> getNecessaryItems() {
        return Collections.emptyList();
    }

    @Override
    public void enable() {
        super.enable();
        currentBurrowPos = null;
        targetMythicalMob = null;
        log("Diana Mythological Burrow Finder Enabled! Hold or equip Ancestral Spade.");
    }

    @Override
    public void disable() {
        super.disable();
        if (mc.options != null) {
            KeyBindUtil.setKeyBindState(mc.options.keyUse, false);
            KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);
        }
    }

    @Override
    public void onTick() {
        if (!isEnabled() || mc.player == null || mc.level == null) return;
        if (actionClock.isScheduled() && !actionClock.passed()) return;

        // Priority 1: Target and kill spawned Mythological Mobs
        targetMythicalMob = findNearestMythicalMob();
        if (targetMythicalMob != null) {
            Vec3 mobPos = targetMythicalMob.position().add(0, targetMythicalMob.getEyeHeight() * 0.5, 0);
            double dist = mc.player.distanceTo(targetMythicalMob);

            RotationHandler.getInstance().easeTo(new RotationConfiguration(new Target(mobPos), 100, null));

            // Equip weapon (Daedalus Axe / Sword)
            int weaponSlot = InventoryUtil.getHotbarSlotOfItem("Daedalus Axe");
            if (weaponSlot == -1) weaponSlot = InventoryUtil.getHotbarSlotOfItem("Sword");
            if (weaponSlot != -1) mc.player.getInventory().setSelectedSlot(weaponSlot);

            if (dist <= 3.5f && mc.gameMode != null) {
                mc.gameMode.attack(mc.player, targetMythicalMob);
                mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                actionClock.schedule(200);
            }
            return;
        }

        // Priority 2: Equip Ancestral Spade to pulse/locate burrows
        int spadeSlot = InventoryUtil.getHotbarSlotOfItem("Ancestral Spade");
        if (spadeSlot != -1) {
            mc.player.getInventory().setSelectedSlot(spadeSlot);
        }

        // Right click spade to refresh particle trail
        if (currentBurrowPos == null) {
            KeyBindUtil.setKeyBindState(mc.options.keyUse, true);
            actionClock.schedule(300);
            KeyBindUtil.setKeyBindState(mc.options.keyUse, false);
        }
    }

    private Entity findNearestMythicalMob() {
        if (mc.level == null || mc.player == null) return null;
        Entity nearest = null;
        double nearestDist = 30.0;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player) continue;

            if (entity.getCustomName() != null) {
                String name = entity.getCustomName().getString().toLowerCase();
                boolean isMythical = name.contains("minos") || name.contains("inquisitor") ||
                                     name.contains("minotaur") || name.contains("gaia") ||
                                     name.contains("siamese");

                if (isMythical && entity.isAlive() && !entity.isRemoved()) {
                    double dist = mc.player.distanceTo(entity);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = entity;
                    }
                }
            }
        }
        return nearest;
    }
}
