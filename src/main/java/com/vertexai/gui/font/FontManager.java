package com.vertexai.gui.font;

import com.vertexai.Vertex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class FontManager {

    private static final Map<String, Font> loadedFonts = new HashMap<>();
    public static final String[] FONT_NAMES = {"Inter", "Outfit", "Roboto", "JetBrains Mono", "Minecraft"};

    public static void init() {
        loadFont("Inter", "/assets/vertexai/fonts/Inter.ttf");
        loadFont("Outfit", "/assets/vertexai/fonts/Outfit.ttf");
        loadFont("Roboto", "/assets/vertexai/fonts/Roboto.ttf");
        loadFont("JetBrains Mono", "/assets/vertexai/fonts/JetBrainsMono.ttf");
    }

    private static void loadFont(String name, String resourcePath) {
        try (InputStream stream = FontManager.class.getResourceAsStream(resourcePath)) {
            if (stream != null) {
                Font awtFont = Font.createFont(Font.TRUETYPE_FONT, stream).deriveFont(14.0f);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(awtFont);
                loadedFonts.put(name, awtFont);
                System.out.println("[vertexai/DEBUG] FontManager: Successfully loaded font " + name);
            }
        } catch (Exception e) {
            System.err.println("[vertexai/DEBUG] FontManager: Failed to load font " + name + ": " + e.getMessage());
        }
    }

    public static String getSelectedFontName() {
        try {
            var config = Vertex.config();
            if (config != null && config.general != null) {
                int index = config.general.guiFont;
                if (index >= 0 && index < FONT_NAMES.length) {
                    return FONT_NAMES[index];
                }
            }
        } catch (Exception ignored) {}
        return "Inter";
    }

    public static int getStringWidth(String text) {
        String fontName = getSelectedFontName();
        if ("Minecraft".equalsIgnoreCase(fontName) || !loadedFonts.containsKey(fontName)) {
            return Minecraft.getInstance().font.width(text);
        }
        Font font = loadedFonts.get(fontName);
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setFont(font);
        int w = g.getFontMetrics().stringWidth(text);
        g.dispose();
        return Math.max(1, w);
    }

    public static void drawString(GuiGraphics context, String text, int x, int y, int color) {
        // High-performance, zero-bleed text rendering
        context.drawString(Minecraft.getInstance().font, text, x, y, color, true);
    }
}
