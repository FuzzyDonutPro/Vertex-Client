package com.vertexai.macro.impl.navigation;

import lombok.Getter;
import lombok.Setter;
import com.vertexai.macro.AbstractFeature;
import com.vertexai.handler.RouteHandler;
import com.vertexai.util.Logger;
import com.vertexai.util.PlayerUtil;
import com.vertexai.util.helper.route.WaypointType;

import net.minecraft.core.BlockPos;

/**
 * RouteAutoRecorder â€” Live automatic Mining Route recorder.
 * Automatically drops waypoints as you walk or mine gemstone blocks, and saves them
 * directly into your active RouteMiner configuration.
 */
public class RouteAutoRecorder extends AbstractFeature {

    private static final RouteAutoRecorder instance = new RouteAutoRecorder();
    public static RouteAutoRecorder getInstance() { return instance; }

    @Getter @Setter
    private boolean autoMiningMode = true; // Auto-detects gemstone mining vs walking
    private BlockPos lastRecordedPos = null;

    @Override
    public String getName() {
        return "RouteAutoRecorder";
    }

    public void startRecording() {
        this.enabled = true;
        this.lastRecordedPos = null;
        if (!RouteBuilder.getInstance().isRunning()) {
            RouteBuilder.getInstance().start();
        }
        Logger.sendMessage("RouteAutoRecorder: Started live route recording! Walk or mine to drop waypoints.");
    }

    public void stopRecording() {
        this.enabled = false;
        this.lastRecordedPos = null;
        Logger.sendMessage("RouteAutoRecorder: Stopped live route recording.");
    }

    public void toggleRecording() {
        if (this.enabled) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    @Override
    protected void onTick() {
        if (!this.enabled || mc.player == null) return;

        BlockPos currentPos = PlayerUtil.getBlockStandingOn();
        if (currentPos == null) return;

        // Auto-drop waypoint if moved >= 2.0 horizontal blocks (X/Z plane) from last recorded waypoint
        double dx = lastRecordedPos != null ? currentPos.getX() - lastRecordedPos.getX() : 99.0;
        double dz = lastRecordedPos != null ? currentPos.getZ() - lastRecordedPos.getZ() : 99.0;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        if (lastRecordedPos == null || horizontalDist >= 2.0) {
            WaypointType type = autoMiningMode ? WaypointType.MINE : WaypointType.WALK;
            if (RouteHandler.getInstance().addToCurrentRoute(currentPos, type)) {
                lastRecordedPos = currentPos;
                Logger.sendMessage("Auto-Recorded Waypoint #" + RouteHandler.getInstance().getSelectedRoute().size() + " [" + type.name() + "]");
            }
        }
    }

    /**
     * Call when a gemstone block is mined to record an exact MINE node.
     */
    public void onGemstoneMined(BlockPos pos) {
        if (!this.enabled || pos == null) return;
        if (RouteHandler.getInstance().addToCurrentRoute(pos, WaypointType.MINE)) {
            lastRecordedPos = pos;
            Logger.sendMessage("Auto-Recorded Gemstone Node at " + pos.toShortString());
        }
    }
}
