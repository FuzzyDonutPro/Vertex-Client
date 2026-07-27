package com.vertexai.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Color;

public class VertexAIScreen extends Screen {
    private final String[] TABS = {"Combat", "Mining", "Farming", "Fishing", "Foraging", "Hunting"};
    private int currentTab = 0;

    public VertexAIScreen() {
        super(Component.literal("Vertex AI Settings"));
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw a dark glassmorphism-style background (translucent dark gradient)
        context.fillGradient(0, 0, this.width, this.height, 0xD0000000, 0xF0000000);

        int tabWidth = this.width / TABS.length;
        int tabHeight = 30;

        // Draw Tabs
        for (int i = 0; i < TABS.length; i++) {
            int x = i * tabWidth;
            int y = 0;
            
            boolean isHovered = mouseX >= x && mouseX < x + tabWidth && mouseY >= y && mouseY < y + tabHeight;
            boolean isSelected = (i == currentTab);

            // Tab background color
            int bgColor = isSelected ? 0x803b82f6 : (isHovered ? 0x50ffffff : 0x20ffffff);
            context.fill(x, y, x + tabWidth, y + tabHeight, bgColor);

            // Highlight bar at the top of the selected tab
            if (isSelected) {
                context.fill(x, y, x + tabWidth, y + 2, 0xFF3b82f6); // bright blue
            }

            // Tab text
            int textColor = isSelected ? 0xFFFFFF : (isHovered ? 0xDDDDDD : 0xAAAAAA);
            int textWidth = this.textRenderer.getWidth(TABS[i]);
            context.drawTextWithShadow(this.textRenderer, TABS[i], x + (tabWidth - textWidth) / 2, y + (tabHeight - 8) / 2, textColor);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            int tabWidth = this.width / TABS.length;
            int tabHeight = 30;

            if (mouseY >= 0 && mouseY < tabHeight) {
                int clickedTab = (int) (mouseX / tabWidth);
                if (clickedTab >= 0 && clickedTab < TABS.length) {
                    this.currentTab = clickedTab;
                    // Play click sound
                    if (this.client != null && this.Minecraft.getInstance().player != null) {
                        this.Minecraft.getInstance().player.playSound(net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
