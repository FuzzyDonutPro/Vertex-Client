package com.vertexai.macro.impl.GardenVisitorMacro;

import lombok.Getter;
import com.vertexai.macro.AbstractMacro;
import com.vertexai.handler.RotationHandler;
import com.vertexai.util.InventoryUtil;
import com.vertexai.util.helper.Clock;
import com.vertexai.util.helper.RotationConfiguration;
import com.vertexai.util.helper.Target;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;

/**
 * GardenVisitorMacro — Automatically pathfinds to Garden Visitor NPCs, opens their trade menu,
 * accepts crop offers for Copper/EXP, and exchanges pests with Sam NPC.
 */
public class GardenVisitorMacro extends AbstractMacro {

    public static final GardenVisitorMacro instance = new GardenVisitorMacro();
    public static GardenVisitorMacro getInstance() { return instance; }

    private final Clock interactionClock = new Clock();
    private Entity targetVisitor = null;

    @Override
    public String getName() {
        return "Garden Visitor & Pest Trader";
    }

    @Override
    public List<String> getNecessaryItems() {
        return Collections.emptyList();
    }

    @Override
    public void enable() {
        super.enable();
        targetVisitor = null;
        log("Garden Visitor & Pest Trader Enabled! Will automatically trade with visitors and Sam NPC.");
    }

    @Override
    public void disable() {
        super.disable();
    }

    @Override
    public void onTick() {
        if (!isEnabled() || mc.player == null || mc.level == null) return;
        if (interactionClock.isScheduled() && !interactionClock.passed()) return;

        // If inside a visitor / trade container menu, click "Accept Offer"
        if (mc.screen instanceof AbstractContainerScreen<?> containerScreen) {
            String title = containerScreen.getTitle().getString().toLowerCase();
            if (title.contains("visitor") || title.contains("offer") || title.contains("sam") || title.contains("pest exchange")) {
                int containerId = containerScreen.getMenu().containerId;
                
                // Find "Accept Offer" or "Exchange Pests" slot
                for (int slot = 0; slot < containerScreen.getMenu().slots.size(); slot++) {
                    var stack = containerScreen.getMenu().getSlot(slot).getItem();
                    if (!stack.isEmpty()) {
                        String name = stack.getHoverName().getString().toLowerCase();
                        if (name.contains("accept") || name.contains("accept offer") || name.contains("exchange") || name.contains("claim")) {
                            mc.gameMode.handleContainerInput(containerId, slot, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, mc.player);
                            interactionClock.schedule(600);
                            return;
                        }
                    }
                }
            }
        }

        // Find nearest Visitor NPC or Sam in the Garden
        targetVisitor = findNearestVisitor();
        if (targetVisitor != null) {
            Vec3 pos = targetVisitor.position().add(0, targetVisitor.getEyeHeight() * 0.8, 0);
            double dist = mc.player.distanceTo(targetVisitor);

            RotationHandler.getInstance().easeTo(new RotationConfiguration(new Target(pos), 120, null));

            if (dist <= 4.0f && mc.gameMode != null) {
                mc.gameMode.interact(mc.player, targetVisitor, new net.minecraft.world.phys.EntityHitResult(targetVisitor), net.minecraft.world.InteractionHand.MAIN_HAND);
                mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                interactionClock.schedule(1000);
            }
        }
    }

    public Entity findNearestVisitor() {
        if (mc.level == null || mc.player == null) return null;
        Entity nearest = null;
        double nearestDist = 20.0;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player) continue;

            boolean isVisitor = (entity.getCustomName() != null && (
                entity.getCustomName().getString().toLowerCase().contains("visitor") ||
                entity.getCustomName().getString().toLowerCase().contains("sam")
            ));

            if (isVisitor && entity.isAlive() && !entity.isRemoved()) {
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
