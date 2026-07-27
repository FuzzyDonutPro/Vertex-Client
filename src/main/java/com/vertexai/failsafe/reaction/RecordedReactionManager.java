package com.vertexai.failsafe.reaction;

import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.Logger;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RecordedReactionManager {

    private static RecordedReactionManager instance;
    private final Minecraft mc = Minecraft.getInstance();

    private boolean isRecording = false;
    private RecordedReaction currentRecording;
    private float lastYaw;
    private float lastPitch;
    private int maxRecordingTicks = 200;

    private boolean isPlaying = false;
    private RecordedReaction activePlayback;
    private int playbackTickIndex = 0;

    public static RecordedReactionManager getInstance() {
        if (instance == null) {
            instance = new RecordedReactionManager();
        }
        return instance;
    }

    private File getReactionDirectory() {
        File dir = new File(mc.gameDirectory, "config/vertex/reactions");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void startRecording(String name) {
        startRecording(name, 200);
    }

    public void startRecording(String name, int ticks) {
        if (isRecording) {
            Logger.sendWarning("Already recording reaction!");
            return;
        }
        if (isPlaying) {
            stopPlayback();
        }

        this.currentRecording = new RecordedReaction(name);
        this.isRecording = true;
        this.maxRecordingTicks = ticks > 0 ? ticks : 200;

        if (mc.player != null) {
            this.lastYaw = mc.player.getYRot();
            this.lastPitch = mc.player.getXRot();
        }
        float seconds = maxRecordingTicks / 20.0f;
        Logger.sendMessage(String.format("§a[Reaction Recorder] Started recording '§e%s§a' for %d ticks (%.1fs). Auto-stops when complete.", name, maxRecordingTicks, seconds));
    }

    public void stopRecording() {
        if (!isRecording) {
            Logger.sendWarning("Not currently recording any reaction.");
            return;
        }
        this.isRecording = false;

        File dir = getReactionDirectory();
        File file = new File(dir, currentRecording.getName().toLowerCase().replace(" ", "_") + ".json");
        currentRecording.save(file);

        float seconds = currentRecording.getTicks().size() / 20.0f;
        Logger.sendMessage(String.format("§a[Reaction Recorder] Saved reaction '§e%s§a' to §e%s §a(%d ticks / %.1fs)", currentRecording.getName(), file.getName(), currentRecording.getTicks().size(), seconds));
    }

    public void playReaction(String name) {
        File dir = getReactionDirectory();
        File file = new File(dir, name.toLowerCase().replace(" ", "_") + ".json");
        if (!file.exists()) {
            file = new File(dir, name.toLowerCase().replace(" ", "_"));
            if (!file.exists()) {
                Logger.sendError("Reaction preset '" + name + "' not found.");
                return;
            }
        }

        RecordedReaction reaction = RecordedReaction.load(file);
        if (reaction == null || reaction.getTicks().isEmpty()) {
            Logger.sendError("Failed to load reaction preset '" + name + "'.");
            return;
        }

        if (isRecording) stopRecording();

        this.activePlayback = reaction;
        this.playbackTickIndex = 0;
        this.isPlaying = true;
        Logger.sendMessage("§b[Reaction Player] Playing back reaction: §e" + reaction.getName() + " §7(" + reaction.getTicks().size() + " ticks)");
    }

    public void stopPlayback() {
        if (!isPlaying) return;
        this.isPlaying = false;
        this.activePlayback = null;
        this.playbackTickIndex = 0;
        KeyBindUtil.stopMovement();
        Logger.sendMessage("§c[Reaction Player] Playback stopped.");
    }

    public List<String> getSavedReactionNames() {
        List<String> names = new ArrayList<>();
        File dir = getReactionDirectory();
        File[] files = dir.listFiles((d, f) -> f.endsWith(".json"));
        if (files != null) {
            for (File f : files) {
                names.add(f.getName().replace(".json", ""));
            }
        }
        return names;
    }

    public boolean deleteReaction(String name) {
        File dir = getReactionDirectory();
        File file = new File(dir, name.toLowerCase().replace(" ", "_") + ".json");
        if (file.exists() && file.delete()) {
            Logger.sendMessage("§c[Reaction Manager] Deleted reaction preset: " + name);
            return true;
        }
        return false;
    }

    public void onTick() {
        if (mc.player == null) return;

        if (isRecording) {
            if (currentRecording.getTicks().size() >= maxRecordingTicks) {
                stopRecording();
                return;
            }

            float currentYaw = mc.player.getYRot();
            float currentPitch = mc.player.getXRot();
            float deltaYaw = currentYaw - lastYaw;
            float deltaPitch = currentPitch - lastPitch;

            boolean forward = mc.options.keyUp.isDown();
            boolean backward = mc.options.keyDown.isDown();
            boolean left = mc.options.keyLeft.isDown();
            boolean right = mc.options.keyRight.isDown();
            boolean jump = mc.options.keyJump.isDown();
            boolean sneak = mc.options.keyShift.isDown();

            currentRecording.addTick(deltaYaw, deltaPitch, forward, backward, left, right, jump, sneak);

            this.lastYaw = currentYaw;
            this.lastPitch = currentPitch;
        } else if (isPlaying && activePlayback != null) {
            if (playbackTickIndex >= activePlayback.getTicks().size()) {
                stopPlayback();
                return;
            }

            RecordedReaction.ReactionTick tick = activePlayback.getTicks().get(playbackTickIndex++);
            mc.player.setYRot(mc.player.getYRot() + tick.deltaYaw);
            mc.player.setXRot(mc.player.getXRot() + tick.deltaPitch);

            KeyBindUtil.setKeyBindState(mc.options.keyUp, tick.forward);
            KeyBindUtil.setKeyBindState(mc.options.keyDown, tick.backward);
            KeyBindUtil.setKeyBindState(mc.options.keyLeft, tick.left);
            KeyBindUtil.setKeyBindState(mc.options.keyRight, tick.right);
            KeyBindUtil.setKeyBindState(mc.options.keyJump, tick.jump);
            KeyBindUtil.setKeyBindState(mc.options.keyShift, tick.sneak);
        }
    }
}
