package com.vertexai.macro.features.farming;

import lombok.Getter;
import com.vertexai.macro.AbstractFeature;
import com.vertexai.util.helper.Clock;
import net.minecraft.world.entity.LivingEntity;

import java.util.stream.StreamSupport;

public class AutoPestExchange extends AbstractFeature {

    @Getter
    public static final AutoPestExchange instance = new AutoPestExchange();

    private final Clock cooldown = new Clock();

    @Override
    public String getName() {
        return "AutoPestExchange";
    }

    @Override
    protected void onTick() {
        if (mc.player == null || mc.level == null) return;
        if (cooldown.isScheduled() && !cooldown.passed()) return;

        // Search for Phillip NPC within 5 blocks in Garden
        LivingEntity phillip = StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false)
                .filter(e -> e instanceof LivingEntity && e != mc.player)
                .map(e -> (LivingEntity) e)
                .filter(e -> e.getName().getString().toLowerCase().contains("phillip"))
                .filter(e -> mc.player.distanceTo(e) <= 5.0f)
                .findFirst()
                .orElse(null);

        if (phillip != null) {
            log("AutoPestExchange: Found Phillip NPC! Exchanging pests...");
            if (mc.gameMode != null) {
                mc.gameMode.interact(mc.player, phillip, net.minecraft.world.InteractionHand.MAIN_HAND);
            }
            cooldown.schedule(10000); // 10s cooldown
        }
    }
}
