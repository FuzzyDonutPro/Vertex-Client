package com.vertexai.ui.hud.elements;

import com.vertexai.client.overlay.TextHud;
import com.vertexai.macro.features.mining.BlockMiner.BlockMiner;
import com.vertexai.macro.impl.MiningMacro.MiningMacro;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Custom Mining HUD Overlay for Vertex Client.
 * Displays real-time status, target block, mining speed, blocks mined count, mining rate, and runtime.
 */
public class MiningHUD extends TextHud {

    private static final MiningHUD instance = new MiningHUD();

    private long startTime = -1;
    private int blocksMinedCount = 0;
    private BlockPos lastTarget = null;

    public MiningHUD() {
        super();
        this.x = 5;
        this.y = 130;
        this.anchor = 0; // Top-Left
        this.enabled = true;
    }

    public static MiningHUD getInstance() {
        return instance;
    }

    @Override
    protected int getAccentColor() {
        return 0xFF06B6D4; // Cyan 500
    }

    public void incrementBlocksMined() {
        this.blocksMinedCount++;
    }

    public void resetStats() {
        this.blocksMinedCount = 0;
        this.startTime = -1;
        this.lastTarget = null;
    }

    @Override
    protected void getLines(List<String> lines, boolean example) {
        if (example) {
            lines.add("§b§lVERTEX §7| §f§lMINING OVERLAY");
            lines.add("§8§m------------------------");
            lines.add("§8» §7Status: §aMINING");
            lines.add("§8» §7Target: §bMithril Ore §8(§73.2m§8)");
            lines.add("§8» §7Mining Speed: §e2,450");
            lines.add("§8» §7Mined: §a+142 §8(§71,850/hr§8)");
            lines.add("§8» §7Runtime: §f00:14:22");
            return;
        }

        lines.add("§b§lVERTEX §7| §f§lMINING OVERLAY");
        lines.add("§8§m------------------------");

        if (mc.player == null || mc.level == null) {
            lines.add("§8» §cOffline");
            return;
        }

        MiningMacro macro = MiningMacro.getInstance();
        boolean isRunning = macro != null && macro.isEnabled();
        BlockMiner miner = BlockMiner.getInstance();

        if (!isRunning) {
            this.startTime = -1;
            lines.add("§8» §7Status: §7IDLE");
            lines.add("§8» §7Mining Speed: §e" + String.format("%,d", miner.getMiningSpeed() > 0 ? miner.getMiningSpeed() : 2000));
            lines.add("§8» §7Runtime: §700:00:00");
            return;
        }

        if (this.startTime == -1) {
            this.startTime = System.currentTimeMillis();
        }

        long elapsedSec = Math.max(1, (System.currentTimeMillis() - this.startTime) / 1000);
        long ratePerHour = (blocksMinedCount * 3600L) / elapsedSec;

        BlockPos target = miner.getTargetBlockPos();
        if (target != null && !target.equals(lastTarget)) {
            lastTarget = target;
        }

        String targetStr;
        if (target != null && mc.level != null) {
            String blockName = mc.level.getBlockState(target).getBlock().getName().getString();
            double dist = Math.sqrt(mc.player.distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5));
            targetStr = "§b" + blockName + " §8(§7" + String.format("%.1fm", dist) + "§8)";
        } else {
            targetStr = "§7Searching...";
        }

        int speed = miner.getMiningSpeed() > 0 ? miner.getMiningSpeed() : 2000;
        String statusStr = miner.isRunning() ? "§aMINING" : "§eAIMING";

        lines.add("§8» §7Status: " + statusStr);
        lines.add("§8» §7Target: " + targetStr);
        lines.add("§8» §7Mining Speed: §e" + String.format("%,d", speed));
        lines.add("§8» §7Mined: §a+" + blocksMinedCount + " §8(§7" + String.format("%,d/hr", ratePerHour) + "§8)");
        lines.add("§8» §7Runtime: §f" + formatTime(elapsedSec));
    }

    private String formatTime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    @Override
    protected boolean shouldShow() {
        if (!enabled || mc.player == null || mc.level == null) return false;
        MiningMacro macro = MiningMacro.getInstance();
        return macro != null && macro.isEnabled();
    }
}
