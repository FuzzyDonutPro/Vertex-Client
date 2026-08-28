package com.vertexai.client.overlay;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public abstract class AbstractHUDElement {

    protected float x = 5;
    protected float y = 5;
    protected boolean enabled = true;
    protected float scale = 1.0f;
    protected int anchor = 0;
    protected Minecraft mc = Minecraft.getInstance();

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    public float getY() { return y; }
    public void setY(float y) { this.y = y; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public float getScale() { return scale; }
    public void setScale(float scale) { this.scale = scale; }
    public int getAnchor() { return anchor; }
    public void setAnchor(int anchor) { this.anchor = anchor; }

    public abstract void render(GuiGraphicsExtractor context, float tickDelta);

    /**
     * Render variant used by the HUD editor.
     * Defaults to normal rendering.
     */
    public void renderForEditor(GuiGraphicsExtractor context, float tickDelta) {
        render(context, tickDelta);
    }

    public abstract int getWidth();

    public abstract int getHeight();

    /**
     * Size variant used by the HUD editor.
     * Defaults to normal size.
     */
    public int getEditorWidth() {
        return getWidth();
    }

    /**
     * Size variant used by the HUD editor.
     * Defaults to normal size.
     */
    public int getEditorHeight() {
        return getHeight();
    }

    public float getActualX(int elementWidth) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        float raw = switch (anchor) {
            case 1, 3 -> screenWidth - x - elementWidth;
            default -> x;
        };
        float maxX = Math.max(0, screenWidth - elementWidth);
        return Math.max(0, Math.min(raw, maxX));
    }

    public float getActualY(int elementHeight) {
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        float raw = switch (anchor) {
            case 2, 3 -> screenHeight - y - elementHeight;
            default -> y;
        };
        float maxY = Math.max(0, screenHeight - elementHeight);
        return Math.max(0, Math.min(raw, maxY));
    }

    public float getActualX() {
        return getActualX(getWidth());
    }

    public float getActualY() {
        return getActualY(getHeight());
    }

    public boolean isHovered(double mouseX, double mouseY) {
        return isHovered(mouseX, mouseY, getWidth(), getHeight());
    }

    public boolean isHovered(double mouseX, double mouseY, int width, int height) {
        float ax = getActualX(width);
        float ay = getActualY(height);
        return mouseX >= ax && mouseX <= ax + width && mouseY >= ay && mouseY <= ay + height;
    }
}
