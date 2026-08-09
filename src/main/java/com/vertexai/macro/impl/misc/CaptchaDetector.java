package com.vertexai.macro.impl.misc;

import lombok.Getter;
import com.vertexai.macro.AbstractFeature;
import com.vertexai.macro.MacroManager;
import com.vertexai.util.DiscordWebhookNotifier;
import com.vertexai.util.helper.Clock;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * CaptchaDetector â€” Detects visual GUI/inventory Head Captchas and Map Captchas.
 * Instantly freezes the macro and dispatches a high-priority Discord alert embed.
 */
public class CaptchaDetector extends AbstractFeature {

    @Getter
    public static final CaptchaDetector instance = new CaptchaDetector();

    private final Clock cooldown = new Clock();

    public CaptchaDetector() {
        this.enabled = true;
    }

    @Override
    public String getName() {
        return "CaptchaDetector";
    }

    @Override
    protected void onTick() {
        if (mc.player == null || mc.screen == null) return;
        if (cooldown.isScheduled() && !cooldown.passed()) return;

        if (mc.screen instanceof AbstractContainerScreen<?> containerScreen) {
            String title = containerScreen.getTitle().getString().toLowerCase();

            boolean isCaptchaTitle = title.contains("captcha") || title.contains("select") ||
                                     title.contains("click the") || title.contains("verify") ||
                                     title.contains("security");

            boolean hasMapItem = false;
            for (ItemStack stack : containerScreen.getMenu().getItems()) {
                if (stack.is(Items.FILLED_MAP) || stack.is(Items.MAP)) {
                    hasMapItem = true;
                    break;
                }
            }

            if (isCaptchaTitle || hasMapItem) {
                cooldown.schedule(15000); // 15s alert cooldown
                warn("ALERT: Visual Captcha / Security Check detected! Pausing macro immediately...");

                if (MacroManager.getInstance().isRunning()) {
                    MacroManager.getInstance().pause();
                }

                DiscordWebhookNotifier.sendWebhookNotification(
                        "ðŸš¨ CAPTCHA CHECK DETECTED",
                        "Visual Captcha / Verification GUI detected (`" + containerScreen.getTitle().getString() + "`)!\nMacro paused immediately. Please check your screen.",
                        0xFFEF4444
                );
            }
        }
    }
}
