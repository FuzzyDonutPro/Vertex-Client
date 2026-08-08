package com.vertexai.render;

import lombok.Getter;
import com.vertexai.macro.AbstractFeature;
import com.vertexai.macro.features.mining.BlockMiner.BlockMiner;
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
 * WorldESP â€” Renders 3D ESP boxes for:
 * 1. Target block currently being mined by the Mining Macro
 * 2. Secret & Dungeon Chests
 * 3. Garden Visitors & NPCs
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

                    if (state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST) || state.is(Blocks.ENDER_CHEST)) {
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
        var config = com.vertexai.Vertex.config();
        if (!enabled || mc.player == null) return;
        if (config != null && config.utilities != null && !config.utilities.enableWorldEsp) return;

        boolean seeThrough = config != null && config.utilities != null && config.utilities.renderEspThroughWalls;

        // A. Render 3D ESP on the block currently being mined by Mining Macro (Bright Emerald Green)
        BlockMiner miner = BlockMiner.getInstance();
        if (miner != null && miner.isRunning() && miner.getTargetBlockPos() != null) {
            BlockPos targetMiningBlock = miner.getTargetBlockPos();
            Color miningColor = new Color(34, 197, 94, 220); // Bright Emerald Green
            RenderUtil.drawBlock(targetMiningBlock, miningColor, seeThrough);
            RenderUtil.outlineBlock(targetMiningBlock, new Color(255, 255, 255, 240), seeThrough);
        }

        // B. Render 3D ESP on Secret / Dungeon Chests (Amber Gold)
        Color chestColor = new Color(245, 158, 11, 200); // Amber Gold
        for (BlockPos chestPos : chestBlocks) {
            RenderUtil.outlineBlock(chestPos, chestColor, seeThrough);
        }

        // C. Render 3D ESP on Garden Visitors (Violet Purple Box)
        Color visitorColor = new Color(168, 85, 247, 200); // Violet Purple
        for (LivingEntity visitor : gardenVisitors) {
            AABB boundingBox = visitor.getBoundingBox();
            RenderUtil.drawAABB(boundingBox, visitorColor, false, seeThrough);
        }

        // D. 5x5 NoRender Zone Box Indicator
        if (config != null && config.misc.noRenderMode && com.vertexai.macro.MacroManager.getInstance().isRunning()) {
            BlockPos p = mc.player.blockPosition();
            AABB zoneBox = new AABB(p.getX() - 2, p.getY() - 1, p.getZ() - 2, p.getX() + 3, p.getY() + 3, p.getZ() + 3);
            RenderUtil.drawAABB(zoneBox, new Color(59, 130, 246, 160), false, seeThrough);
        }
    }
}
