package com.vertexai.macro.impl.combat;

import lombok.Getter;
import com.vertexai.macro.AbstractFeature;
import com.vertexai.util.InventoryUtil;
import com.vertexai.util.helper.Clock;

public class AutoBuffs extends AbstractFeature {

    @Getter
    public static final AutoBuffs instance = new AutoBuffs();

    private final Clock checkClock = new Clock();

    @Override
    public String getName() {
        return "AutoBuffs";
    }

    @Override
    protected void onTick() {
        if (mc.player == null || mc.level == null) return;
        if (checkClock.isScheduled() && !checkClock.passed()) return;

        checkClock.schedule(30000); // Check every 30 seconds

        // Check if player has Booster Cookie in hotbar to consume
        int cookieSlot = InventoryUtil.getHotbarSlotOfItem("Booster Cookie");
        if (cookieSlot != -1) {
            log("AutoBuffs: Found Booster Cookie in hotbar!");
            InventoryUtil.holdItem("Booster Cookie");
        }
    }
}
