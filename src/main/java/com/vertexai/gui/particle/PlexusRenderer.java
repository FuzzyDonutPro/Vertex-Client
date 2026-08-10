package com.vertexai.gui.particle;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PlexusRenderer {

    private static class Particle {
        float x, y, vx, vy;

        Particle(float x, float y, float vx, float vy) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
        }
    }

    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    private int width;
    private int height;
    private boolean initialized = false;

    // Reduced particles to 75 to completely eliminate any matrix calculation lag
    private static final int MAX_PARTICLES = 75;
    private static final float MAX_DISTANCE = 80.0f;
    private static final float SPEED = 0.6f;

    public void init(int width, int height) {
        this.width = width;
        this.height = height;
        
        if (!initialized) {
            particles.clear();
            for (int i = 0; i < MAX_PARTICLES; i++) {
                particles.add(new Particle(
                        random.nextFloat() * width,
                        random.nextFloat() * height,
                        (random.nextFloat() - 0.5f) * SPEED,
                        (random.nextFloat() - 0.5f) * SPEED
                ));
            }
            initialized = true;
        }
    }

    public void tick() {
        if (!initialized) return;

        for (Particle p : particles) {
            p.x += p.vx;
            p.y += p.vy;

            if (p.x < 0 || p.x > width) p.vx *= -1;
            if (p.y < 0 || p.y > height) p.vy *= -1;
        }
    }

    public void render(GuiGraphics context, float delta) {
        if (!initialized) return;

        // Draw solid dark blue background (covers the panorama)
        context.fill(0, 0, width, height, 0xFF040A18);

        float maxDistSq = MAX_DISTANCE * MAX_DISTANCE;
        
        // 1. Draw connections (Lines)
        for (int i = 0; i < particles.size(); i++) {
            Particle p1 = particles.get(i);
            for (int j = i + 1; j < particles.size(); j++) {
                Particle p2 = particles.get(j);
                float dx = p1.x - p2.x;
                float dy = p1.y - p2.y;
                float distSq = dx * dx + dy * dy;

                if (distSq < maxDistSq) {
                    float dist = (float) Math.sqrt(distSq);
                    float alpha = 1.0f - (dist / MAX_DISTANCE);
                    int alphaInt = (int) (alpha * 150); // Max opacity 150/255
                    int color = (alphaInt << 24) | 0x004CA6FF; // Blue tint
                    
                    drawLine(context, p1.x, p1.y, p2.x, p2.y, dist, color);
                }
            }
        }

        // 2. Draw perfectly smooth round glowing dots
        int coreColor = 0xFF88CCFF; // Light blue core
        int glowColor = 0x442266FF; // Semi-transparent deep blue glow
        for (Particle p : particles) {
            drawRoundDot(context, p.x, p.y, coreColor, glowColor);
        }
    }

    private void drawLine(GuiGraphics context, float x1, float y1, float x2, float y2, float dist, int color) {
        float angle = (float) Math.atan2(y2 - y1, x2 - x1);
        
        // Drastically reduce line opacity to make them appear much thinner and softer (anti-aliasing illusion)
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = Math.max(10, ((color >> 24) & 0xFF) / 3); // 3x more transparent
        int thinColor = (a << 24) | (r << 16) | (g << 8) | b;
        
        context.pose().pushMatrix();
        context.pose().translate(x1, y1);
        context.pose().rotate(angle);
        // Draw exactly 1 pixel thick, starting exactly at 0
        context.fill(0, 0, (int)dist, 1, thinColor);
        context.pose().popMatrix();
    }

    private void drawRoundDot(GuiGraphics context, float fx, float fy, int coreColor, int glowColor) {
        int ix = (int) fx;
        int iy = (int) fy;
        
        int faintGlow = (glowColor & 0x00FFFFFF) | 0x11000000;
        int midGlow = (glowColor & 0x00FFFFFF) | 0x44000000;
        
        // Radius 6
        drawCircleStrips(context, ix, iy, 6, faintGlow);
        // Radius 4
        drawCircleStrips(context, ix, iy, 4, midGlow);
        // Radius 2
        drawCircleStrips(context, ix, iy, 2, coreColor);
    }

    private void drawCircleStrips(GuiGraphics context, int x, int y, int radius, int color) {
        for (int i = -radius; i <= radius; i++) {
            int dx = (int) Math.round(Math.sqrt(radius * radius - i * i));
            context.fill(x - dx, y + i, x + dx, y + i + 1, color);
        }
    }
}
