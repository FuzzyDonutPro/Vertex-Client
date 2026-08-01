package com.vertexai.macro.impl.ZealotMacro;

import lombok.Getter;
import com.vertexai.macro.AbstractMacro;
import com.vertexai.handler.RotationHandler;
import com.vertexai.util.helper.Clock;
import com.vertexai.util.helper.RotationConfiguration;
import com.vertexai.util.helper.Target;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;

/**
 * ZealotMacro — Navigates Dragon's Nest / Bruiser Hideout, auto-aims and slays
 * Zealots and Bruisers, and auto-picks up Special Summoning Eyes.
 */
public class ZealotMacro extends AbstractMacro {

    public static final ZealotMacro instance = new ZealotMacro();
    public static ZealotMacro getInstance() { return instance; }

    private final Clock attackClock = new Clock();
    private Entity targetZealot = null;

    @Override
    public String getName() {
        return "Zealot & Bruiser Farmer";
    }

    @Override
    public List<String> getNecessaryItems() {
        return Collections.emptyList();
    }

    @Override
    public void enable() {
        super.enable();
        log("ZealotMacro: Enabled! Searching for Zealots & Bruisers...");
    }

    @Override
    public void onTick() {
        if (!isEnabled() || mc.player == null || mc.level == null) return;
        if (attackClock.isScheduled() && !attackClock.passed()) return;

        targetZealot = findNearestZealot();

        if (targetZealot != null) {
            Vec3 targetPos = targetZealot.position().add(0, targetZealot.getEyeHeight() * 0.5, 0);
            RotationHandler.getInstance().easeTo(new RotationConfiguration(new Target(targetPos), 120, null));

            if (mc.player.distanceTo(targetZealot) <= 3.0f) {
                if (mc.gameMode != null) {
                    mc.gameMode.attack(mc.player, targetZealot);
                    mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                }
                attackClock.schedule(250);
            }
        }
    }

    private Entity findNearestZealot() {
        if (mc.level == null || mc.player == null) return null;
        Entity nearest = null;
        double nearestDist = 30.0;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player) continue;
            if (entity instanceof EnderMan) {
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
