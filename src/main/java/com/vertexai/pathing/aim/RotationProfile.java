package com.vertexai.pathing.aim;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RotationProfile {

    public List<TickData> ticks = new ArrayList<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public void addTick(float deltaYaw, float deltaPitch, long timeElapsed) {
        ticks.add(new TickData(deltaYaw, deltaPitch, timeElapsed));
    }

    public void save(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static RotationProfile load(File file) {
        if (!file.exists()) return new RotationProfile();
        try (FileReader reader = new FileReader(file)) {
            return GSON.fromJson(reader, RotationProfile.class);
        } catch (IOException e) {
            e.printStackTrace();
            return new RotationProfile();
        }
    }

    public static class TickData {
        public float deltaYaw;
        public float deltaPitch;
        public long timeElapsed;

        public TickData(float deltaYaw, float deltaPitch, long timeElapsed) {
            this.deltaYaw = deltaYaw;
            this.deltaPitch = deltaPitch;
            this.timeElapsed = timeElapsed;
        }
    }
}
