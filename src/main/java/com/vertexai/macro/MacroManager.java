package com.vertexai.macro;

import com.vertexai.Vertex;
import com.vertexai.event.PacketEvent;
import com.vertexai.event.UpdateTablistEvent;
import com.vertexai.feature.FeatureManager;
import com.vertexai.feature.impl.MouseUngrab;
import com.vertexai.macro.impl.CommissionMacro.CommissionMacro;
import com.vertexai.macro.impl.FishingMacro.FishingMacro;
import com.vertexai.macro.impl.GlacialMacro.GlacialMacro;
import com.vertexai.macro.impl.MiningMacro.MiningMacro;
import com.vertexai.macro.impl.PowderMacro.PowderMacro;
import com.vertexai.macro.impl.RouteMiner.RouteMinerMacro;
import com.vertexai.util.KeyPressUtil;
import com.vertexai.util.Logger;
import com.vertexai.util.WorldRenderContextWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Manages macro lifecycle and event dispatch.
 * Events are called from EventManager.
 */
public class MacroManager {

    private static final MacroManager instance = new MacroManager();
    private final Minecraft mc = Minecraft.getInstance();
    private AbstractMacro currentMacro;

    public static MacroManager getInstance() {
        return instance;
    }

    public AbstractMacro getCurrentMacro() {
        var config = Vertex.config();
        if (config == null) return null;

        switch (config.general.macroType) {
            case 0:
                return CommissionMacro.getInstance();
            case 1:
                return GlacialMacro.getInstance();
            case 2:
                return MiningMacro.getInstance();
            case 3:
                return RouteMinerMacro.getInstance();
            case 4:
                return com.vertexai.macro.impl.NukerMacro.NukerMacro.getInstance();
            case 5:
                return com.vertexai.macro.impl.SlayerMacro.SlayerMacro.getInstance();
            case 6:
                return com.vertexai.macro.impl.FarmBuilderMacro.FarmBuilderMacro.getInstance();
            default:
                return CommissionMacro.getInstance();
        }
    }

    /**
     * The macro currently managed by MacroManager (running or paused), or null if off.
     */
    public AbstractMacro getActiveMacro() {
        return currentMacro;
    }

    public void toggle() {
        log("Toggling General Macro");
        if (currentMacro != null) {
            log("CurrMacro != null");
            this.disable();
        } else {
            log("CurrMacro == null");
            this.enable();
        }
    }

    public void enableMacro(AbstractMacro macro) {
        if (macro == null) return;
        this.disable();
        log("MacroManager: Enabling macro " + macro.getName());
        FeatureManager.getInstance().enableAll();
        this.currentMacro = macro;
        send(this.currentMacro.getName() + " Enabled");
        com.vertexai.util.DiscordWebhookNotifier.sendWebhookNotification(
                "🟢 Macro Started",
                "Enabled macro: **" + this.currentMacro.getName() + "**",
                0x22C55E // Emerald Green
        );
        this.currentMacro.enable();
    }

    public void enable() {
        log("Macro::enable");
        AbstractMacro macro = getCurrentMacro();
        if (macro == null) {
            error("No macro selected!");
            return;
        }
        enableMacro(macro);
    }

    public void disable() {
        if (this.currentMacro == null) {
            return;
        }

        var config = Vertex.config();
        if (config != null && config.debug.debugMode) {
            Vertex.LOGGER.debug("Macro disable stack trace", new Throwable());
        }

        log("Macro::disable");
        FeatureManager.getInstance().disableAll();
        MouseUngrab.getInstance().regrabMouse();
        this.currentMacro.disable();
        send(this.currentMacro.getName() + " Disabled");

        com.vertexai.ui.hud.elements.ProfitHUD profit = com.vertexai.ui.hud.elements.ProfitHUD.getInstance();
        String summaryMsg = "Disabled macro: **" + this.currentMacro.getName() + "**";

        com.vertexai.util.DiscordWebhookNotifier.sendWebhookNotification(
                "📊 SESSION SUMMARY REPORT",
                summaryMsg,
                0xEF4444 // Red
        );
        this.currentMacro = null;
    }

    public void pause() {
        if (this.currentMacro == null) {
            return;
        }
        log("Macro::pause");
        this.currentMacro.pause();
        send(this.currentMacro.getName() + " Paused");
    }

    public void resume() {
        if (this.currentMacro == null) {
            return;
        }
        log("Macro::resume");
        this.currentMacro.resume();
        send(this.currentMacro.getName() + " Resumed");
    }

    public boolean isEnabled() {
        return this.currentMacro != null;
    }

    public boolean isPaused() {
        return this.currentMacro != null && !this.currentMacro.isEnabled();
    }

    public boolean isRunning() {
        return this.currentMacro != null && this.currentMacro.isEnabled();
    }

    // ==================== Event Handlers (called from EventManager) ====================

    /**
     * Called every client tick.
     */
    public void onTick() {
        com.vertexai.pathing.aim.RotationRecorder.getInstance().onTick();
        com.vertexai.failsafe.reaction.RecordedReactionManager.getInstance().onTick();
        
        if (this.currentMacro == null) {
            return;
        }

        // Disable if macro stopped itself
        if (!currentMacro.isEnabled()) {
            this.disable();
            return;
        }

        this.currentMacro.onTick();
        this.currentMacro.getStateMachine().onTick();
    }

    /**
     * Called for key input checks.
     */
    public void onInput() {
        var config = Vertex.config();
        if (config == null) return;

        if (KeyPressUtil.wasPressed(mc.getWindow(), config.general.openConfigGuiKeybind, true)) {
            // Nothing here since GUI opening is handled by EventManager, but we keep this clean
        }
    }

    /**
     * Called when a chat message is received.
     */
    public void onChat(String message) {
        if (this.currentMacro == null) {
            return;
        }
        this.currentMacro.onChat(message);
    }

    /**
     * Called when tablist updates.
     */
    public void onTablistUpdate(UpdateTablistEvent event) {
        if (this.currentMacro == null) {
            return;
        }
        this.currentMacro.onTablistUpdate(event);
    }

    /**
     * Called for world rendering.
     */
    public void onWorldRender(WorldRenderContextWrapper context) {
        if (this.currentMacro == null) {
            return;
        }
        this.currentMacro.onWorldRender(context);
    }

    /**
     * Called for HUD rendering.
     */
    public void onHudRender(GuiGraphics GuiGraphics) {
        if (this.currentMacro == null) {
            return;
        }
        this.currentMacro.onOverlayRender(GuiGraphics);
    }

    /**
     * Called when a packet is received.
     */
    public void onPacketReceive(PacketEvent.Received event) {
        if (this.currentMacro == null) {
            return;
        }
        this.currentMacro.onReceivePacket(event);
    }

    // ==================== Logging Utilities ====================

    public void log(String message) {
        Logger.sendLog(getMessage(message));
    }

    public void send(String message) {
        Logger.sendMessage(getMessage(message));
    }

    public void error(String message) {
        Logger.sendError(getMessage(message));
    }

    public void warn(String message) {
        Logger.sendWarning(getMessage(message));
    }

    public String getMessage(String message) {
        return "[MacroHandler] " + message;
    }
}
