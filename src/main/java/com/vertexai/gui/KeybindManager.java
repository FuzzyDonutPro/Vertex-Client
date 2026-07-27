package com.vertexai.gui;

import com.vertexai.gui.cef.VertexUIServer;
import com.vertexai.gui.web.WebDashboardScreen;
import com.vertexai.util.Logger;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class KeybindManager {
    public static KeyMapping openGuiKey;

    public static void init() {
        if (openGuiKey != null) return;
        
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.vertexai.opengui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath("vertexai", "general"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.consumeClick()) {
                if (client.screen == null) {
                    Logger.sendLog("[KeybindManager] Opening Vertex AI Web Dashboard Screen...");
                    client.setScreen(new WebDashboardScreen());
                }
            }
        });
        
        Logger.sendLog("[KeybindManager] Registered openGuiKey (RIGHT SHIFT)");
    }
}

