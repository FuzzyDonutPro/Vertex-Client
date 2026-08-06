package com.vertexai.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * ChatUtil — High-performance chat formatting, color code stripping, and client notification helper.
 */
public class ChatUtil {

    private static final String PREFIX = "§8[§bVertex§8] §r";

    public static String stripFormatting(String input) {
        if (input == null) return "";
        return ChatFormatting.stripFormatting(input);
    }

    public static void sendMessage(String message) {
        Logger.sendMessage(message);
    }

    public static void sendClientMessage(String message) {
        Logger.sendMessage(PREFIX + message);
    }

    public static void sendErrorMessage(String message) {
        Logger.sendError("§c" + message);
    }

    public static boolean containsIgnoreCase(String source, String target) {
        if (source == null || target == null) return false;
        return source.toLowerCase().contains(target.toLowerCase());
    }
}
