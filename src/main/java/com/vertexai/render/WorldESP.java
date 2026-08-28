package com.vertexai.render;

import lombok.Getter;
import com.vertexai.feature.AbstractFeature;
import com.vertexai.feature.impl.BlockMiner.BlockMiner;
import com.vertexai.macro.impl.ForagingMacro.ForagingMacro;
import com.vertexai.util.RenderUtil;
import com.vertexai.util.WorldRenderContextWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * WorldESP — Renders 3D ESP boxes for:
 * 1. Target block currently being mined by Mining Macros & BlockMiner
 * 2. Target tree logs for Foraging Macro
 * 3. Secret & Dungeon Chests
 * 4. Garden Visitors & NPCs
 */
public class WorldESP extends AbstractFeature {

    @Getter
    public static final WorldESP instance = new WorldESP();

    private final List<BlockPos> chestBlocks = new ArrayList<>();
    private final List<LivingEntity> gardenVisitors = new ArrayList<>();
    private long lastScanTime = 0;

    public WorldESP() {
        this.enabled = true;
    }

    @Override
    public String getName() {
        return "WorldESP";
    }

    @Override
    public boolean shouldStartAtLaunch() {
        return true;
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public void stop() {
        // Persistent background render feature - do not kill on macro stop
        this.enabled = true;
    }

    @Override
    protected void onTick() {
        if (mc.player == null || mc.level == null) return;
        if (System.currentTimeMillis() - lastScanTime < 1000) return; // Scan every 1s

        lastScanTime = System.currentTimeMillis();
        chestBlocks.clear();
        gardenVisitors.clear();

        // 1. Scan for Secret & Dungeon Chests
        BlockPos playerPos = mc.player.blockPosition();
        int radius = 16;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    BlockState state = mc.level.getBlockState(pos);

                    if (state.getBlock() == Blocks.CHEST || state.getBlock() == Blocks.TRAPPED_CHEST || state.getBlock() == Blocks.ENDER_CHEST) {
                        chestBlocks.add(pos);
                    }
                }
            }
        }

        // 2. Scan for Garden Visitors (Villagers, ArmorStands, or NPCs with visitor names)
        StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false)
                .filter(e -> e instanceof LivingEntity && e != mc.player && e.isAlive())
                .map(e -> (LivingEntity) e)
                .filter(this::isGardenVisitor)
                .forEach(gardenVisitors::add);
    }

    private boolean isGardenVisitor(LivingEntity entity) {
        String typeName = entity.getType().toString().toLowerCase();
        if (typeName.contains("villager")) return true;
        if (entity instanceof ArmorStand && entity.hasCustomName()) {
            String name = entity.getCustomName().getString().toLowerCase();
            return name.contains("visitor") || name.contains("guest") || name.contains("spaceman") ||
                   name.contains("jacob") || name.contains("farmer") || name.contains("beth");
        }
        String name = entity.getName().getString().toLowerCase();
        return name.contains("visitor") || name.contains("guest") || name.contains("spaceman") ||
               name.contains("jacob") || name.contains("farmer") || name.contains("beth");
    }

    @Override
    public void onWorldRender(WorldRenderContextWrapper context) {
        if (mc.player == null || mc.level == null) return;

        // A. Render 3D ESP on the block currently being mined by Mining Macros / BlockMiner
        BlockMiner miner = BlockMiner.getInstance();
        if (miner != null && miner.getTargetBlockPos() != null) {
            BlockPos targetMiningBlock = miner.getTargetBlockPos();
            Color miningFill = new Color(34, 197, 94, 140); // Bright Emerald Green
            Color miningOutline = new Color(74, 222, 128, 255); // Neon Green
            RenderUtil.drawBlock(targetMiningBlock, miningFill);
            RenderUtil.outlineBlock(targetMiningBlock, miningOutline);

            if (miner.getTargetParticlePos() != null) {
                RenderUtil.drawPoint(miner.getTargetParticlePos(), new Color(250, 204, 21, 240));
            }
        }

        // B. Render 3D ESP on target tree log for Foraging Macro
        ForagingMacro foraging = ForagingMacro.getInstance();
        if (foraging != null && foraging.isEnabled() && foraging.getTargetBlockPos() != null) {
            BlockPos targetLog = foraging.getTargetBlockPos();
            RenderUtil.drawBlock(targetLog, new Color(56, 189, 248, 140)); // Sky Blue Fill
            RenderUtil.outlineBlock(targetLog, new Color(14, 165, 233, 255)); // Bright Blue Outline
        }

        // C. Render 3D ESP on Secret / Dungeon Chests (Amber Gold)
        Color chestColor = new Color(245, 158, 11, 200); // Amber Gold
        for (BlockPos chestPos : chestBlocks) {
            RenderUtil.outlineBlock(chestPos, chestColor);
        }

        // D. Render 3D ESP on Garden Visitors (Violet Purple Box)
        Color visitorColor = new Color(168, 85, 247, 200); // Violet Purple
        for (LivingEntity visitor : gardenVisitors) {
            AABB boundingBox = visitor.getBoundingBox();
            RenderUtil.drawAABB(boundingBox, visitorColor, false);
        }

        // E. 5x5 NoRender Zone Box Indicator
        var config = com.vertexai.Vertex.config();
        if (config != null && config.misc.noRenderMode && com.vertexai.macro.MacroManager.getInstance().isRunning()) {
            BlockPos p = mc.player.blockPosition();
            AABB zoneBox = new AABB(p.getX() - 2, p.getY() - 1, p.getZ() - 2, p.getX() + 3, p.getY() + 3, p.getZ() + 3);
            RenderUtil.drawAABB(zoneBox, new Color(59, 130, 246, 160), false);
        }
    }
}
