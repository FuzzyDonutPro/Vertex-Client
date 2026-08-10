package com.vertexai.feature.impl;

import com.vertexai.Vertex;
import com.vertexai.feature.AbstractFeature;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

public class PerspectiveMod extends AbstractFeature {

    private static PerspectiveMod instance;
    private final Minecraft mc = Minecraft.getInstance();

    private boolean active = false;
    private boolean wasKeyDown = false;

    public PerspectiveMod() {
        super();
        this.enabled = true; // Always tick to listen for keybind
    }

    // perspektive state
    public float freeLookYaw = 0.0f;
    public float freeLookPitch = 0.0f;
    public float cameraDistance = 4.0f;
    
    private CameraType originalCameraType = CameraType.FIRST_PERSON;
    private boolean awaitingRelease = false;

    public static PerspectiveMod getInstance() {
        if (instance == null) {
            instance = new PerspectiveMod();
        }
        return instance;
    }

    @Override
    public String getName() {
        return "PerspectiveMod";
    }

    @Override
    public boolean isRunning() {
        return active;
    }

    @Override
    protected void onTick() {
        if (mc.player == null) {
            if (active) disable();
            return;
        }

        int keybind = Vertex.config().gui.freeLookKeybind;
        int mode = Vertex.config().gui.freeLookMode; // 0 = Hold, 1 = Toggle
        
        if (!mc.isWindowActive() || mc.screen != null) {
            awaitingRelease = true;
        }

        boolean rawKeyDown = com.mojang.blaze3d.platform.InputConstants.isKeyDown(mc.getWindow(), keybind);
        if (!rawKeyDown) {
            awaitingRelease = false;
        }

        // Prevent getting stuck in freelook when Alt-Tabbing or opening a GUI (e.g. Chat/Inventory)
        // awaitingRelease prevents the GLFW bug where glfwGetKey returns true after regaining focus
        boolean isKeyDown = mc.isWindowActive() && mc.screen == null && rawKeyDown && !awaitingRelease;

        if (mode == 0) {
            // Hold mode
            if (isKeyDown && !active) {
                enable();
            } else if (!isKeyDown && active) {
                disable();
            }
        } else {
            // Toggle mode
            if (isKeyDown && !wasKeyDown) {
                if (active) {
                    disable();
                } else {
                    enable();
                }
            }
        }

        wasKeyDown = isKeyDown;
    }

    public void enable() {
        if (active) return;
        active = true;
        originalCameraType = mc.options.getCameraType();
        
        // Match player current rotation exactly like Perspektive
        freeLookYaw = mc.player.getYRot();
        freeLookPitch = mc.player.getXRot();
        
        // Switch to third person back view
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }

    public void disable() {
        if (!active) return;
        active = false;
        // Restore original camera type
        mc.options.setCameraType(originalCameraType);
    }
}
