package com.vertexai.macro.features.misc;

import lombok.Getter;
import com.vertexai.macro.AbstractFeature;
import com.vertexai.macro.MacroManager;
import com.vertexai.util.helper.Clock;

import java.util.Random;

/**
 * BreakScheduler â€” Takes natural 20â€“45 second "stretching/drink" breaks
 * every 35â€“50 minutes of continuous macroing.
 */
public class BreakScheduler extends AbstractFeature {

    @Getter
    public static final BreakScheduler instance = new BreakScheduler();

    private final Random random = new Random();
    private final Clock sessionClock = new Clock();
    private final Clock breakTimer = new Clock();

    private boolean takingBreak = false;
    private long nextBreakDelay = 0;

    public BreakScheduler() {
        this.enabled = true;
    }

    @Override
    public String getName() {
        return "BreakScheduler";
    }

    @Override
    protected void onTick() {
        if (mc.player == null) return;
        if (!MacroManager.getInstance().isRunning()) {
            sessionClock.reset();
            takingBreak = false;
            return;
        }

        if (!sessionClock.isScheduled()) {
            scheduleNextBreak();
        }

        // Trigger human break
        if (sessionClock.passed() && !takingBreak) {
            takingBreak = true;
            int breakDurationSec = 20 + random.nextInt(25); // 20â€“45s break
            log("BreakScheduler: Taking a natural human break for " + breakDurationSec + " seconds...");
            MacroManager.getInstance().pause();
            breakTimer.schedule(breakDurationSec * 1000L);
        }

        // Resume after break
        if (takingBreak && breakTimer.passed()) {
            takingBreak = false;
            log("BreakScheduler: Break finished! Resuming macro...");
            MacroManager.getInstance().resume();
            scheduleNextBreak();
        }
    }

    private void scheduleNextBreak() {
        nextBreakDelay = (35 + random.nextInt(15)) * 60 * 1000L; // 35â€“50 minutes
        sessionClock.schedule(nextBreakDelay);
    }
}
