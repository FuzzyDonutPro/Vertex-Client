package com.vertexai.config;

import io.github.notenoughupdates.moulconfig.gui.GuiContext;
import io.github.notenoughupdates.moulconfig.gui.GuiElementComponent;
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor;
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent;
import com.vertexai.VertexClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigGuiManager {
    private static final Component CONFIG_TITLE = Component.literal("Vertex Settings");
    public static MoulConfigEditor<VertexConfig> editor = null;

    public static void openConfigGui(String search) {
        Minecraft client = Minecraft.getInstance();
        client.gui.setScreen(createConfigScreen(client.gui.screen(), search));
    }

    public static MoulConfigScreenComponent createConfigScreen(Screen parent) {
        return createConfigScreen(parent, null);
    }

    public static MoulConfigScreenComponent createConfigScreen(Screen parent, String search) {
        ensureEditor();
        if (search != null) editor.search(search);
        MoulConfigScreenComponent screen = new MoulConfigScreenComponent(
                CONFIG_TITLE,
                new GuiContext(new GuiElementComponent(editor)),
                parent
        ) {
            @Override
            public void onClose() {
                super.onClose();
                VertexClient.configManager.saveConfig();
            }

            @Override
            public void removed() {
                super.removed();
                VertexClient.configManager.saveConfig();
            }
        };
        return screen;
    }

    private static void ensureEditor() {
        if (editor == null) {
            editor = new MoulConfigEditor<>(
                    VertexClient.configManager.processor
            );
        }
    }
}
