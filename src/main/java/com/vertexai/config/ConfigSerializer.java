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
                    if (Modifier.isStatic(field.getModifiers())) continue;
                    if (Modifier.isTransient(field.getModifiers()) && !field.isAnnotationPresent(ConfigEditorButton.class)) continue;
                    
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
                    } else if (field.isAnnotationPresent(io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind.class)) {
                        settingJson.addProperty("type", "keybind");
                        settingJson.addProperty("value", field.getInt(categoryObj));
                    } else if (field.isAnnotationPresent(ConfigEditorButton.class)) {
                        ConfigEditorButton btn = field.getAnnotation(ConfigEditorButton.class);
                        settingJson.addProperty("type", "button");
                        settingJson.addProperty("buttonText", btn.buttonText());
                        settingJson.addProperty("value", btn.buttonText());
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

    public static void executeButton(VertexConfig config, String categoryId, String fieldId) {
        if (config == null || categoryId == null || fieldId == null) return;
        try {
            Field catField = null;
            for (Field f : VertexConfig.class.getDeclaredFields()) {
                if (f.getName().equalsIgnoreCase(categoryId)) {
                    catField = f;
                    break;
                }
            }
            if (catField == null) return;
            catField.setAccessible(true);
            Object categoryObj = catField.get(config);
            if (categoryObj == null) return;

            Field field = null;
            Class<?> clazz = categoryObj.getClass();
            while (clazz != null && clazz != Object.class) {
                for (Field f : clazz.getDeclaredFields()) {
                    if (f.getName().equalsIgnoreCase(fieldId)) {
                        field = f;
                        break;
                    }
                }
                if (field != null) break;
                clazz = clazz.getSuperclass();
            }
            if (field == null) return;
            field.setAccessible(true);
            Object val = field.get(categoryObj);
            if (val instanceof Runnable runnable) {
                runnable.run();
            } else if ("setMiningToolButton".equalsIgnoreCase(fieldId) || "miningTool".equalsIgnoreCase(fieldId)) {
                ConfigActions.setMiningTool();
            } else if ("setAltMiningToolButton".equalsIgnoreCase(fieldId) || "altMiningTool".equalsIgnoreCase(fieldId)) {
                ConfigActions.setAltMiningTool();
            } else if ("setSlayerWeaponButton".equalsIgnoreCase(fieldId) || "slayerWeapon".equalsIgnoreCase(fieldId)) {
                ConfigActions.setSlayerWeapon();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateField(VertexConfig config, String categoryId, String fieldId, String valueStr) {
        if (config == null || categoryId == null || fieldId == null) return;
        try {
            Field catField = null;
            for (Field f : VertexConfig.class.getDeclaredFields()) {
                if (f.getName().equalsIgnoreCase(categoryId)) {
                    catField = f;
                    break;
                }
            }
            if (catField == null) {
                com.vertexai.util.Logger.sendError("[Config] Category not found: " + categoryId);
                return;
            }
            catField.setAccessible(true);
            Object categoryObj = catField.get(config);
            if (categoryObj == null) return;

            Field field = null;
            Class<?> clazz = categoryObj.getClass();
            while (clazz != null && clazz != Object.class) {
                for (Field f : clazz.getDeclaredFields()) {
                    if (f.getName().equalsIgnoreCase(fieldId)) {
                        field = f;
                        break;
                    }
                }
                if (field != null) break;
                clazz = clazz.getSuperclass();
            }

            if (field == null) {
                com.vertexai.util.Logger.sendError("[Config] Field not found: " + fieldId + " in " + categoryId);
                return;
            }
            field.setAccessible(true);

            if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                field.set(categoryObj, Boolean.parseBoolean(valueStr) || "1".equals(valueStr) || "true".equalsIgnoreCase(valueStr));
            } else if (field.getType() == int.class || field.getType() == Integer.class) {
                field.set(categoryObj, (int) Double.parseDouble(valueStr));
            } else if (field.getType() == float.class || field.getType() == Float.class) {
                field.set(categoryObj, Float.parseFloat(valueStr));
            } else if (field.getType() == double.class || field.getType() == Double.class) {
                field.set(categoryObj, Double.parseDouble(valueStr));
            } else if (field.getType() == long.class || field.getType() == Long.class) {
                field.set(categoryObj, (long) Double.parseDouble(valueStr));
            } else if (field.getType() == String.class) {
                field.set(categoryObj, valueStr);
            }

            VertexClient.configManager.saveConfig();
            com.vertexai.util.Logger.sendLog("[Config] Updated " + categoryId + "." + fieldId + " -> " + valueStr);
        } catch (Exception e) {
            com.vertexai.util.Logger.sendError("[Config] Failed to update " + categoryId + "." + fieldId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
