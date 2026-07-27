package com.vertexai.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.notenoughupdates.moulconfig.annotations.*;

import com.vertexai.VertexClient;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ConfigSerializer {

    public static JsonObject serialize(VertexConfig config) {
        JsonObject root = new JsonObject();
        
        try {
            for (Field catField : VertexConfig.class.getDeclaredFields()) {
                if (Modifier.isTransient(catField.getModifiers()) || Modifier.isStatic(catField.getModifiers())) continue;
                
                catField.setAccessible(true);
                Object categoryObj = catField.get(config);
                if (categoryObj == null) continue;

                Category catAnnotation = catField.getAnnotation(Category.class);
                String categoryName = catAnnotation != null ? catAnnotation.name() : catField.getName();
                
                JsonObject catJson = new JsonObject();
                catJson.addProperty("id", catField.getName());
                catJson.addProperty("name", categoryName);
                
                JsonArray settings = new JsonArray();
                
                for (Field field : categoryObj.getClass().getDeclaredFields()) {
                    if (Modifier.isTransient(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) continue;
                    
                    ConfigOption opt = field.getAnnotation(ConfigOption.class);
                    if (opt == null) continue;
                    
                    field.setAccessible(true);
                    JsonObject settingJson = new JsonObject();
                    settingJson.addProperty("id", field.getName());
                    settingJson.addProperty("name", opt.name());
                    settingJson.addProperty("desc", opt.desc());
                    
                    if (field.isAnnotationPresent(ConfigEditorBoolean.class)) {
                        settingJson.addProperty("type", "boolean");
                        settingJson.addProperty("value", field.getBoolean(categoryObj));
                    } else if (field.isAnnotationPresent(ConfigEditorSlider.class)) {
                        ConfigEditorSlider slider = field.getAnnotation(ConfigEditorSlider.class);
                        settingJson.addProperty("type", "slider");
                        settingJson.addProperty("min", slider.minValue());
                        settingJson.addProperty("max", slider.maxValue());
                        settingJson.addProperty("step", slider.minStep());
                        if (field.getType() == int.class) settingJson.addProperty("value", field.getInt(categoryObj));
                        else if (field.getType() == float.class) settingJson.addProperty("value", field.getFloat(categoryObj));
                    } else if (field.isAnnotationPresent(ConfigEditorText.class)) {
                        settingJson.addProperty("type", "text");
                        settingJson.addProperty("value", (String) field.get(categoryObj));
                    } else if (field.isAnnotationPresent(ConfigEditorDropdown.class)) {
                        ConfigEditorDropdown dropdown = field.getAnnotation(ConfigEditorDropdown.class);
                        settingJson.addProperty("type", "dropdown");
                        JsonArray opts = new JsonArray();
                        for (String v : dropdown.values()) {
                            opts.add(v);
                        }
                        settingJson.add("options", opts);
                        settingJson.addProperty("value", field.getInt(categoryObj));
                    } else {
                        continue;
                    }
                    settings.add(settingJson);
                }
                
                catJson.add("settings", settings);
                root.add(catField.getName(), catJson);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return root;
    }

    public static void updateField(VertexConfig config, String categoryId, String fieldId, String valueStr) {
        try {
            Field catField = VertexConfig.class.getDeclaredField(categoryId);
            catField.setAccessible(true);
            Object categoryObj = catField.get(config);
            if (categoryObj == null) return;

            Field field = categoryObj.getClass().getDeclaredField(fieldId);
            field.setAccessible(true);
            
            if (field.getType() == boolean.class) {
                field.setBoolean(categoryObj, Boolean.parseBoolean(valueStr));
            } else if (field.getType() == int.class) {
                field.setInt(categoryObj, (int) Double.parseDouble(valueStr));
            } else if (field.getType() == float.class) {
                field.setFloat(categoryObj, Float.parseFloat(valueStr));
            } else if (field.getType() == String.class) {
                field.set(categoryObj, valueStr);
            }
            
            VertexClient.configManager.saveConfig();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
