package com.vertexai.macro.impl.RouteMiner;

import lombok.Getter;
import lombok.Setter;
import com.vertexai.Vertex;
import com.vertexai.feature.FeatureManager;
import com.vertexai.feature.impl.BlockMiner.BlockMiner;
import com.vertexai.handler.RotationHandler;
import com.vertexai.handler.RouteHandler;
import com.vertexai.macro.AbstractMacro;
import com.vertexai.macro.MacroManager;
import com.vertexai.macro.impl.RouteMiner.states.RouteMinerMacroState;
import com.vertexai.macro.impl.RouteMiner.states.StartingState;
import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.PlayerUtil;
import com.vertexai.util.helper.MineableBlock;
import com.vertexai.util.helper.route.Route;
import com.vertexai.util.helper.route.RouteWaypoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class RouteMinerMacro extends AbstractMacro {

    @Getter
    private static final RouteMinerMacro instance = new RouteMinerMacro();

    @Setter
    @Getter
    private RouteMinerMacroState currentState;

    @Getter
    @Setter
    private int miningSpeed = 0;

    @Getter
    @Setter
    private BlockMiner.PickaxeAbility pickaxeAbility = BlockMiner.PickaxeAbility.NONE;

    @Getter
    @Setter
    private int routeIndex = 0;

    @Override
    public void onEnable() {
        RouteHandler routeHandler = RouteHandler.getInstance();
        Route route = routeHandler.getSelectedRoute();
        int waypointCount = route == null ? 0 : route.size();

        if (waypointCount == 0) {
            error("Selected route '" + routeHandler.getSelectedRouteName() + "' has no waypoints. "
                    + "Enable RouteBuilder and add points with P/I or /rb add <walk|etherwarp|mine>.");
            MacroManager.getInstance().disable();
            return;
        }

        BlockMiner.getInstance().setWaitThreshold(50);
        Optional<RouteWaypoint> waypoint = route.getClosest(PlayerUtil.getBlockStandingOn());
        waypoint.ifPresent(routeWaypoint -> routeIndex = route.indexOf(routeWaypoint));

        this.miningSpeed = 0;
        this.pickaxeAbility = BlockMiner.PickaxeAbility.NONE;
        this.currentState = new StartingState();

        log("Route Miner Macro enabled");
    }

    @Override
    public void onDisable() {
        if (currentState != null) {
            currentState.onEnd(this);
        }

        this.miningSpeed = 0;
        currentState = null;
        log("Route Miner Macro disabled");
        KeyBindUtil.releaseAllExcept();
        RotationHandler.getInstance().stop();
        FeatureManager.getInstance().disableAll();
    }

    @Override
    public String getName() {
        return "Route Miner";
    }

    @Override
    public void onTick() {
        if (!this.isEnabled()) {
            return;
        }

        if (isTimerRunning() || currentState == null) {
            return;
        }

        RouteMinerMacroState nextState = currentState.onTick(this);
        transitionTo(nextState);
    }

    public void transitionTo(RouteMinerMacroState nextState) {
        if (currentState == nextState || nextState == null) {
            return;
        }

        currentState.onEnd(this);
        currentState = nextState;
        currentState.onStart(this);
    }

    @Override
    public void onPause() {
        FeatureManager.getInstance().pauseAll();
        log("Route Miner Macro Paused");
    }

    @Override
    public void onResume() {
        FeatureManager.getInstance().resumeAll();
        log("Route Miner Macro Resumed");
    }

    public MineableBlock[] getBlocksToMine() {
        List<MineableBlock> blocksList = new ArrayList<>();

        if (Vertex.config().routeMiner.routeMineGemstone) {
            blocksList.add(MineableBlock.RUBY);
            blocksList.add(MineableBlock.AMBER);
            blocksList.add(MineableBlock.AMETHYST);
            blocksList.add(MineableBlock.JADE);
            blocksList.add(MineableBlock.SAPPHIRE);
            blocksList.add(MineableBlock.AQUAMARINE);
            blocksList.add(MineableBlock.ONYX);
            blocksList.add(MineableBlock.CITRINE);
            blocksList.add(MineableBlock.PERIDOT);
            blocksList.add(MineableBlock.JASPER);
        }

        if (Vertex.config().routeMiner.routeMineOre) {
            blocksList.add(MineableBlock.IRON);
            blocksList.add(MineableBlock.REDSTONE);
            blocksList.add(MineableBlock.COAL);
            blocksList.add(MineableBlock.GOLD);
            blocksList.add(MineableBlock.LAPIS);
            blocksList.add(MineableBlock.DIAMOND);
            blocksList.add(MineableBlock.EMERALD);
            blocksList.add(MineableBlock.QUARTZ);
        }

        if (Vertex.config().routeMiner.routeMineTopaz) blocksList.add(MineableBlock.TOPAZ);
        if (Vertex.config().routeMiner.routeMineGlacite) blocksList.add(MineableBlock.GLACITE);
        if (Vertex.config().routeMiner.routeMineUmber) blocksList.add(MineableBlock.UMBER);
        if (Vertex.config().routeMiner.routeMineTungsten) blocksList.add(MineableBlock.TUNGSTEN);

        return blocksList.stream().distinct().toArray(MineableBlock[]::new);
    }

    public int[] getBlockPriority() {
        MineableBlock[] blocksToMine = getBlocksToMine();
        int[] priorities = new int[blocksToMine.length];
        Arrays.fill(priorities, 1);
        return priorities;
    }

    @Override
    public List<String> getNecessaryItems() {
        List<String> items = new ArrayList<>();
        items.add("Aspect of the Void");
        items.add(Vertex.config().general.miningTool);
        return items;
    }

}
