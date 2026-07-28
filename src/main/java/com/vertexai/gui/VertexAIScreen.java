package com.vertexai.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import com.vertexai.gui.cef.VertexCEFBrowser;

public class VertexAIScreen extends Screen {

    private VertexCEFBrowser cefBrowser;

    public VertexAIScreen() {
        super(Component.literal("Vertex AI Dashboard"));
    }

    @Override
    protected void init() {
        super.init();
        cefBrowser = VertexCEFBrowser.getInstance();
        cefBrowser.resize(this.width, this.height);
    }

    public void resize(Minecraft client, int width, int height) {
        if (cefBrowser != null) {
            cefBrowser.resize(width, height);
        }
        super.rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        boolean rendered = false;
        if (cefBrowser != null) {
            rendered = cefBrowser.render(context, mouseX, mouseY, delta);
        }

        if (!rendered) {
            // Render dark glassmorphism loading overlay while CEF is starting
            context.fill(0, 0, this.width, this.height, 0xDD0F172A); // Slate 900 background
            
            String title = "⚡ VERTEX CLIENT UI";
            String subtext = "Initializing Chromium Embedded Framework...";
            String hint = "Press ESC to return to game";
            
            int titleWidth = this.font.width(title);
            int subtextWidth = this.font.width(subtext);
            int hintWidth = this.font.width(hint);

            int centerY = this.height / 2;

            context.drawString(this.font, title, (this.width - titleWidth) / 2, centerY - 25, 0xFF38BDF8, true);
            context.drawString(this.font, subtext, (this.width - subtextWidth) / 2, centerY, 0xFF94A3B8, true);
            context.drawString(this.font, hint, (this.width - hintWidth) / 2, centerY + 25, 0xFF64748B, true);
        }
    }

    @Override
    public void onClose() {
        if (cefBrowser != null) {
            cefBrowser.close();
        }
        super.onClose();
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isAction) {
        if (cefBrowser != null) {
            cefBrowser.injectMouseButton((int) event.x(), (int) event.y(), 0, event.button(), true, 1);
        }
        return super.mouseClicked(event, isAction);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if (cefBrowser != null) {
            cefBrowser.injectMouseButton((int) event.x(), (int) event.y(), 0, event.button(), false, 1);
        }
        return super.mouseReleased(event);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (cefBrowser != null) {
            cefBrowser.injectMouseMove((int) mouseX, (int) mouseY, 0, false);
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (cefBrowser != null) {
            cefBrowser.injectMouseWheel((int) mouseX, (int) mouseY, 0, (int) (verticalAmount * 120), 0);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if (cefBrowser != null) {
            cefBrowser.injectKeyPressed((char) 0, event.key(), event.modifiers());
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(net.minecraft.client.input.KeyEvent event) {
        if (cefBrowser != null) {
            cefBrowser.injectKeyReleased((char) 0, event.key(), event.modifiers());
        }
        return super.keyReleased(event);
    }
}

