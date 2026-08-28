package com.vertexai.macro.impl.PestHunterMacro;

import lombok.Getter;
import com.vertexai.macro.AbstractMacro;
import com.vertexai.handler.GameStateHandler;
import com.vertexai.handler.RotationHandler;
import com.vertexai.pathfinder.calculate.FluidAndFlyingPathfinder;
import com.vertexai.util.InventoryUtil;
import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.helper.Clock;
import com.vertexai.util.helper.RotationConfiguration;
import com.vertexai.util.helper.Target;
import com.vertexai.util.helper.location.Location;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;

/**
 * PestHunterMacro — Automates tracking, aiming, vacuum switching, and holding right-click
 * until Garden Pests (Fly, Beetle, Locust, Slug, Mite) are completely exterminated.
 */
public class PestHunterMacro extends AbstractMacro {

    public static final PestHunterMacro instance = new PestHunterMacro();
    public static PestHunterMacro getInstance() { return instance; }

    private final Clock attackClock = new Clock();
    private Entity targetPest = null;
    private boolean isVacuuming = false;

    @Override
    public String getName() {
        return "Pest Exterminator";
    }

    @Override
    public List<String> getNecessaryItems() {
        return Collections.emptyList();
    }

    /**
     * Finds the best Vacuum item in the player's hotbar in hierarchy order:
     * 1. InfiniVacuum
     * 2. SkyMart Vacuum
     * 3. Vacuum
     */
    public int getBestVacuumSlot() {
        int slot = InventoryUtil.getHotbarSlotOfItem("InfiniVacuum");
        if (slot != -1) return slot;

        slot = InventoryUtil.getHotbarSlotOfItem("SkyMart Vacuum");
        if (slot != -1) return slot;

        slot = InventoryUtil.getHotbarSlotOfItem("Vacuum");
        if (slot != -1) return slot;

        // Fallback: Check upper inventory (slots 9-35) and auto-swap into hotbar slot 8
        // Only send the swap packet when the player inventory screen is actually open server-side
        int invSlot = InventoryUtil.getSlotOfItemInMainInventory("Vacuum");
        if (invSlot != -1 && mc.gameMode != null && mc.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen) {
            mc.gameMode.handleContainerInput(
                mc.player.inventoryMenu.containerId, invSlot, 8, net.minecraft.world.inventory.ContainerInput.SWAP, mc.player);
            return 8;
        }

        return -1;
    }

    public boolean canFlyInCurrentLocation() {
        if (mc.player == null) return false;

        if (mc.player.getAbilities().mayfly || mc.player.getAbilities().flying) {
            return true;
        }

        boolean hasCookie = GameStateHandler.getInstance().isCookieActive();
        Location loc = GameStateHandler.getInstance().getCurrentLocation();
        boolean isPrivateZone = (loc == Location.GARDEN || loc == Location.PRIVATE_ISLAND);

        return hasCookie && isPrivateZone;
    }

    @Override
    public void enable() {
        super.enable();
        isVacuuming = false;
        boolean canFly = canFlyInCurrentLocation();
        log("PestHunterMacro: Enabled! Flying mode: " + (canFly ? "ENABLED (Booster Cookie & Garden)" : "DISABLED (Ground Only)"));
    }

    @Override
    public void disable() {
        super.disable();
        if (mc.options != null) {
            KeyBindUtil.setKeyBindState(mc.options.keyUse, false);
            KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);
        }
        isVacuuming = false;
    }

    @Override
    public void onTick() {
        if (!isEnabled() || mc.player == null || mc.level == null) {
            stopVacuuming();
            return;
        }

        // Validate current target pest state
        if (targetPest != null && (!targetPest.isAlive() || targetPest.isRemoved())) {
            targetPest = null;
            stopVacuuming();
        }

        targetPest = findNearestPest();

        if (targetPest != null) {
            Vec3 targetPos = targetPest.position().add(0, targetPest.getEyeHeight() * 0.5, 0);

            // 1. Navigation / Flight
            if (canFlyInCurrentLocation()) {
                RotationHandler.getInstance().easeTo(new RotationConfiguration(new Target(targetPos), 100, null));
                if (!mc.player.getAbilities().flying && mc.player.getAbilities().mayfly) {
                    mc.player.getAbilities().flying = true;
                }
            } else {
                RotationHandler.getInstance().easeTo(new RotationConfiguration(new Target(targetPos), 120, null));
            }

            // 2. Vacuum Switch & Hold Right-Click till Pest Disappears
            double dist = mc.player.distanceTo(targetPest);
            if (dist <= 6.0f) {
                int vacSlot = getBestVacuumSlot();
                if (vacSlot != -1) {
                    mc.player.getInventory().setSelectedSlot(vacSlot);
                    // Hold Right Click continuously for Vacuum suction!
                    KeyBindUtil.setKeyBindState(mc.options.keyUse, true);
                    isVacuuming = true;
                } else {
                    // Fallback melee attack if no vacuum found (Survival reach <= 2.9 blocks)
                    if (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit && hit.getEntity() == targetPest && dist <= 2.9f && mc.gameMode != null && (!attackClock.isScheduled() || attackClock.passed())) {
                        mc.gameMode.attack(mc.player, targetPest);
                        mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                        attackClock.schedule(150 + (long)(Math.random() * 80));
                    }
                }
            } else {
                stopVacuuming();
            }
        } else {
            stopVacuuming();
        }
    }

    private void stopVacuuming() {
        if (isVacuuming && mc.options != null) {
            KeyBindUtil.setKeyBindState(mc.options.keyUse, false);
            isVacuuming = false;
        }
    }

    private Entity findNearestPest() {
        if (mc.level == null || mc.player == null) return null;
        Entity nearest = null;
        double nearestDist = 35.0;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player) continue;
            boolean isPest = entity instanceof Silverfish || entity instanceof Slime ||
                             (entity.getCustomName() != null && entity.getCustomName().getString().toLowerCase().contains("pest"));

            if (isPest && entity.isAlive() && !entity.isRemoved()) {
                double dist = mc.player.distanceTo(entity);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = entity;
                }
            }
        }
        return nearest;
    }
}
