package com.vertexai.macro.impl.MiningMacro;

import lombok.Getter;
import com.vertexai.Vertex;
import com.vertexai.macro.FeatureManager;
import com.vertexai.macro.features.mining.AutoDrillRefuel.AutoDrillRefuel;
import com.vertexai.macro.features.misc.AutoGetStats.AutoGetStats;
import com.vertexai.macro.features.misc.AutoGetStats.tasks.impl.MiningSpeedRetrievalTask;
import com.vertexai.macro.features.misc.AutoGetStats.tasks.impl.PickaxeAbilityRetrievalTask;
import com.vertexai.macro.features.mining.BlockMiner.BlockMiner;
import com.vertexai.macro.AbstractMacro;
import com.vertexai.util.InventoryUtil;
import com.vertexai.util.helper.Clock;
import com.vertexai.util.helper.MineableBlock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>This macro retrieves the player's mining speed before starting the mining loop.
 * It determines which blocks to mine based on Vertex configs, and coordinates
 * with the {@link BlockMiner} to perform mining actions.</p>
 */
public class MiningMacro extends AbstractMacro {

    private static final MiningMacro instance = new MiningMacro();
    public static MiningMacro getInstance() { return instance; }
    private static final int LOW_FUEL_THRESHOLD = 100;

    private final BlockMiner miner = BlockMiner.getInstance();
    private final List<String> necessaryItems = new ArrayList<>();

    private MiningSpeedRetrievalTask miningSpeedRetrievalTask;
    private PickaxeAbilityRetrievalTask pickaxeAbilityRetrievalTask;
    private int miningSpeed = 0;
    private BlockMiner.PickaxeAbility pickaxeAbility =
            BlockMiner.PickaxeAbility.NONE;

    private MineableBlock[] blocksToMine = {};
    private boolean isMining = false;

    @Override
    public String getName() {
        return "Mining Macro";
    }

    private boolean handleRefuelIfNeeded() {
        if (!Vertex.config().general.drillRefuel) return false;

        String tool = Vertex.config().general.miningTool;
        if (tool == null) return false;

        if (!tool.toLowerCase().contains("drill")) return false;

        int fuel = InventoryUtil.getDrillRemainingFuel(tool);

        if (fuel <= LOW_FUEL_THRESHOLD && !AutoDrillRefuel.getInstance().isRunning()) {
            log("Low drill fuel detected (" + fuel + "). Starting auto refuel.");

            miner.stop();          // stop mining safely
            isMining = false;      // allow restart after refuel

            AutoDrillRefuel.FuelType[] fuelTypeMap = {
                    AutoDrillRefuel.FuelType.VOLTA,
                    AutoDrillRefuel.FuelType.OIL_BARREL,
                    AutoDrillRefuel.FuelType.SUNFLOWER_OIL
            };
            AutoDrillRefuel.getInstance().start(tool, fuelTypeMap[Vertex.config().general.refuelMachineFuel]);
            return true;
        }

        return AutoDrillRefuel.getInstance().isRunning();
    }

    @Override
    public List<String> getNecessaryItems() {
        if (necessaryItems.isEmpty()) {
            necessaryItems.add(Vertex.config().general.miningTool);
            log("Necessary items initialized: " + necessaryItems);
        }
        return necessaryItems;
    }

    private Clock statsTimer = new Clock();

    @Override
    public void onEnable() {
        log("Enabling Mining Macro");
        resetVariables();
        setBlocksToMineBasedOnOreType();
        com.vertexai.ui.hud.elements.MiningHUD.getInstance().resetStats();
        statsTimer.schedule(2500);

        if (miningSpeed == 0) {
            miningSpeedRetrievalTask = new MiningSpeedRetrievalTask();
            AutoGetStats.getInstance().startTask(miningSpeedRetrievalTask);

            if (Vertex.config().general.usePickaxeAbility) {
                pickaxeAbilityRetrievalTask = new PickaxeAbilityRetrievalTask();
                AutoGetStats.getInstance().startTask(pickaxeAbilityRetrievalTask);
            }
        }
    }

    @Override
    public void onDisable() {
        log("Disabling Mining Macro");
        miner.stop();
        isMining = false;
        resetVariables();
    }

    private void resetVariables() {
        miningSpeed = 0;
        necessaryItems.clear();
        isMining = false;
    }

    @Override
    public void onPause() {
        FeatureManager.getInstance().pauseAll();
        log("Mining Macro paused");
    }

    @Override
    public void onResume() {
        FeatureManager.getInstance().resumeAll();
        log("Mining Macro resumed");
    }

    public void onTick() {
        if (miningSpeed == 0) {
            handleGettingStats();
            if (miningSpeed == 0) return;
        }

        if (handleRefuelIfNeeded()) {
            return;
        }

        setBlocksToMineBasedOnOreType();
        if (!isMining) {
            miner.setWaitThreshold(
                    Vertex.config().general.oreRespawnWaitThreshold * 1000
            );
            // Use slot if set, otherwise use item name string
            String miningTool = Vertex.config().general.miningTool;
            int miningToolSlot = Vertex.config().general.miningToolSlot;
            String effectiveTool = (miningToolSlot >= 1 && miningToolSlot <= 9) ? String.valueOf(miningToolSlot) : miningTool;

            miner.start(
                    blocksToMine,
                    miningSpeed,
                    pickaxeAbility,
                    determinePriority(),
                    effectiveTool
            );

            isMining = true;
            log("Started mining with speed: " + miningSpeed);
            log(
                    "Started mining with pickaxe ability: " + pickaxeAbility.name()
            );
        }

        handleMining();
    }

    private void handleGettingStats() {
        boolean finished = AutoGetStats.getInstance().hasFinishedAllTasks();
        boolean timedOut = statsTimer.passed();

        if (!finished && !timedOut) return;

        if (miningSpeedRetrievalTask != null && miningSpeedRetrievalTask.getResult() != null) {
            miningSpeed = miningSpeedRetrievalTask.getResult();
        } else {
            miningSpeed = 2000;
        }

        if (Vertex.config().general.usePickaxeAbility && pickaxeAbilityRetrievalTask != null && pickaxeAbilityRetrievalTask.getResult() != null) {
            pickaxeAbility = pickaxeAbilityRetrievalTask.getResult();
        } else {
            pickaxeAbility = BlockMiner.PickaxeAbility.NONE;
        }

        log("Finished getting stats (Speed: " + miningSpeed + ", Ability: " + pickaxeAbility + ")");
    }

    private void handleMining() {
        switch (miner.getError()) {
            case NO_POINTS_FOUND:
                log("Restarting because the block chosen cannot be mined");
                isMining = false;
                break;
            case NO_TARGET_BLOCKS:
                disable(
                        "Please set at least one type of target block in configs!"
                );
                break;
            case NOT_ENOUGH_BLOCKS:
                disable("Not enough blocks nearby! Please move to a new vein");
                break;
            case NO_TOOLS_AVAILABLE:
                disable(
                        "Cannot find tools in hotbar! Please set it in configs"
                );
                break;
            case NO_PICKAXE_ABILITY:
                disable(
                        "Cannot find messages for pickaxe ability! " +
                                "Either enable any pickaxe ability in HOTM or enable chat messages. You can also disable pickaxe ability in configs."
                );
                break;
        }
    }

    private void setBlocksToMineBasedOnOreType() {
        switch (Vertex.config().miningMacro.oreType) {
            case 0:
                List<MineableBlock> list = new ArrayList<>();
                if (Vertex.config().miningMacro.mineGrayMithril) list.add(MineableBlock.GRAY_MITHRIL);
                if (Vertex.config().miningMacro.mineGrayTerracottaMithril) list.add(MineableBlock.GRAY_TERRACOTTA_MITHRIL);
                if (Vertex.config().miningMacro.mineGreenMithril) list.add(MineableBlock.GREEN_MITHRIL);
                if (Vertex.config().miningMacro.mineBlueMithril) list.add(MineableBlock.BLUE_MITHRIL);
                if (Vertex.config().miningMacro.mineTitanium) list.add(MineableBlock.TITANIUM);
                blocksToMine = list.toArray(new MineableBlock[0]);
                break;
            case 1:
                blocksToMine = new MineableBlock[]{MineableBlock.DIAMOND};
                break;
            case 2:
                blocksToMine = new MineableBlock[]{MineableBlock.EMERALD};
                break;
            case 3:
                blocksToMine = new MineableBlock[]{MineableBlock.REDSTONE};
                break;
            case 4:
                blocksToMine = new MineableBlock[]{MineableBlock.LAPIS};
                break;
            case 5:
                blocksToMine = new MineableBlock[]{MineableBlock.GOLD};
                break;
            case 6:
                blocksToMine = new MineableBlock[]{MineableBlock.IRON};
                break;
            case 7:
                blocksToMine = new MineableBlock[]{MineableBlock.COAL};
                break;
            case 8:
                blocksToMine = new MineableBlock[]{MineableBlock.HARDSTONE};
                break;
            case 9:
                blocksToMine = new MineableBlock[]{
                        MineableBlock.RUBY, MineableBlock.OPAL, MineableBlock.SAPPHIRE,
                        MineableBlock.TOPAZ, MineableBlock.AMBER, MineableBlock.JADE,
                        MineableBlock.AMETHYST, MineableBlock.JASPER, MineableBlock.AQUAMARINE,
                        MineableBlock.PERIDOT, MineableBlock.ONYX, MineableBlock.CITRINE
                };
                break;
            case 10:
                blocksToMine = new MineableBlock[]{MineableBlock.GLACITE};
                break;
            case 11:
                blocksToMine = new MineableBlock[]{MineableBlock.TUNGSTEN};
                break;
            case 12:
                blocksToMine = new MineableBlock[]{MineableBlock.UMBER};
                break;
            default:
                blocksToMine = new MineableBlock[]{};
                break;
        }
    }

    private int[] determinePriority() {
        if (Vertex.config().miningMacro.oreType == 0) {
            List<Integer> priorities = new ArrayList<>();
            if (Vertex.config().miningMacro.mineGrayMithril) priorities.add(Vertex.config().miningMacro.mithrilPriorityGrayDefault);
            if (Vertex.config().miningMacro.mineGrayTerracottaMithril) priorities.add(Vertex.config().miningMacro.mithrilPriorityGrayDefault);
            if (Vertex.config().miningMacro.mineGreenMithril) priorities.add(Vertex.config().miningMacro.mithrilPriorityGreenDefault);
            if (Vertex.config().miningMacro.mineBlueMithril) priorities.add(Vertex.config().miningMacro.mithrilPriorityBlueDefault);
            if (Vertex.config().miningMacro.mineTitanium) priorities.add(Vertex.config().miningMacro.mithrilPriorityTitaniumDefault);
            return priorities.stream().mapToInt(i -> i).toArray();
        }
        return new int[]{1, 1, 1, 1};
    }
}
