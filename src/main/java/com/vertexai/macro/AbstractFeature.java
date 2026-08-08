package com.vertexai.macro;

import lombok.Getter;
import com.vertexai.event.BlockChangeEvent;
import com.vertexai.event.BlockDestroyEvent;
import com.vertexai.event.SpawnParticleEvent;
import com.vertexai.event.UpdateTablistEvent;
import com.vertexai.failsafe.AbstractFailsafe.Failsafe;
import com.vertexai.util.Logger;
import com.vertexai.util.helper.Clock;
import com.vertexai.util.WorldRenderContextWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.Packet;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractFeature {

    protected final Minecraft mc = Minecraft.getInstance();
    protected final Clock timer = new Clock();
    protected List<Failsafe> failsafesToIgnore;
    public List<Failsafe> getFailsafesToIgnore() { return failsafesToIgnore; }
    protected boolean enabled = false;

    public AbstractFeature() {
        this.failsafesToIgnore = new ArrayList<>();
    }

    public abstract String getName();

    /**
     * Returns whether the feature is currently running.
     * This is determined by the internal 'enabled' flag,
     * which is toggled through start, stop, pause, and resume logic.
     * <p>
     * IMPORTANT:This is different from isEnabled()
     *
     * @return true if the feature is active and running
     */
    public boolean isRunning() {
        return this.enabled;
    }

    /**
     * Indicates whether the feature is marked as enabled by config or default logic.
     * IMPORTANT: This is independent of whether it is currently running.
     *
     * @return true if the feature is considered enabled
     */
    public boolean isEnabled() {
        return true;
    }

    /**
     * Starts the feature. Should be overridden by subclasses
     * to initialize or enable feature-specific logic.
     * NOTE: This does NOT automatically set 'enabled' to true.
     */
    public void start() {
        this.enabled = true;
        FeatureManager.getInstance().markActiveDirty();
    }

    public void stop() {
        this.enabled = false;
        FeatureManager.getInstance().markActiveDirty();
        this.resetStatesAfterStop();
    }

    public void pause() {
        this.enabled = false;
        FeatureManager.getInstance().markActiveDirty();
    }

    public void resume() {
        this.enabled = true;
        FeatureManager.getInstance().markActiveDirty();
    }

    /**
     * Override this method to clean up or reset custom states when the feature stops.
     */
    // Cleanup hook for subclasses
    public void resetStatesAfterStop() {
    }

    /**
     * Determines whether this feature should auto-start on application launch.
     *
     * @return true if the feature should start automatically
     */
    public boolean shouldStartAtLaunch() {
        return false;
    }

    /**
     * Indicates if failsafe checks should be skipped for this feature.
     *
     * @return true to bypass failsafe logic
     */
    public boolean shouldNotCheckForFailsafe() {
        return false;
    }

    /**
     * Checks whether the internal timer is currently running
     * and has not yet completed its duration.
     *
     * @return true if the timer is scheduled and still in progress
     */
    protected boolean isTimerRunning() {
        return this.timer.isScheduled() && !this.timer.passed();
    }

    /**
     * Checks whether the internal timer is scheduled and has completed.
     *
     * @return true if the timer is scheduled and has elapsed
     */
    protected boolean hasTimerEnded() {
        return this.timer.isScheduled() && this.timer.passed();
    }

    public final void handleTick() {
        if (!this.enabled) {
            return;
        }
        this.onTick();
    }

    public final void handleWorldRender(WorldRenderContextWrapper context) {
        if (!this.enabled) {
            return;
        }
        this.onWorldRender(context);
    }

    public final void handleHudRender(GuiGraphics GuiGraphics) {
        if (!this.enabled) {
            return;
        }
        this.onHudRender(GuiGraphics);
    }

    public final void handleChat(String message) {
        if (!this.enabled) {
            return;
        }
        this.onChat(message);
    }

    public final void handleTablistUpdate(UpdateTablistEvent event) {
        if (!this.enabled) {
            return;
        }
        this.onTablistUpdate(event);
    }

    public final void handlePacketReceive(Packet<?> packet) {
        if (!this.enabled) {
            return;
        }
        this.onPacketReceive(packet);
    }

    public final void handleWorldLoad(ClientLevel world) {
        if (!this.enabled) {
            return;
        }
        this.onWorldLoad(world);
    }

    public final void handleWorldUnload(ClientLevel world) {
        if (!this.enabled) {
            return;
        }
        this.onWorldUnload(world);
    }

    public final void handleBlockChange(BlockChangeEvent event) {
        if (!this.enabled) {
            return;
        }
        this.onBlockChange(event);
    }

    public final void handleBlockDestroy(BlockDestroyEvent event) {
        if (!this.enabled) {
            return;
        }
        this.onBlockDestroy(event);
    }

    public final void handleParticleSpawn(SpawnParticleEvent event) {
        if (!this.enabled) {
            return;
        }
        this.onParticleSpawn(event);
    }

    public final void handleScreenOpen(Screen screen) {
        if (!this.enabled) {
            return;
        }
        this.onScreenOpen(screen);
    }

    protected void onTick() {
    }

    protected void onWorldRender(WorldRenderContextWrapper context) {
    }

    protected void onHudRender(GuiGraphics GuiGraphics) {
    }

    protected void onChat(String message) {
    }

    protected void onTablistUpdate(UpdateTablistEvent event) {
    }

    protected void onPacketReceive(Packet<?> packet) {
    }

    protected void onWorldLoad(ClientLevel world) {
    }

    protected void onWorldUnload(ClientLevel world) {
    }

    protected void onBlockChange(BlockChangeEvent event) {
    }

    protected void onBlockDestroy(BlockDestroyEvent event) {
    }

    protected void onParticleSpawn(SpawnParticleEvent event) {
    }

    protected void onScreenOpen(Screen screen) {
    }

    protected void log(String message) {
        Logger.sendLog(formatMessage(message));
    }

    protected void send(String message) {
        Logger.sendMessage(formatMessage(message));
    }

    protected void logError(String message) {
        Logger.sendLog(formatMessage("Error: " + message));
    }

    protected void sendError(String message) {
        Logger.sendError(formatMessage(message));
    }

    protected void warn(String message) {
        Logger.sendWarning(formatMessage(message));
    }

    protected void note(String message) {
        Logger.sendNote(formatMessage(message));
    }

    protected String formatMessage(String message) {
        return "[" + getName() + "] " + message;
    }
}
