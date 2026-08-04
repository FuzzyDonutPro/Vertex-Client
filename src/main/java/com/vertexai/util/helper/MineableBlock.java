package com.vertexai.util.helper;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum MineableBlock {

    QUARTZ(Blocks.QUARTZ_BLOCK, Blocks.NETHER_QUARTZ_ORE),
    DIAMOND(Blocks.DIAMOND_BLOCK),
    EMERALD(Blocks.EMERALD_BLOCK),
    REDSTONE(Blocks.REDSTONE_BLOCK),
    LAPIS(Blocks.LAPIS_BLOCK),
    GOLD(Blocks.GOLD_BLOCK),
    IRON(Blocks.IRON_BLOCK),
    COAL(Blocks.COAL_BLOCK),
    SULPHUR(Blocks.SPONGE),
    HARDSTONE(Blocks.STONE),
    TITANIUM(Blocks.POLISHED_DIORITE),
    // SkyBlock mining blocks (vanilla representations)
    GRAY_MITHRIL(Blocks.GRAY_WOOL, Blocks.LIGHT_GRAY_WOOL),
    GRAY_TERRACOTTA_MITHRIL(Blocks.CYAN_TERRACOTTA, Blocks.GRAY_TERRACOTTA, Blocks.LIGHT_GRAY_TERRACOTTA),
    GREEN_MITHRIL(Blocks.PRISMARINE, Blocks.DARK_PRISMARINE, Blocks.PRISMARINE_BRICKS),
    BLUE_MITHRIL(Blocks.LIGHT_BLUE_WOOL),
    // Gemstones
    OPAL(Blocks.WHITE_STAINED_GLASS, Blocks.WHITE_STAINED_GLASS_PANE),
    JASPER(Blocks.MAGENTA_STAINED_GLASS, Blocks.MAGENTA_STAINED_GLASS_PANE),
    TOPAZ(Blocks.ORANGE_STAINED_GLASS, Blocks.ORANGE_STAINED_GLASS_PANE),
    AMBER(Blocks.YELLOW_STAINED_GLASS, Blocks.YELLOW_STAINED_GLASS_PANE),
    SAPPHIRE(Blocks.LIGHT_BLUE_STAINED_GLASS, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE),
    JADE(Blocks.LIME_STAINED_GLASS, Blocks.LIME_STAINED_GLASS_PANE),
    AMETHYST(Blocks.PURPLE_STAINED_GLASS, Blocks.PURPLE_STAINED_GLASS_PANE),
    RUBY(Blocks.RED_STAINED_GLASS, Blocks.RED_STAINED_GLASS_PANE),
    AQUAMARINE(Blocks.CYAN_STAINED_GLASS, Blocks.CYAN_STAINED_GLASS_PANE),
    PERIDOT(Blocks.GREEN_STAINED_GLASS, Blocks.GREEN_STAINED_GLASS_PANE),
    ONYX(Blocks.BLACK_STAINED_GLASS, Blocks.BLACK_STAINED_GLASS_PANE),
    CITRINE(Blocks.BROWN_STAINED_GLASS, Blocks.BROWN_STAINED_GLASS_PANE),
    GLACITE(Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE),
    TUNGSTEN(Blocks.GRAY_CONCRETE),
    UMBER(Blocks.BROWN_CONCRETE),
    ANHYDRITE(Blocks.WHITE_CONCRETE);

    private final List<Block> block;

    MineableBlock(Block... blocks) {
        this.block = Arrays.asList(blocks);
    }

    public static List<Block> getAllMineableBlocks() {
        List<Block> allBlocks = new ArrayList<>();
        for (MineableBlock mineableBlock : MineableBlock.values()) {
            allBlocks.addAll(mineableBlock.getBlocks());
        }
        return allBlocks;
    }

    public List<Block> getBlocks() {
        return Collections.unmodifiableList(block);
    }
}
