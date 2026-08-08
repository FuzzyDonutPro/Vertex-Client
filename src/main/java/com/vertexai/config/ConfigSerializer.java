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
                collectSettings(categoryObj, settings);
                
                catJson.add("settings", settings);
                root.add(catField.getName(), catJson);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return root;
    }

    private static void collectSettings(Object obj, JsonArray settings) {
        if (obj == null) return;
        Class<?> clazz = obj.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                
                if (field.isAnnotationPresent(Category.class)) {
                    field.setAccessible(true);
                    try {
                        Object subObj = field.get(obj);
                        if (subObj != null) {
                            collectSettings(subObj, settings);
                        }
                    } catch (Exception ignored) {}
                    continue;
                }

                if (Modifier.isTransient(field.getModifiers()) && !field.isAnnotationPresent(ConfigEditorButton.class)) continue;
                
                ConfigOption opt = field.getAnnotation(ConfigOption.class);
                if (opt == null) continue;
                
                field.setAccessible(true);
                JsonObject settingJson = new JsonObject();
                settingJson.addProperty("id", field.getName());
                settingJson.addProperty("name", opt.name());
                settingJson.addProperty("desc", opt.desc());
                
                try {
                    if (field.isAnnotationPresent(ConfigEditorBoolean.class)) {
                        settingJson.addProperty("type", "boolean");
                        settingJson.addProperty("value", field.getBoolean(obj));
                    } else if (field.isAnnotationPresent(ConfigEditorSlider.class)) {
                        ConfigEditorSlider slider = field.getAnnotation(ConfigEditorSlider.class);
                        settingJson.addProperty("type", "slider");
                        settingJson.addProperty("min", slider.minValue());
                        settingJson.addProperty("max", slider.maxValue());
                        settingJson.addProperty("step", slider.minStep());
                        if (field.getType() == int.class) settingJson.addProperty("value", field.getInt(obj));
                        else if (field.getType() == float.class) settingJson.addProperty("value", field.getFloat(obj));
                    } else if (field.isAnnotationPresent(ConfigEditorText.class)) {
                        settingJson.addProperty("type", "text");
                        settingJson.addProperty("value", (String) field.get(obj));
                    } else if (field.isAnnotationPresent(ConfigEditorDropdown.class)) {
                        ConfigEditorDropdown dropdown = field.getAnnotation(ConfigEditorDropdown.class);
                        settingJson.addProperty("type", "dropdown");
                        JsonArray opts = new JsonArray();
                        for (String v : dropdown.values()) {
                            opts.add(v);
                        }
                        settingJson.add("options", opts);
                        settingJson.addProperty("value", field.getInt(obj));
                    } else if (field.isAnnotationPresent(io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind.class)) {
                        settingJson.addProperty("type", "keybind");
                        settingJson.addProperty("value", field.getInt(obj));
                    } else if (field.isAnnotationPresent(ConfigEditorButton.class)) {
                        ConfigEditorButton btn = field.getAnnotation(ConfigEditorButton.class);
                        settingJson.addProperty("type", "button");
                        settingJson.addProperty("buttonText", btn.buttonText());
                        settingJson.addProperty("value", btn.buttonText());
                    } else {
                        continue;
                    }
                    settings.add(settingJson);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    private static class FieldOwnerPair {
        Field field;
        Object owner;
        FieldOwnerPair(Field field, Object owner) {
            this.field = field;
            this.owner = owner;
        }
    }

    private static FieldOwnerPair findFieldAndOwner(Object obj, String fieldId) {
        if (obj == null) return null;
        Class<?> clazz = obj.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field f : clazz.getDeclaredFields()) {
                if (f.getName().equalsIgnoreCase(fieldId)) {
                    f.setAccessible(true);
                    return new FieldOwnerPair(f, obj);
                }
                if (f.isAnnotationPresent(Category.class)) {
                    f.setAccessible(true);
                    try {
                        Object subObj = f.get(obj);
                        FieldOwnerPair subPair = findFieldAndOwner(subObj, fieldId);
                        if (subPair != null) return subPair;
                    } catch (Exception ignored) {}
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    public static JsonObject executeButton(VertexConfig config, String categoryId, String fieldId) {
        if (config == null || fieldId == null) return serialize(config);
        if (categoryId == null || "undefined".equalsIgnoreCase(categoryId) || categoryId.isEmpty()) {
            categoryId = "general";
        }
        
        try {
            String cleanId = fieldId.toLowerCase();

            // 2. Reflective Runnable field execution
            Field catField = null;
            for (Field f : VertexConfig.class.getDeclaredFields()) {
                if (f.getName().equalsIgnoreCase(categoryId)) {
                    catField = f;
                    break;
                }
            }
            if (catField == null) return serialize(config);
            catField.setAccessible(true);
            Object categoryObj = catField.get(config);
            if (categoryObj == null) return serialize(config);

            FieldOwnerPair pair = findFieldAndOwner(categoryObj, fieldId);
            if (pair != null) {
                Object val = pair.field.get(pair.owner);
                if (val instanceof Runnable runnable) {
                    java.util.concurrent.CompletableFuture<Void> future = new java.util.concurrent.CompletableFuture<>();
                    Runnable task = () -> {
                        try {
                            runnable.run();
                            VertexClient.configManager.saveConfig();
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            future.complete(null);
                        }
                    };
                    if (net.minecraft.client.Minecraft.getInstance().isSameThread()) {
                        task.run();
                    } else {
                        net.minecraft.client.Minecraft.getInstance().execute(task);
                        future.join();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return serialize(config);
    }

    public static void updateField(VertexConfig config, String categoryId, String fieldId, String valueStr) {
        if (config == null || fieldId == null) return;
        try {
            FieldOwnerPair pair = null;
            if (categoryId != null && !categoryId.isEmpty() && !"undefined".equalsIgnoreCase(categoryId)) {
                for (Field f : VertexConfig.class.getDeclaredFields()) {
                    if (f.getName().equalsIgnoreCase(categoryId)) {
                        f.setAccessible(true);
                        try {
                            Object categoryObj = f.get(config);
                            if (categoryObj != null) {
                                pair = findFieldAndOwner(categoryObj, fieldId);
                            }
                        } catch (Exception ignored) {}
                        break;
                    }
                }
            }

            if (pair == null) {
                for (Field f : VertexConfig.class.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers())) continue;
                    f.setAccessible(true);
                    try {
                        Object categoryObj = f.get(config);
                        if (categoryObj != null) {
                            pair = findFieldAndOwner(categoryObj, fieldId);
                            if (pair != null) break;
                        }
                    } catch (Exception ignored) {}
                }
            }

            if (pair == null) {
                com.vertexai.util.Logger.sendError("[Config] Field not found: " + fieldId + " (category: " + categoryId + ")");
                return;
            }

            Field field = pair.field;
            Object owner = pair.owner;

            if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                field.set(owner, Boolean.parseBoolean(valueStr) || "1".equals(valueStr) || "true".equalsIgnoreCase(valueStr));
            } else if (field.getType() == int.class || field.getType() == Integer.class) {
                field.set(owner, (int) Double.parseDouble(valueStr));
            } else if (field.getType() == float.class || field.getType() == Float.class) {
                field.set(owner, Float.parseFloat(valueStr));
            } else if (field.getType() == double.class || field.getType() == Double.class) {
                field.set(owner, Double.parseDouble(valueStr));
            } else if (field.getType() == long.class || field.getType() == Long.class) {
                field.set(owner, (long) Double.parseDouble(valueStr));
            } else if (field.getType() == String.class) {
                field.set(owner, valueStr);
            }

            VertexClient.configManager.saveConfig();
            com.vertexai.util.Logger.sendLog("[Config] Updated " + categoryId + "." + fieldId + " -> " + valueStr);
        } catch (Exception e) {
            com.vertexai.util.Logger.sendError("[Config] Failed to update " + categoryId + "." + fieldId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
