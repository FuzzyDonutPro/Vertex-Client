package com.vertexai.config;

import com.vertexai.gui.web.WebDashboardScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class ConfigGuiManager {

    public static void openConfigGui(String search) {
        System.out.println("[vertexai/DEBUG] ConfigGuiManager.openConfigGui() called with search: " + search);
        Minecraft client = Minecraft.getInstance();
        client.setScreen(createConfigScreen(client.screen, search));
    }

    public static Screen createConfigScreen(Screen parent) {
        return createConfigScreen(parent, null);
    }

    public static Screen createConfigScreen(Screen parent, String search) {
        System.out.println("[vertexai/DEBUG] ConfigGuiManager.createConfigScreen() creating WebDashboardScreen...");
        return new WebDashboardScreen();
    }
}
