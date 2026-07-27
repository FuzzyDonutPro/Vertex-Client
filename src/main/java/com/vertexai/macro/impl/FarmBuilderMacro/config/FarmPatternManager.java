package com.vertexai.macro.impl.FarmBuilderMacro.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vertexai.Vertex;
import com.vertexai.util.Logger;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

public class FarmPatternManager {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_DIR = new File(FabricLoader.getInstance().getConfigDir().toFile(), "vertex/farm_patterns");
    
    public static final Map<String, FarmPatternConfig> patterns = new HashMap<>();

    public static void load() {
        patterns.clear();
        
        if (!CONFIG_DIR.exists()) {
            CONFIG_DIR.mkdirs();
            createDefaultPatterns();
        }

        File[] files = CONFIG_DIR.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            createDefaultPatterns();
            files = CONFIG_DIR.listFiles((dir, name) -> name.endsWith(".json"));
        }

        if (files != null) {
            for (File file : files) {
                try (FileReader reader = new FileReader(file)) {
                    FarmPatternConfig config = GSON.fromJson(reader, FarmPatternConfig.class);
                    if (config != null && config.name != null) {
                        patterns.put(config.name, config);
                    }
                } catch (Exception e) {
                    Logger.sendError("Failed to load farm pattern: " + file.getName());
                    e.printStackTrace();
                }
            }
        }
        
        Logger.sendLog("Loaded " + patterns.size() + " farm patterns.");
    }

    private static void createDefaultPatterns() {
        // Sugar Cane Default
        FarmPatternConfig cane = new FarmPatternConfig();
        cane.name = "S-Shape Sugar Cane";
        cane.patternWidth = 3;
        cane.s_shape_turns = true;
        
        FarmPatternConfig.ColumnConfig col1 = new FarmPatternConfig.ColumnConfig();
        col1.offset = 0;
        col1.tools = Arrays.asList("InfiniDirt Wand", "Hoe of Greater Tilling", "Nether Wart Pouch"); // Can use any pouch for cane planting usually, or just plant manual
        
        FarmPatternConfig.ColumnConfig col2 = new FarmPatternConfig.ColumnConfig();
        col2.offset = 1;
        col2.tools = Arrays.asList("InfiniDirt Wand", "Hoe of Greater Tilling", "Nether Wart Pouch");
        
        FarmPatternConfig.ColumnConfig col3 = new FarmPatternConfig.ColumnConfig();
        col3.offset = 2;
        col3.tools = Arrays.asList("Prismapump");

        cane.columns = Arrays.asList(col1, col2, col3);
        
        // S-Shape Flat Default (Wheat/Carrot/Potato)
        FarmPatternConfig flat = new FarmPatternConfig();
        flat.name = "S-Shape Flat (Wheat/Carrot/Potato)";
        flat.patternWidth = 5;
        flat.s_shape_turns = true;
        
        List<FarmPatternConfig.ColumnConfig> flatCols = new ArrayList<>();
        
        FarmPatternConfig.ColumnConfig waterCol = new FarmPatternConfig.ColumnConfig();
        waterCol.offset = 0;
        waterCol.tools = Arrays.asList("Prismapump");
        flatCols.add(waterCol);
        
        for (int i = 1; i <= 4; i++) {
            FarmPatternConfig.ColumnConfig c = new FarmPatternConfig.ColumnConfig();
            c.offset = i;
            c.tools = Arrays.asList("InfiniDirt Wand", "Hoe of Greater Tilling", "Basket of Seeds");
            flatCols.add(c);
        }
        flat.columns = flatCols;

        savePattern(cane, "s_shape_sugar_cane.json");
        savePattern(flat, "s_shape_flat.json");
    }

    private static void savePattern(FarmPatternConfig config, String fileName) {
        File file = new File(CONFIG_DIR, fileName);
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(config, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
