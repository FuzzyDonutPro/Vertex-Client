package com.vertexai.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class VertexAIConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "vertexai.json");

    public String apiKey = "";
    public boolean enableAutoResponder = true;
    public boolean enablePathVisualizer = true;
    
    // Fishing Macro Settings
    public String fishingRod = "Rod";
    public String galateaAxe = "Axe";
    public String galateaFishingWeapon = "Sword";
    public int galateaKillMode = 0;

    private static VertexAIConfig instance;

    public static VertexAIConfig getInstance() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                instance = GSON.fromJson(reader, VertexAIConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
                instance = new VertexAIConfig();
            }
        } else {
            instance = new VertexAIConfig();
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
