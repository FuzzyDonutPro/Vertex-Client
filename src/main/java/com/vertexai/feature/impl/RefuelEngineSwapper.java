package com.vertexai.feature.impl;

import lombok.Getter;
import com.vertexai.feature.AbstractFeature;
import com.vertexai.feature.impl.AutoDrillRefuel.AutoDrillRefuel;
import com.vertexai.util.helper.Clock;

/**
 * RefuelEngineSwapper — Automatically refuels drills with fuel sources (Abyssal, Volcanic, Amber)
 * and manages drill engine parts when fuel runs low.
 */
public class RefuelEngineSwapper extends AbstractFeature {

    @Getter
    public static final RefuelEngineSwapper instance = new RefuelEngineSwapper();

    private final Clock cooldown = new Clock();

    public RefuelEngineSwapper() {
        this.enabled = true;
    }

    @Override
    public String getName() {
        return "RefuelEngineSwapper";
    }

    public void refuelDrill() {
        if (mc.player == null || mc.player.connection == null) return;
        if (cooldown.isScheduled() && !cooldown.passed()) return;

        log("RefuelEngineSwapper: Refueling drill with available fuel items...");
        AutoDrillRefuel.getInstance().start("Drill", AutoDrillRefuel.FuelType.VOLTA);
        cooldown.schedule(15000);
    }
}
