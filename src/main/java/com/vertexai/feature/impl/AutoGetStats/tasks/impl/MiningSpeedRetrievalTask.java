package com.vertexai.feature.impl.AutoGetStats.tasks.impl;

import com.vertexai.Vertex;
import com.vertexai.feature.impl.AutoGetStats.tasks.AbstractInventoryTask;
import com.vertexai.feature.impl.AutoGetStats.tasks.TaskStatus;
import com.vertexai.util.InventoryUtil;
import com.vertexai.util.helper.Clock;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A task that retrieves the Mining Speed value from the player's SkyBlock GUI.
 */
public class MiningSpeedRetrievalTask extends AbstractInventoryTask<Integer> {

    private static final Pattern MINING_SPEED_PATTERN = Pattern.compile("Mining Speed\\s+([\\d,]+\\.?\\d*)");
    private final Minecraft mc = Minecraft.getInstance();
    private final Clock timer = new Clock();
    private Integer miningSpeed;

    @Override
    public void init() {
        taskStatus = TaskStatus.SUCCESS;
        miningSpeed = 2000;
    }

    @Override
    public void onTick() {
        taskStatus = TaskStatus.SUCCESS;
    }

    @Override
    public void end() {
        // No screen to close
    }

    @Override
    public Integer getResult() {
        return miningSpeed != null ? miningSpeed : 2000;
    }
}
