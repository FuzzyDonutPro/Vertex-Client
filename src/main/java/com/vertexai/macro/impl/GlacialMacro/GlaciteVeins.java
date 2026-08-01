package com.vertexai.macro.impl.GlacialMacro;

import lombok.Getter;
import com.vertexai.util.BlockUtil;
import com.vertexai.util.PlayerUtil;
import com.vertexai.util.helper.MineableBlock;
import com.vertexai.util.helper.route.RouteWaypoint;
import com.vertexai.util.helper.route.WaypointType;
import net.minecraft.world.level.block.Block;

import java.util.*;

/**
 * Enum representing the different types of Glacite veins and their properties.
 * Each vein has a display name and a drop name.
 */
public enum GlaciteVeins {

    TOPAZ("Topaz Gemstone", "Rough Topaz Gemstone", MineableBlock.TOPAZ),
    AMBER("Amber Gemstone", "Rough Amber Gemstone", MineableBlock.AMBER),
    SAPPHIRE("Sapphire Gemstone", "Rough Sapphire Gemstone", MineableBlock.SAPPHIRE),
    JADE("Jade Gemstone", "Rough Jade Gemstone", MineableBlock.JADE),
    AMETHYST("Amethyst Gemstone", "Rough Amethyst Gemstone", MineableBlock.AMETHYST),
    RUBY("Ruby Gemstone", "Rough Ruby Gemstone", MineableBlock.RUBY),
    AQUAMARINE("Aquamarine Gemstone", "Rough Aquamarine Gemstone", MineableBlock.AQUAMARINE),
    PERIDOT("Peridot Gemstone", "Rough Peridot Gemstone", MineableBlock.PERIDOT),
    ONYX("Onyx Gemstone", "Rough Onyx Gemstone", MineableBlock.ONYX),
    CITRINE("Citrine Gemstone", "Rough Citrine Gemstone", MineableBlock.CITRINE),
    GLACITE("Glacite", "Glacite", MineableBlock.GLACITE),
    UMBER("Umber", "Umber", MineableBlock.UMBER),
    TUNGSTEN("Tungsten", "Tungsten", MineableBlock.TUNGSTEN);

    private static final Map<GlaciteVeins, RouteWaypoint[]> VEINS = new HashMap<>();
    private static final Map<GlaciteVeins, String> DROP_NAMES_MAP = new HashMap<>();

    private final String dropName;
    private final String displayName;
    private final List<MineableBlock> mineableBlocks;

    public String getDropName() { return dropName; }
    public String getDisplayName() { return displayName; }
    public List<MineableBlock> getMineableBlocks() { return mineableBlocks; }
    public static Map<GlaciteVeins, String> getDropNamesMap() { return DROP_NAMES_MAP; }

    GlaciteVeins(String displayName, String dropName, MineableBlock... blocks) {
        this.displayName = displayName;
        this.dropName = dropName;
        this.mineableBlocks = Collections.unmodifiableList(Arrays.asList(blocks));
    }

    public static RouteWaypoint[] getVeins(GlaciteVeins vein) {
        return VEINS.get(vein);
    }

    public static RouteWaypoint findClosestWaypoint() {
        RouteWaypoint closestWaypoint = null;
        double closestDistance = Double.MAX_VALUE;
        for (RouteWaypoint waypoint : VEINS.get(GLACITE)) {
            double distance = waypoint.distanceTo(PlayerUtil.getBlockStandingOn());
            System.out.println("Distance: " + distance + " to " + waypoint);
            if (distance < 3) return waypoint;

            if (distance < closestDistance) {
                closestDistance = distance;
                closestWaypoint = waypoint;
            }
        }
        return closestWaypoint;
    }

    public static int ticksToMine(GlaciteVeins vein, int miningSpeed) {
        Block block = getRepresentativeBlock(vein);
        if (block == null) {
            return 0;
        }
        return BlockUtil.getMiningTime(block.defaultBlockState(), miningSpeed);
    }

    private static Block getRepresentativeBlock(GlaciteVeins vein) {
        if (vein == null || vein.mineableBlocks == null || vein.mineableBlocks.isEmpty()) {
            return null;
        }
        MineableBlock mineable = vein.mineableBlocks.get(0);
        if (mineable == null || mineable.getBlocks() == null || mineable.getBlocks().isEmpty()) {
            return null;
        }
        return mineable.getBlocks().get(0);
    }

    public static GlaciteVeins getVeinFromName(String name) {
        for (GlaciteVeins vein : GlaciteVeins.values()) {
            if (vein.displayName.toLowerCase().contains(name.toLowerCase())) {
                return vein;
            }
        }
        return null;
    }
}
