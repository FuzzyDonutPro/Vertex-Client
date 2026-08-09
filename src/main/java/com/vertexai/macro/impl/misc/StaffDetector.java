package com.vertexai.macro.impl.misc;

import lombok.Getter;
import com.vertexai.macro.AbstractFeature;
import com.vertexai.macro.MacroManager;
import com.vertexai.util.DiscordWebhookNotifier;
import com.vertexai.util.helper.Clock;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * StaffDetector â€” Detects Hypixel Staff, Admins, and Watchdog bots in current lobby.
 * Auto-evacuates (/l or /hub) instantly upon detection!
 */
public class StaffDetector extends AbstractFeature {

    @Getter
    public static final StaffDetector instance = new StaffDetector();

    private static final Set<String> KNOWN_STAFF = new HashSet<>(Arrays.asList(
            "hypixel", "rezzus", "plancke", "connorlinfoot", "inventivetalent", "yeleha",
            "orangemarshall", "jayavarmen", "timedeo", "xhascox", "heatran", "apunch",
            "cerus", "mrkeith", "nitroholic_", "relenter", "skyerzz", "tacnayn",
            "thebirmanator", "themgrf"
    ));

    private final Clock checkClock = new Clock();
    private boolean evacuating = false;

    public StaffDetector() {
        this.enabled = true;
    }

    @Override
    public String getName() {
        return "StaffDetector";
    }

    @Override
    protected void onTick() {
        if (mc.player == null || mc.getConnection() == null) return;
        var config = com.vertexai.Vertex.config();
        if (config != null && config.failsafe != null && !config.failsafe.enableStaffDetector) return;
        if (checkClock.isScheduled() && !checkClock.passed()) return;

        checkClock.schedule(2000); // Check tablist every 2s

        for (PlayerInfo playerInfo : mc.getConnection().getOnlinePlayers()) {
            if (playerInfo.getProfile() == null) continue;
            String name = playerInfo.getProfile().name();
            if (name == null) continue;

            if (KNOWN_STAFF.contains(name.toLowerCase())) {
                warn("ALERT: Hypixel Staff/Admin detected in lobby: " + name + "! Evacuating...");
                evacuateLobby(name);
                return;
            }
        }
    }

    public void onChat(String message) {
        String msg = message.toLowerCase();
        for (String staff : KNOWN_STAFF) {
            if (msg.contains(staff)) {
                warn("ALERT: Staff name mentioned in chat: " + staff + "! Evacuating...");
                evacuateLobby(staff);
                return;
            }
        }
    }

    private void evacuateLobby(String staffName) {
        if (evacuating) return;
        evacuating = true;

        if (MacroManager.getInstance().isRunning()) {
            MacroManager.getInstance().disable();
        }

        DiscordWebhookNotifier.sendWebhookNotification("ðŸš¨ STAFF DETECTED", "Hypixel Staff/Admin `" + staffName + "` detected in lobby! Macro terminated and evacuated to hub.", 0xFFDC2626);

        if (mc.player != null && mc.player.connection != null) {
            mc.player.connection.sendCommand("l");
        }

        new Thread(() -> {
            try {
                Thread.sleep(5000);
                evacuating = false;
            } catch (Exception ignored) {}
        }).start();
    }
}
