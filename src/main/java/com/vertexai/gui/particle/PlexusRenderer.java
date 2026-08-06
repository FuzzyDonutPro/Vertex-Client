package com.vertexai.gui.particle;

import net.minecraft.client.gui.GuiGraphics;
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

    private static final int MAX_PARTICLES = 80;
    private static final float MAX_DISTANCE = 85.0f;
    private static final float SPEED = 0.5f;

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

        // Draw solid dark blue background
        context.fill(0, 0, width, height, 0xFF040A18);

        float maxDistSq = MAX_DISTANCE * MAX_DISTANCE;

        // 1. Draw smooth connecting lines
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
                    int alphaInt = (int) (alpha * 120);

                    drawLine(context, p1.x, p1.y, p2.x, p2.y, dist, alphaInt);
                }
            }
        }

        // 2. Draw smooth glowing particle dots
        for (Particle p : particles) {
            drawGlowDot(context, p.x, p.y);
        }
    }

    private void drawLine(GuiGraphics context, float x1, float y1, float x2, float y2, float dist, int alpha) {
        float angle = (float) Math.atan2(y2 - y1, x2 - x1);
        int softColor = (alpha << 24) | 0x0038BDF8;

        context.pose().pushMatrix();
        context.pose().translate(x1, y1);
        context.pose().rotate(angle);
        
        // Draw ultra-thin soft line
        context.fill(0, 0, (int) Math.ceil(dist), 1, softColor);
        context.pose().popMatrix();
    }

    private void drawGlowDot(GuiGraphics context, float fx, float fy) {
        int x = Math.round(fx);
        int y = Math.round(fy);

        // Core bright center
        context.fill(x - 1, y - 1, x + 2, y + 2, 0xFFBAE6FD);
        // Inner glow
        context.fill(x - 2, y - 2, x + 3, y + 3, 0x5538BDF8);
        // Outer ambient glow
        context.fill(x - 4, y - 4, x + 5, y + 5, 0x150EA5E9);
    }
}
