package com.vertexai.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Extracts displayable text and interactive click events from inbound chat-related packets.
 */
public final class ChatPacketUtil {

    private static final Minecraft mc = Minecraft.getInstance();

    private ChatPacketUtil() {
    }

    /**
     * @return a best-effort plain string, or null if not a chat packet.
     */
    public static String extractMessage(Packet<?> packet) {
        if (packet == null) {
            return null;
        }

        if (packet instanceof ClientboundSystemChatPacket messagePacket) {
            return strip(messagePacket.content().getString());
        }

        if (packet instanceof ClientboundPlayerChatPacket chatPacket) {
            return strip(chatPacket.body().content());
        }

        if (packet instanceof ClientboundSetActionBarTextPacket(net.minecraft.network.chat.Component text)) {
            return strip(text.getString());
        }

        return null;
    }

    /**
     * Recursively searches a chat Component hierarchy for a Style with an attached ClickEvent matching an optional keyword.
     */
    public static Style findStyleWithClickEvent(Component component, String keyword) {
        if (component == null) return null;

        if (component.getStyle() != null && component.getStyle().getClickEvent() != null) {
            String text = component.getString();
            if (keyword == null || text.toLowerCase().contains(keyword.toLowerCase())) {
                return component.getStyle();
            }
        }

        for (Component sibling : component.getSiblings()) {
            Style style = findStyleWithClickEvent(sibling, keyword);
            if (style != null) {
                return style;
            }
        }

        return null;
    }

    /**
     * Automatically extracts and executes the ClickEvent command on a Style across all mapping layers.
     */
    public static boolean executeStyleClick(Style style) {
        if (mc.player == null || style == null) return false;
        Object clickEvent = style.getClickEvent();
        if (clickEvent == null) return false;

        try {
            String value = null;
            try {
                Method m = clickEvent.getClass().getMethod("getValue");
                value = (String) m.invoke(clickEvent);
            } catch (Throwable t1) {
                try {
                    Method m = clickEvent.getClass().getMethod("value");
                    value = (String) m.invoke(clickEvent);
                } catch (Throwable t2) {
                    Field f = clickEvent.getClass().getDeclaredField("value");
                    f.setAccessible(true);
                    value = (String) f.get(clickEvent);
                }
            }

            if (value != null && !value.isEmpty()) {
                if (value.startsWith("/")) {
                    mc.player.connection.sendCommand(value.substring(1));
                } else {
                    mc.player.connection.sendChat(value);
                }
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static String strip(String s) {
        if (s == null) {
            return null;
        }
        String stripped = ChatFormatting.stripFormatting(s);
        if (stripped == null) {
            stripped = s;
        }
        stripped = stripped.trim();
        return stripped.isEmpty() ? null : stripped;
    }
}
