package com.vertexai.macro.states;

import com.vertexai.macro.FishingMacro;
import net.minecraft.client.Minecraft;

public class WarpingState implements FishingMacroState {
    private static final int MAX_WARP_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2_000L;
    private static final long WARNING_COOLDOWN_MS = 5_000L;

    private long lastWarpTime = 0;
    private long lastWarningTime = 0;
    private int retryCount = 0;
    private boolean warpInitiated = false;

    @Override
    public void onStart(FishingMacro macro) {
        this.retryCount = 0;
        this.lastWarpTime = 0;
        this.lastWarningTime = 0;
        this.warpInitiated = false;
        log("Entered WarpingState");
        
        // As requested by user, bypass location check and assume we always run
        startGalateaWarp();
    }

    @Override
    public FishingMacroState onTick(FishingMacro macro) {
        // Dummy check to simulate warp completion.
        // In a real scenario, this would wait for teleport confirmation from the server.
        if (warpInitiated) {
            long now = System.currentTimeMillis();
            if (now - lastWarpTime > RETRY_DELAY_MS) {
                // Assume warp finished after 2 seconds
                return new PathfindingState();
            }
        }
        return this;
    }

    private void startGalateaWarp() {
        retryCount++;
        Minecraft client = Minecraft.getInstance();
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.connection.sendCommand("warp galatea");
        }
        warpInitiated = true;
        lastWarpTime = System.currentTimeMillis();
        log("Warping to Galatea (attempt " + retryCount + ")");
    }

    @Override
    public void onEnd(FishingMacro macro) {
        log("Leaving WarpingState");
    }

    private void warnThrottled(String message) {
        long now = System.currentTimeMillis();
        if (now - lastWarningTime > WARNING_COOLDOWN_MS) {
            log(message);
            lastWarningTime = now;
        }
    }
}
