package com.vertexai.macro.impl.mining.BlockMiner;

import lombok.Getter;
import lombok.Setter;
import com.vertexai.Vertex;
import com.vertexai.event.BlockChangeEvent;
import com.vertexai.event.SpawnParticleEvent;
import com.vertexai.macro.AbstractMacro;
import com.vertexai.macro.impl.mining.BlockMiner.states.ApplyAbilityState;
import com.vertexai.macro.impl.mining.BlockMiner.states.BlockMinerState;
import com.vertexai.macro.impl.mining.BlockMiner.states.StartingState;
import com.vertexai.util.InventoryUtil;
import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.Logger;
import com.vertexai.util.RenderUtil;
import com.vertexai.util.helper.MineableBlock;
import com.vertexai.util.WorldRenderContextWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BlockMiner
 * <p>
 * Main controller class for automatic block mining feature.
 * Implements a state machine pattern to manage different phases of the mining process.
 * Handles mining block selection, breaking, and speed boost management.
 */
public class BlockMiner extends AbstractMacro {

    public static final long DEFAULT_PICKAXE_ABILITY_COOLDOWN_MS = 60000L;
    private static final Pattern PICKAXE_COOLDOWN_PATTERN = Pattern.compile("cooldown for\\s+(\\d+)\\s*([sm])");
    private static BlockMiner instance;

    @Getter
    private final Map<Block, Integer> blockPriority = new HashMap<>();
    private BlockMinerState currentState;

    @Getter
    @Setter
    private long lastAbilityUse = System.currentTimeMillis();

    @Getter
    @Setter
    private BlockMinerError error = BlockMinerError.NONE;

    private int retryActivatePickaxeAbility;

    @Getter
    @Setter
    private BlockPos targetBlockPos;

    @Getter
    @Setter
    private Block targetBlockType;

    @Getter
    @Setter
    private Vec3 targetParticlePos;

    @Getter
    @Setter
    private Vec3 targetPoint;

    @Getter
    @Setter
    private Direction miningDirection;

    @Getter
    @Setter
    private BlockPos startPos;

    @Getter
    @Setter
    private int miningSpeed;

    @Getter
    @Setter
    private PickaxeAbility pickaxeAbility;

    @Getter
    @Setter
    private int waitThreshold;

    @Getter
    @Setter
    private PickaxeAbilityState pickaxeAbilityState = PickaxeAbilityState.AVAILABLE;

    @Getter
    @Setter
    private long pickaxeAbilityCooldownEndMs;

    @Getter
    @Setter
    private boolean blockChanged;

    public static BlockMiner getInstance() {
        if (instance == null) {
            instance = new BlockMiner();
        }
        return instance;
    }

    public boolean isRunning() {
        return isEnabled();
    }

    public void logError(String msg) {
        Logger.sendLog("[BlockMiner] ERROR: " + msg);
    }

    public void sendError(String msg) {
        Logger.sendLog("[BlockMiner] " + msg);
    }

    private static long parseCooldownMs(String messageLower) {
        Matcher matcher = PICKAXE_COOLDOWN_PATTERN.matcher(messageLower);
        if (!matcher.find()) {
            return -1L;
        }

        try {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            if (value <= 0L) {
                return -1L;
            }

            if ("m".equals(unit)) {
                return value * 60_000L;
            }
            return value * 1000L;
        } catch (RuntimeException ignored) {
            return -1L;
        }
    }

    @Override
    public String getName() {
        return "BlockMiner";
    }

    @Override
    public List<String> getNecessaryItems() {
        String tool = Vertex.config().general.miningTool;
        return tool != null && !tool.isEmpty() ? List.of(tool) : List.of();
    }

    @Override
    public void onEnable() {
        int oreIdx = Vertex.config().miningMacro.oreType;
        setupPrioritiesForOreType(oreIdx);
        String tool = Vertex.config().general.miningTool;
        PickaxeAbility ability = Vertex.config().general.usePickaxeAbility ? PickaxeAbility.MINING_SPEED_BOOST : PickaxeAbility.NONE;
        int detectedSpeed = com.vertexai.macro.impl.misc.AutoGetStats.tasks.impl.MiningSpeedRetrievalTask.detectMiningSpeed();
        startWithConfiguredPriorities(detectedSpeed, ability, tool != null ? tool : "");
    }

    public void setupPrioritiesForOreType(int oreTypeIdx) {
        blockPriority.clear();
        List<MineableBlock> targetEnums = new java.util.ArrayList<>();
        switch (oreTypeIdx) {
            case 0: // Mithril & Titanium
                targetEnums.addAll(List.of(
                        MineableBlock.GREEN_MITHRIL,
                        MineableBlock.BLUE_MITHRIL,
                        MineableBlock.GRAY_MITHRIL,
                        MineableBlock.GRAY_TERRACOTTA_MITHRIL,
                        MineableBlock.TITANIUM
                ));
                break;
            case 1: targetEnums.add(MineableBlock.DIAMOND); break;
            case 2: targetEnums.add(MineableBlock.EMERALD); break;
            case 3: targetEnums.add(MineableBlock.REDSTONE); break;
            case 4: targetEnums.add(MineableBlock.LAPIS); break;
            case 5: targetEnums.add(MineableBlock.GOLD); break;
            case 6: targetEnums.add(MineableBlock.IRON); break;
            case 7: targetEnums.add(MineableBlock.COAL); break;
            case 8: targetEnums.add(MineableBlock.HARDSTONE); break;
            case 9: // Gemstones
                targetEnums.addAll(List.of(
                        MineableBlock.RUBY, MineableBlock.SAPPHIRE, MineableBlock.JASPER, MineableBlock.TOPAZ,
                        MineableBlock.AMBER, MineableBlock.JADE, MineableBlock.AMETHYST, MineableBlock.OPAL,
                        MineableBlock.AQUAMARINE, MineableBlock.PERIDOT, MineableBlock.ONYX, MineableBlock.CITRINE
                ));
                break;
            case 10: targetEnums.add(MineableBlock.GLACITE); break;
            case 11: targetEnums.add(MineableBlock.TUNGSTEN); break;
            case 12: targetEnums.add(MineableBlock.UMBER); break;
            default:
                targetEnums.addAll(List.of(
                        MineableBlock.GREEN_MITHRIL, MineableBlock.BLUE_MITHRIL, MineableBlock.GRAY_MITHRIL,
                        MineableBlock.GRAY_TERRACOTTA_MITHRIL, MineableBlock.TITANIUM
                ));
                break;
        }

        for (MineableBlock mb : targetEnums) {
            int weight = (mb == MineableBlock.TITANIUM) ? 15 : 10;
            for (Block b : mb.getBlocks()) {
                if (b != null) {
                    blockPriority.put(b, weight);
                }
            }
        }
    }

    public void startWithConfiguredPriorities(final int miningSpeed, final PickaxeAbility pickaxeAbility, String miningTool) {
        if (!miningTool.isEmpty() && !InventoryUtil.holdItem(miningTool)) {
            logError(miningTool + " not found in inventory!");
            error = BlockMinerError.NO_TOOLS_AVAILABLE;
            this.stop();
            return;
        }

        if (blockPriority.isEmpty()) {
            setupPrioritiesForOreType(Vertex.config().miningMacro.oreType);
        }

        if (mc.player != null) {
            this.startPos = mc.player.blockPosition();
        }

        this.miningSpeed = miningSpeed - 200;
        this.pickaxeAbility = pickaxeAbility;
        this.error = BlockMinerError.NONE;
        this.retryActivatePickaxeAbility = 0;
        targetParticlePos = null;

        this.currentState = new StartingState();
    }

    @Override
    public void onDisable() {
        stop();
    }

    /**
     * Starts the BlockMiner with specified parameters. Will continue to mine {@code blocksToMine} until stop() is called
     *
     * @param blocksToMine   Array of mine-able block types to target
     * @param miningSpeed    Base mining speed (higher = faster)
     * @param pickaxeAbility Users selected pickaxe ability
     * @param priority       Array of priority values for block selection
     * @param miningTool     Item name of the tool to use for mining
     */
    public void start(MineableBlock[] blocksToMine, final int miningSpeed, final PickaxeAbility pickaxeAbility, final int[] priority, String miningTool) {
        if (!miningTool.isEmpty() && !InventoryUtil.holdItem(miningTool)) {
            logError(miningTool + " not found in inventory!");
            error = BlockMinerError.NO_TOOLS_AVAILABLE;
            this.stop();
            return;
        }

        if (blocksToMine == null || Arrays.stream(priority).allMatch(i -> i == 0)) {
            logError("Target blocks not set!");
            error = BlockMinerError.NO_TARGET_BLOCKS;
            return;
        }

        blockPriority.clear();
        for (int i = 0; i < blocksToMine.length; i++) {
            MineableBlock mb = blocksToMine[i];
            int p = priority[i];
            if (p <= 0) continue;

            // Expand Mithril categories if any Mithril block type is passed
            if (mb == MineableBlock.GREEN_MITHRIL || mb == MineableBlock.BLUE_MITHRIL || mb == MineableBlock.GRAY_MITHRIL || mb == MineableBlock.GRAY_TERRACOTTA_MITHRIL) {
                for (MineableBlock mithrilMB : List.of(MineableBlock.GREEN_MITHRIL, MineableBlock.BLUE_MITHRIL, MineableBlock.GRAY_MITHRIL, MineableBlock.GRAY_TERRACOTTA_MITHRIL, MineableBlock.TITANIUM)) {
                    int weight = (mithrilMB == MineableBlock.TITANIUM) ? p + 5 : p;
                    for (Block block : mithrilMB.getBlocks()) {
                        if (block != null) {
                            blockPriority.put(block, weight);
                        }
                    }
                }
            } else {
                for (Block block : mb.getBlocks()) {
                    if (block != null) {
                        blockPriority.put(block, p);
                    }
                }
            }
        }

        if (mc.player != null) {
            this.startPos = mc.player.blockPosition();
        }

        this.miningSpeed = miningSpeed - 200;
        this.pickaxeAbility = pickaxeAbility;
        this.error = BlockMinerError.NONE;
        this.retryActivatePickaxeAbility = 0;
        targetParticlePos = null;

        this.currentState = new StartingState();
    }

    public void stop() {
        if (currentState != null)
            currentState.onEnd(this);
        KeyBindUtil.releaseAllExcept();
        blockPriority.clear();
    }

    public void onTick() {
        if (mc.screen != null) {
            return;
        }

        if (currentState == null)
            return;

        BlockMinerState nextState = currentState.onTick(this);
        transitionTo(nextState);

        if (retryActivatePickaxeAbility >= 4) {
            sendError("Cannot find messages for pickaxe ability! Disabling pickaxe ability for this session.");
            sendError("Either enable any pickaxe ability in HOTM or enable chat messages.");
            this.pickaxeAbility = PickaxeAbility.NONE;
            this.retryActivatePickaxeAbility = 0;
        }
    }

    private void transitionTo(BlockMinerState nextState) {
        if (currentState == nextState)
            return;

        if ((currentState instanceof StartingState && nextState instanceof ApplyAbilityState)
                || (currentState instanceof ApplyAbilityState && nextState instanceof StartingState)) {
            retryActivatePickaxeAbility++;
        } else {
            retryActivatePickaxeAbility = 0;
        }

        currentState.onEnd(this);
        currentState = nextState;

        if (currentState == null) {
            log("null state, returning");
            return;
        }

        currentState.onStart(this);
    }

    public void onChat(String message) {
        message = message.toLowerCase();

        long now = System.currentTimeMillis();

        if (message.contains("is now available!")) {
            pickaxeAbilityState = PickaxeAbilityState.AVAILABLE;
            pickaxeAbilityCooldownEndMs = 0L;
        }

        if (message.contains("you used your")) {
            pickaxeAbilityState = PickaxeAbilityState.UNAVAILABLE;
            lastAbilityUse = now;
            pickaxeAbilityCooldownEndMs = Math.max(pickaxeAbilityCooldownEndMs, now + DEFAULT_PICKAXE_ABILITY_COOLDOWN_MS);
            return;
        }

        if (message.contains("your pickaxe ability is on cooldown for")) {
            pickaxeAbilityState = PickaxeAbilityState.UNAVAILABLE;
            long cooldownMs = parseCooldownMs(message);
            if (cooldownMs > 0L) {
                pickaxeAbilityCooldownEndMs = Math.max(pickaxeAbilityCooldownEndMs, now + cooldownMs);
            }
        }
    }

    public void onParticleSpawn(SpawnParticleEvent event) {
        if (!Vertex.config().general.precisionMiner
                || event.getParticleType() != ParticleTypes.CRIT
                || targetBlockPos == null
                || mc.player.position().distanceToSqr(event.getPos()) >= 64) {

            targetParticlePos = null;
            return;
        }

        Vec3 particlePos = event.getPos();
        double expansion = 0.2;
        AABB expandedAABB = new AABB(
                targetBlockPos.getX() - expansion, targetBlockPos.getY() - expansion, targetBlockPos.getZ() - expansion,
                targetBlockPos.getX() + 1 + expansion, targetBlockPos.getY() + 1 + expansion, targetBlockPos.getZ() + 1 + expansion
        );

        if (!expandedAABB.contains(particlePos)) return;

        targetParticlePos = particlePos;
    }

    public void onBlockChange(BlockChangeEvent event) {
        if (targetBlockPos != null && event.pos().equals(targetBlockPos)) {
            log("Block change detected at target " + targetBlockPos + ": " + event.oldState().getBlock() + " -> " + event.newState().getBlock());
            blockChanged = true;
        }
    }

    public void onWorldRender(WorldRenderContextWrapper context) {
        if (this.targetParticlePos != null) {
            RenderUtil.drawPoint(this.targetParticlePos, new Color(255, 0, 0, 100));
        }
    }

    public enum PickaxeAbilityState {
        AVAILABLE, UNAVAILABLE,
    }

    public enum BlockMinerError {
        NONE,
        NOT_ENOUGH_BLOCKS,
        NO_TOOLS_AVAILABLE,
        NO_POINTS_FOUND,
        NO_TARGET_BLOCKS,
        NO_PICKAXE_ABILITY,
    }

    public enum PickaxeAbility {
        NONE,
        PICKOBULUS,
        MINING_SPEED_BOOST
    }
}
