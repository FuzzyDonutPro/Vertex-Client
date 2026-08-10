package com.vertexai.feature.impl;

import lombok.Getter;
import com.vertexai.feature.AbstractFeature;
import com.vertexai.util.helper.Clock;

public class PetSwapper extends AbstractFeature {

    @Getter
    public static final PetSwapper instance = new PetSwapper();

    private final Clock cooldown = new Clock();

    @Override
    public String getName() {
        return "PetSwapper";
    }

    public void equipPet(String petName) {
        if (mc.player == null || petName == null || petName.trim().isEmpty()) return;
        if (cooldown.isScheduled() && !cooldown.passed()) return;

        log("PetSwapper: Executing /pets command for " + petName + "...");
        if (mc.player.connection != null) {
            mc.player.connection.sendCommand("pets");
        }
        cooldown.schedule(5000);
    }
}
