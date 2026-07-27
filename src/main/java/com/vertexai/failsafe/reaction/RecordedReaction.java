package com.vertexai.failsafe.reaction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class RecordedReaction {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private String name;
    private List<ReactionTick> ticks = new ArrayList<>();

    public RecordedReaction(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<ReactionTick> getTicks() {
        return ticks;
    }

    public void addTick(float deltaYaw, float deltaPitch, boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean sneak) {
        ticks.add(new ReactionTick(deltaYaw, deltaPitch, forward, backward, left, right, jump, sneak));
    }

    public void save(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(this, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static RecordedReaction load(File file) {
        try (FileReader reader = new FileReader(file)) {
            return GSON.fromJson(reader, RecordedReaction.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static class ReactionTick {
        public float deltaYaw;
        public float deltaPitch;
        public boolean forward;
        public boolean backward;
        public boolean left;
        public boolean right;
        public boolean jump;
        public boolean sneak;

        public ReactionTick(float deltaYaw, float deltaPitch, boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean sneak) {
            this.deltaYaw = deltaYaw;
            this.deltaPitch = deltaPitch;
            this.forward = forward;
            this.backward = backward;
            this.left = left;
            this.right = right;
            this.jump = jump;
            this.sneak = sneak;
        }
    }
}
