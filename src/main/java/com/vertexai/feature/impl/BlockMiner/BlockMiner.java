package com.vertexai.feature.impl.BlockMiner;

import lombok.Getter;
import lombok.Setter;
import com.vertexai.Vertex;
import com.vertexai.event.BlockChangeEvent;
import com.vertexai.event.SpawnParticleEvent;
import com.vertexai.feature.AbstractFeature;
import com.vertexai.feature.impl.BlockMiner.states.ApplyAbilityState;
import com.vertexai.feature.impl.BlockMiner.states.BlockMinerState;
import com.vertexai.feature.impl.BlockMiner.states.StartingState;
import com.vertexai.util.InventoryUtil;
import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.RenderUtil;
import com.vertexai.util.helper.MineableBlock;
import com.vertexai.util.WorldRenderContextWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
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
public class BlockMiner extends AbstractFeature {

    public static final long DEFAULT_PICKAXE_ABILITY_COOLDOWN_MS = 60000L;
    private static final Pattern PICKAXE_COOLDOWN_PATTERN = Pattern.compile("cooldown for\\s+(\\d+)\\s*([sm])");
    private static BlockMiner instance;
    /**
     * The map of the state ID of the block -> its priority
     */
    private final Map<Block, Integer> blockPriority = new HashMap<>();
    private BlockMinerState currentState;
    private long lastAbilityUse = System.currentTimeMillis();
    private BlockMinerError error = BlockMinerError.NONE;
    private int retryActivatePickaxeAbility;
    private BlockPos targetBlockPos;
    private Block targetBlockType;
    private Vec3 targetParticlePos;
    private int miningSpeed;
    private PickaxeAbility pickaxeAbility;
    private int waitThreshold;
    private PickaxeAbilityState pickaxeAbilityState = PickaxeAbilityState.AVAILABLE;
    private long pickaxeAbilityCooldownEndMs;
    private boolean blockChanged;
    private BlockPos startPos;

    public BlockPos getStartPos() { return startPos; }
    public void setStartPos(BlockPos startPos) { this.startPos = startPos; }
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

    public static BlockMiner getInstance() {
        if (instance == null) {
            instance = new BlockMiner();
        }
        return instance;
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
        // Try to hold the specified mining tool if provided
        if (!miningTool.isEmpty() && !InventoryUtil.holdItem(miningTool)) {
            logError(miningTool + " not found in inventory!");
            error = BlockMinerError.NO_TOOLS_AVAILABLE;
            this.stop();
            return;
        }

        // Validate blocks to mine
        if (blocksToMine == null || Arrays.stream(priority).allMatch(i -> i == 0)) {
            logError("Target blocks not set!");
            error = BlockMinerError.NO_TARGET_BLOCKS;
            return;
        }

        // Build priority mapping for block selection
        for (int i = 0; i < blocksToMine.length; i++) {
            for (Block block : blocksToMine[i].getBlocks()) {
                if (block != null) {
                    blockPriority.put(block, priority[i]);
                }
            }
        }

        // Initialize parameters
        this.startPos = com.vertexai.util.PlayerUtil.getBlockStandingOn();
        this.miningSpeed = miningSpeed - 200;  // Base adjustment to mining speed
        this.pickaxeAbility = pickaxeAbility;
        this.enabled = true;
        this.error = BlockMinerError.NONE;
        this.retryActivatePickaxeAbility = 0;
        targetParticlePos = null;

        // Initialize with starting state
        this.currentState = new StartingState();
        this.start();
    }

    @Override
    public void stop() {
        if (currentState != null)
            currentState.onEnd(this);
        super.stop();
        KeyBindUtil.releaseAllExcept();
        blockPriority.clear();

    }

    @Override
    protected void onTick() {
        if (!this.enabled || mc.screen != null) {
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
        // Skip if no state change
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

    @Override
    protected void onChat(String message) {
        message = message.toLowerCase();

        long now = System.currentTimeMillis();

        if (message.contains("is now available!")) {
            pickaxeAbilityState = PickaxeAbilityState.AVAILABLE;
            pickaxeAbilityCooldownEndMs = 0L;
        }

        // Treat any "used"/"cooldown" message as authoritative UNAVAILABLE.
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

    @Override
    protected void onParticleSpawn(SpawnParticleEvent event) {
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

    @Override
    protected void onBlockChange(BlockChangeEvent event) {
        if (targetBlockPos != null && event.pos().equals(targetBlockPos)) {
            log("Block change detected at target " + targetBlockPos + ": " + event.oldState().getBlock() + " -> " + event.newState().getBlock());
            blockChanged = true;
        }
    }

    @Override
    protected void onWorldRender(WorldRenderContextWrapper context) {
        if (this.targetParticlePos != null) {
            RenderUtil.drawPoint(this.targetParticlePos, new Color(255, 0, 0, 100));
        }
    }

    /**
     * Possible states for pickaxe ability.
     * AVAILABLE: Pickaxe ability can be activated
     * UNAVAILABLE: Pickaxe ability is on cooldown or is currently in effect
     */
    public enum PickaxeAbilityState {
        AVAILABLE, UNAVAILABLE,
    }

    public enum BlockMinerError {
        NONE,              // No error
        NOT_ENOUGH_BLOCKS, // Cannot find blocks to mine
        NO_TOOLS_AVAILABLE, // Required mining tool not found in inventory
        NO_POINTS_FOUND,    // Cannot find valid points to target on block
        NO_TARGET_BLOCKS,   // The user did not set any blocks for the miner to mine
        NO_PICKAXE_ABILITY,    // The user cannot use the pickaxe ability
    }


    /**
     * The type of pickaxe ability to be used. At the moment, {@code MINING_SPEED_BOOST} represents all pickaxe abilities
     * other than pickobulus
     */
    public enum PickaxeAbility {
        NONE,
        PICKOBULUS,
        MINING_SPEED_BOOST
    }
}
