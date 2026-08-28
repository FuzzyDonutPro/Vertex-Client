package com.vertexai.gui;

import com.vertexai.Vertex;
import com.vertexai.VertexClient;
import com.vertexai.gui.web.WebDashboardScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class KeybindCaptureScreen extends Screen {

    private final Screen parent;
    private final String keybindName;
    private final Runnable onKeyCaptured;

    public KeybindCaptureScreen(Screen parent, String keybindName, Runnable onKeyCaptured) {
        super(Component.literal("Rebind " + keybindName));
        this.parent = parent;
        this.keybindName = keybindName;
        this.onKeyCaptured = onKeyCaptured;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        // Render sleek dark backdrop overlay
        context.fill(0, 0, this.width, this.height, 0xDD0F172A);

        String title = "⚡ REBIND " + keybindName.toUpperCase();
        String prompt = "Press any key on your keyboard to set new keybind...";
        String cancel = "Press ESCAPE to cancel";

        int centerY = this.height / 2;

        context.centeredText(this.font, title, this.width / 2, centerY - 25, 0xFF38BDF8);
        context.centeredText(this.font, prompt, this.width / 2, centerY, 0xFFFFFFFF);
        context.centeredText(this.font, cancel, this.width / 2, centerY + 25, 0xFF94A3B8);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            closeAndReturn();
            return true;
        }

        Vertex.config().gui.freeLookKeybind = key;
        VertexClient.configManager.saveConfig();

        if (onKeyCaptured != null) {
            onKeyCaptured.run();
        }
        closeAndReturn();
        return true;
    }

    private void closeAndReturn() {
        Minecraft.getInstance().setScreen(parent != null ? parent : new WebDashboardScreen());
    }
}
