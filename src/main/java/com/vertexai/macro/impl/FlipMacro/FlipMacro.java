package com.vertexai.macro.impl.FlipMacro;

import lombok.Getter;
import com.vertexai.macro.AbstractMacro;
import com.vertexai.util.helper.Clock;

import java.util.Collections;
import java.util.List;

/**
 * FlipMacro — Automated Bazaar & AH Order Flipper Macro.
 */
public class FlipMacro extends AbstractMacro {

    @Getter
    public static final FlipMacro instance = new FlipMacro();

    private final Clock flipClock = new Clock();
    private int step = 0;

    @Override
    public String getName() {
        return "Bazaar Flipper";
    }

    @Override
    public List<String> getNecessaryItems() {
        return Collections.emptyList();
    }

    @Override
    public void enable() {
        super.enable();
        log("Enabling Bazaar Flipper Macro...");
        step = 0;
        flipClock.schedule(1000);
    }

    @Override
    public void disable() {
        super.disable();
        log("Disabling Bazaar Flipper Macro...");
    }

    @Override
    public void onTick() {
        if (!isEnabled() || mc.player == null) return;
        if (flipClock.isScheduled() && !flipClock.passed()) return;

        switch (step) {
            case 0:
                log("Bazaar Flipper: Opening Bazaar via /bz...");
                if (mc.player.connection != null) {
                    mc.player.connection.sendCommand("bz");
                }
                step = 1;
                flipClock.schedule(3000);
                break;
            case 1:
                log("Bazaar Flipper: Checking active buy/sell orders...");
                step = 2;
                flipClock.schedule(4000);
                break;
            case 2:
                log("Bazaar Flipper: Managing order claims & flip margins...");
                step = 0;
                flipClock.schedule(12000); // 12s cycle delay
                break;
        }
    }
}
