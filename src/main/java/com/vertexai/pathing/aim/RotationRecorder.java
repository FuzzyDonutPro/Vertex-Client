package com.vertexai.pathing.aim;

import com.vertexai.Vertex;
import com.vertexai.util.Logger;
import net.minecraft.client.Minecraft;

import java.io.File;

public class RotationRecorder {

    private static RotationRecorder instance;
    private RotationProfile currentProfile;
    private boolean isRecording = false;

    private float lastYaw;
    private float lastPitch;
    private long lastTime;

    public static RotationRecorder getInstance() {
        if (instance == null) {
            instance = new RotationRecorder();
        }
        return instance;
    }

    public void toggleRecording() {
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    public void startRecording() {
        if (isRecording) return;
        this.currentProfile = new RotationProfile();
        this.isRecording = true;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            this.lastYaw = mc.player.getYRot();
            this.lastPitch = mc.player.getXRot();
        }
        this.lastTime = System.currentTimeMillis();
        
        Logger.sendMessage("§a[Rotation Training] Started recording mouse movements.");
    }

    public void stopRecording() {
        if (!isRecording) return;
        this.isRecording = false;
        
        // Save to file
        File dir = new File(Minecraft.getInstance().gameDirectory, "config/vertex/aim_profiles");
        if (!dir.exists()) dir.mkdirs();
        
        File file = new File(dir, "recorded_aim.json");
        this.currentProfile.save(file);
        
        Logger.sendMessage("§c[Rotation Training] Stopped recording. Saved to " + file.getName());
    }

    public void onTick() {
        if (!isRecording) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float currentYaw = mc.player.getYRot();
        float currentPitch = mc.player.getXRot();
        long currentTime = System.currentTimeMillis();

        float deltaYaw = currentYaw - lastYaw;
        float deltaPitch = currentPitch - lastPitch;
        long elapsedTime = currentTime - lastTime;

        // Only record if the mouse actually moved
        if (deltaYaw != 0 || deltaPitch != 0) {
            this.currentProfile.addTick(deltaYaw, deltaPitch, elapsedTime);
        }

        this.lastYaw = currentYaw;
        this.lastPitch = currentPitch;
        this.lastTime = currentTime;
    }
    
    public RotationProfile getCurrentProfile() {
        return currentProfile;
    }
}
