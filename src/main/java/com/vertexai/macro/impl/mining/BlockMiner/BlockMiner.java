package com.vertexai.macro.impl.mining.BlockMiner;

import lombok.Getter;
import lombok.Setter;
import com.vertexai.Vertex;
import com.vertexai.event.BlockChangeEvent;
import com.vertexai.event.SpawnParticleEvent;
import com.vertexai.macro.AbstractMacro;
import com.vertexai.macro.impl.mining.AutoDrillRefuel.AutoDrillRefuel;
import com.vertexai.macro.impl.mining.BlockMiner.states.ApplyAbilityState;
import com.vertexai.macro.impl.mining.BlockMiner.states.BlockMinerState;
import com.vertexai.macro.impl.mining.BlockMiner.states.StartingState;
import com.vertexai.macro.impl.misc.AutoGetStats.AutoGetStats;
import com.vertexai.macro.impl.misc.AutoGetStats.tasks.impl.MiningSpeedRetrievalTask;
import com.vertexai.macro.impl.misc.AutoGetStats.tasks.impl.PickaxeAbilityRetrievalTask;
import com.vertexai.util.InventoryUtil;
import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.RenderUtil;
import com.vertexai.util.helper.Clock;
import com.vertexai.util.helper.MineableBlock;
import com.vertexai.util.WorldRenderContextWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BlockMiner
 * <p>
 * Complete unified block mining macro and state machine.
 * Handles stats retrieval, drill auto-refuel, block targeting priorities,
 * rotation aiming, and 1.21.11 block destruction.
 */
public class BlockMiner extends AbstractMacro {

    public enum BlockMinerError {
        NONE,
        NO_POINTS_FOUND,
        NO_TARGET_BLOCKS,
        NOT_ENOUGH_BLOCKS,
        NO_TOOLS_AVAILABLE,
        NO_PICKAXE_ABILITY
    }

    public enum PickaxeAbility {
        NONE,
        MINING_SPEED_BOOST,
        PICKOBULUS,
        ANOMALY,
        MANIAC_MINER,
        GEMSTONE_INFUSION,
        HAZARDOUS_RE_MINE
    }

    public enum PickaxeAbilityState {
        AVAILABLE,
        COOLDOWN,
        ACTIVE,
        UNAVAILABLE
    }

    public static final long DEFAULT_PICKAXE_ABILITY_COOLDOWN_MS = 60000L;
    private static final Pattern PICKAXE_COOLDOWN_PATTERN = Pattern.compile("cooldown for\\s+(\\d+)\\s*([sm])");
    private static final int LOW_FUEL_THRESHOLD = 100;
    private static final BlockMiner instance = new BlockMiner();

    private final Map<Block, Integer> blockPriority = new HashMap<>();
    private final List<String> necessaryItems = new ArrayList<>();
    private BlockMinerState currentState;
    private long lastAbilityUse = System.currentTimeMillis();
    private BlockMinerError error = BlockMinerError.NONE;
    private int retryActivatePickaxeAbility;
    private BlockPos targetBlockPos;
    private Block targetBlockType;
    private Vec3 targetParticlePos;
    private int miningSpeed = 0;
    private PickaxeAbility pickaxeAbility = PickaxeAbility.NONE;
    private int waitThreshold;
    private PickaxeAbilityState pickaxeAbilityState = PickaxeAbilityState.AVAILABLE;
    private long pickaxeAbilityCooldownEndMs;
    private boolean blockChanged;
    private BlockPos startPos;
    private Vec3 targetPoint;
    private net.minecraft.core.Direction miningDirection;

    private MiningSpeedRetrievalTask miningSpeedRetrievalTask;
    private PickaxeAbilityRetrievalTask pickaxeAbilityRetrievalTask;
    private MineableBlock[] blocksToMine = {};
    private boolean isMining = false;
    private final Clock statsTimer = new Clock();

    public static BlockMiner getInstance() {
        return instance;
    }

    @Override
    public String getName() {
        return "Mining Macro";
    }

    public BlockPos getStartPos() { return startPos; }
    public void setStartPos(BlockPos startPos) { this.startPos = startPos; }
    public Vec3 getTargetPoint() { return targetPoint; }
    public void setTargetPoint(Vec3 targetPoint) { this.targetPoint = targetPoint; }
    public net.minecraft.core.Direction getMiningDirection() { return miningDirection; }
    public void setMiningDirection(net.minecraft.core.Direction miningDirection) { this.miningDirection = miningDirection; }
    public Map<Block, Integer> getBlockPriority() { return blockPriority; }
    public long getLastAbilityUse() { return lastAbilityUse; }
    public void setLastAbilityUse(long lastAbilityUse) { this.lastAbilityUse = lastAbilityUse; }
    public BlockMinerError getError() { return error; }
    public void setError(BlockMinerError error) { this.error = error; }
    public BlockPos getTargetBlockPos() { return targetBlockPos; }
    public void setTargetBlockPos(BlockPos targetBlockPos) { this.targetBlockPos = targetBlockPos; }
    public Block getTargetBlockType() { return targetBlockType; }
    public void setTargetBlockType(Block targetBlockType) { this.targetBlockType = targetBlockType; }
    public Vec3 getTargetParticlePos() { return targetParticlePos; }
    public void setTargetParticlePos(Vec3 targetParticlePos) { this.targetParticlePos = targetParticlePos; }
    public int getMiningSpeed() { return miningSpeed; }
    public void setMiningSpeed(int miningSpeed) { this.miningSpeed = miningSpeed; }
    public PickaxeAbility getPickaxeAbility() { return pickaxeAbility; }
    public void setPickaxeAbility(PickaxeAbility pickaxeAbility) { this.pickaxeAbility = pickaxeAbility; }
    public int getWaitThreshold() { return waitThreshold; }
    public void setWaitThreshold(int waitThreshold) { this.waitThreshold = waitThreshold; }
    public PickaxeAbilityState getPickaxeAbilityState() { return pickaxeAbilityState; }
    public void setPickaxeAbilityState(PickaxeAbilityState pickaxeAbilityState) { this.pickaxeAbilityState = pickaxeAbilityState; }
    public long getPickaxeAbilityCooldownEndMs() { return pickaxeAbilityCooldownEndMs; }
    public void setPickaxeAbilityCooldownEndMs(long pickaxeAbilityCooldownEndMs) { this.pickaxeAbilityCooldownEndMs = pickaxeAbilityCooldownEndMs; }
    public boolean isBlockChanged() { return blockChanged; }
    public void setBlockChanged(boolean blockChanged) { this.blockChanged = blockChanged; }
    public boolean isRunning() { return isEnabled(); }

    public void start(MineableBlock[] blocks, int miningSpeed, PickaxeAbility ability, int[] priority, String tool) {
        if (!isEnabled()) {
            enable();
        }
        this.blocksToMine = blocks;
        this.miningSpeed = miningSpeed;
        this.pickaxeAbility = ability;
        startMiningLoop(blocks, miningSpeed, ability, priority, tool);
    }

    public void stop() {
        disable();
    }

    @Override
    public List<String> getNecessaryItems() {
        if (necessaryItems.isEmpty()) {
            necessaryItems.add(Vertex.config().general.miningTool);
        }
        return necessaryItems;
    }

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
        stopStateMachine();
        resetVariables();
    }

    @Override
    public void onPause() {
        stopStateMachine();
        log("Mining Macro paused");
    }

    @Override
    public void onResume() {
        log("Mining Macro resumed");
    }

    private void resetVariables() {
        miningSpeed = 0;
        necessaryItems.clear();
        isMining = false;
        error = BlockMinerError.NONE;
    }

    private boolean handleRefuelIfNeeded() {
        if (!Vertex.config().general.drillRefuel) return false;

        String tool = Vertex.config().general.miningTool;
        if (tool == null) return false;
        if (!tool.toLowerCase().contains("drill")) return false;

        int fuel = InventoryUtil.getDrillRemainingFuel(tool);

        if (fuel <= LOW_FUEL_THRESHOLD && !AutoDrillRefuel.getInstance().isRunning()) {
            log("Low drill fuel detected (" + fuel + "). Starting auto refuel.");
            stopStateMachine();
            isMining = false;

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
    public void onTick() {
        if (!isEnabled()) return;

        if (miningSpeed == 0) {
            handleGettingStats();
            if (miningSpeed == 0) return;
        }

        if (handleRefuelIfNeeded()) {
            return;
        }

        setBlocksToMineBasedOnOreType();

        if (!isMining) {
            this.waitThreshold = Vertex.config().general.oreRespawnWaitThreshold * 1000;
            String miningTool = Vertex.config().general.miningTool;
            int miningToolSlot = Vertex.config().general.miningToolSlot;
            String effectiveTool = (miningToolSlot >= 1 && miningToolSlot <= 9) ? String.valueOf(miningToolSlot) : miningTool;

            startMiningLoop(blocksToMine, miningSpeed, pickaxeAbility, determinePriority(), effectiveTool);
            isMining = true;
            log("Started mining with speed: " + miningSpeed + ", Ability: " + pickaxeAbility.name());
        }

        handleErrors();

        // Run state machine tick
        if (currentState != null) {
            BlockMinerState nextState = currentState.onTick(this);
            if (nextState != currentState) {
                currentState.onEnd(this);
                currentState = nextState;
                if (currentState != null) {
                    currentState.onStart(this);
                }
            }
        }
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
            pickaxeAbility = PickaxeAbility.NONE;
        }

        log("Finished getting stats (Speed: " + miningSpeed + ", Ability: " + pickaxeAbility + ")");
    }

    private void handleErrors() {
        switch (error) {
            case NO_POINTS_FOUND:
                log("Restarting because block chosen cannot be mined");
                isMining = false;
                error = BlockMinerError.NONE;
                break;
            case NO_TARGET_BLOCKS:
                disable("Please set at least one type of target block in configs!");
                break;
            case NOT_ENOUGH_BLOCKS:
                disable("Not enough blocks nearby! Please move to a new vein");
                break;
            case NO_TOOLS_AVAILABLE:
                disable("Cannot find tools in hotbar! Please set it in configs");
                break;
            case NO_PICKAXE_ABILITY:
                disable("Cannot find messages for pickaxe ability! Check HOTM or chat settings.");
                break;
        }
    }

    private void startMiningLoop(MineableBlock[] blocks, int miningSpeed, PickaxeAbility ability, int[] priority, String tool) {
        if (blocks == null || blocks.length == 0) {
            error = BlockMinerError.NO_TARGET_BLOCKS;
            return;
        }

        if (priority == null || priority.length != blocks.length) {
            int[] defaultPriority = new int[blocks.length];
            Arrays.fill(defaultPriority, 1);
            priority = defaultPriority;
        }

        blockPriority.clear();
        for (int i = 0; i < blocks.length; i++) {
            for (Block block : blocks[i].getBlocks()) {
                blockPriority.put(block, priority[i]);
            }
        }

        if (tool != null && !tool.isEmpty()) {
            try {
                int slot = Integer.parseInt(tool);
                if (slot >= 1 && slot <= 9) {
                    InventoryUtil.selectSlot(slot - 1);
                } else {
                    InventoryUtil.holdItem(tool);
                }
            } catch (NumberFormatException e) {
                InventoryUtil.holdItem(tool);
            }
        }

        this.miningSpeed = miningSpeed;
        this.pickaxeAbility = ability;
        this.startPos = com.vertexai.util.PlayerUtil.getBlockStandingOn();

        if (currentState != null) {
            currentState.onEnd(this);
        }
        currentState = new StartingState();
        currentState.onStart(this);
    }

    public void stopStateMachine() {
        if (currentState != null) {
            currentState.onEnd(this);
            currentState = null;
        }
        isMining = false;
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
