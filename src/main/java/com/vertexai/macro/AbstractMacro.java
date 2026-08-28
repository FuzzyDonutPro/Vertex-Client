package com.vertexai.macro;

import lombok.Getter;
import com.vertexai.event.PacketEvent;
import com.vertexai.event.UpdateTablistEvent;
import com.vertexai.ui.hud.elements.CommissionHUD;
import com.vertexai.util.Logger;
import com.vertexai.util.helper.Clock;
import com.vertexai.util.WorldRenderContextWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public abstract class AbstractMacro {

    protected final Minecraft mc = Minecraft.getInstance();
    public Clock timer = new Clock();
    public Clock uptime = new Clock();
    private boolean enabled = false;
    protected final MacroStateMachine stateMachine = new MacroStateMachine(this);

    private long lastStuckCheckTime = System.currentTimeMillis();
    private Vec3 lastPlayerPos = Vec3.ZERO;
    private float lastHealth = -1;

    public boolean isEnabled() { return enabled; }
    public MacroStateMachine getStateMachine() { return stateMachine; }

    public abstract String getName();

    public void enable() {
        log("AbstractMacro::enable");
        this.onEnable();
        this.uptime.start(CommissionHUD.getInstance().commHudResetStats);
        this.enabled = true;
    }

    public void disable(String reason) {
        error(reason);
        this.disable();
    }

    public void disable() {
        log("AbstractMacro::disable");
        this.uptime.stop(CommissionHUD.getInstance().commHudResetStats);
        this.enabled = false;
        this.onDisable();
    }

    public void pause() {
        log("AbstractMacro::pause");
        this.uptime.stop(false);
        this.enabled = false;
        this.onPause();
    }

    public void resume() {
        log("AbstractMacro::resume");
        this.onResume();
        this.uptime.start(false);
        this.enabled = true;
    }

    public void toggle() {
        if (this.enabled) {
            this.disable();
        } else {
            this.enable();
        }
    }

    public abstract List<String> getNecessaryItems();

    public boolean hasTimerEnded() {
        return this.timer.isScheduled() && this.timer.passed();
    }

    public boolean isTimerRunning() {
        return this.timer.isScheduled() && !this.timer.passed();
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void onPause() {
    }

    public void onResume() {
    }

    public void smoothLookAt(Vec3 targetPos, float speedMultiplier) {
        if (mc.player == null || targetPos == null) return;
        com.vertexai.util.helper.Angle targetAngle = com.vertexai.util.AngleUtil.getRotation(targetPos);
        float currentYaw = mc.player.getYRot();
        float currentPitch = mc.player.getXRot();

        float yawDiff = com.vertexai.util.AngleUtil.normalizeAngle(targetAngle.getYaw() - currentYaw);
        float pitchDiff = targetAngle.getPitch() - currentPitch;

        // Humanized Perlin-style micro noise
        float noise = (float) ((Math.random() - 0.5) * 0.4);
        float step = Math.min(1.0f, 0.25f * speedMultiplier);

        mc.player.setYRot(currentYaw + (yawDiff * step) + noise);
        mc.player.setXRot(currentPitch + (pitchDiff * step) + noise);
    }

    public boolean checkStuckAndRecover() {
        if (mc.player == null) return false;
        // Only trigger stuck recovery if player is actively attempting to move!
        if (!mc.options.keyUp.isDown() && !mc.options.keyDown.isDown()) {
            lastPlayerPos = mc.player.position();
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - lastStuckCheckTime > 500) {
            Vec3 currentPos = mc.player.position();
            if (lastPlayerPos != Vec3.ZERO && currentPos.distanceToSqr(lastPlayerPos) < 0.01) {
                // Execute sub-tick stuck recovery jump/strafe
                if (mc.player.onGround()) {
                    mc.player.jumpFromGround();
                }
                lastStuckCheckTime = now;
                lastPlayerPos = currentPos;
                return true; // Stuck detected & recovery triggered
            }
            lastPlayerPos = currentPos;
            lastStuckCheckTime = now;
        }
        return false;
    }

    public void checkHealthFailsafe() {
        if (mc.player == null) return;
        if (!uptime.isScheduled() || uptime.getTimePassed() < 3000L) return; // 3-second startup grace period
        float currentHealth = mc.player.getHealth();
        float maxHealth = mc.player.getMaxHealth();

        if (maxHealth > 0 && currentHealth > 0) {
            float healthRatio = currentHealth / maxHealth;
            if (healthRatio < 0.15f) { // Only pause if health critically drops below 15%
                warn("CRITICAL HEALTH ALERT (" + (int)(healthRatio * 100) + "%)! Pausing macro safety...");
                if (isEnabled()) {
                    pause();
                    com.vertexai.util.DiscordWebhookNotifier.sendWebhookNotification(
                            "CRITICAL HEALTH FAILSAFE",
                            "Health dropped to `" + (int)(healthRatio * 100) + "%`! Macro paused.",
                            0xEF4444
                    );
                }
            }
        }
        lastHealth = currentHealth;
    }

    public void onTick() {
        if (isEnabled()) {
            checkHealthFailsafe();
        }
    }

    public void onWorldRender(WorldRenderContextWrapper context) {
    }

    public void onChat(String message) {
    }

    public void onTablistUpdate(UpdateTablistEvent event) {
    }

    public void onOverlayRender(GuiGraphicsExtractor GuiGraphics) {
    }

    public void onReceivePacket(PacketEvent.Received event) {
    }

    public void onSendPacket(PacketEvent.Sent event) {
    }

    protected void log(String message) {
        Logger.sendLog(formatMessage(message));
    }

    protected void send(String message) {
        Logger.sendMessage(formatMessage(message));
    }

    protected void error(String message) {
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
