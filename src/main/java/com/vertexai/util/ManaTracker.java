package com.vertexai.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks SkyBlock player Mana from actionbar and chat packets in real-time.
 */
public class ManaTracker {

    private static int currentMana = 100;
    private static int maxMana = 100;
    private static long lastUpdatedMs = 0;

    private static final Pattern MANA_SLASH_PATTERN = Pattern.compile("(\\d+)/(?:\\d+)\\s*(?:✎\\s*)?mana", Pattern.CASE_INSENSITIVE);
    private static final Pattern MANA_SIMPLE_PATTERN = Pattern.compile("(\\d+)\\s*(?:✎\\s*)?mana", Pattern.CASE_INSENSITIVE);

    public static void onPacketReceive(Packet<?> packet) {
        if (packet instanceof ClientboundSetActionBarTextPacket p) {
            parseMana(p.text().getString());
        } else if (packet instanceof ClientboundSystemChatPacket p && p.overlay()) {
            parseMana(p.content().getString());
        }
    }

    public static void parseMana(String text) {
        if (text == null || text.isEmpty()) return;
        String clean = ChatFormatting.stripFormatting(text);
        if (clean == null || clean.isEmpty()) return;

        Matcher m = MANA_SLASH_PATTERN.matcher(clean);
        if (m.find()) {
            try {
                currentMana = Integer.parseInt(m.group(1));
                lastUpdatedMs = System.currentTimeMillis();
                return;
            } catch (Exception ignored) {}
        }

        Matcher m2 = MANA_SIMPLE_PATTERN.matcher(clean);
        if (m2.find()) {
            try {
                currentMana = Integer.parseInt(m2.group(1));
                lastUpdatedMs = System.currentTimeMillis();
            } catch (Exception ignored) {}
        }
    }

    public static int getCurrentMana() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && (System.currentTimeMillis() - lastUpdatedMs > 5000L)) {
            // Fallback to XP level if actionbar has not updated in 5 seconds
            return mc.player.experienceLevel > 0 ? mc.player.experienceLevel : currentMana;
        }
        return currentMana;
    }
}
