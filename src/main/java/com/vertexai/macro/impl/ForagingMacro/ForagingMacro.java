package com.vertexai.macro.impl.ForagingMacro;

import com.vertexai.Vertex;
import com.vertexai.macro.AbstractMacro;
import com.vertexai.macro.impl.ForagingMacro.states.StartingState;
import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.helper.Clock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ForagingMacro extends AbstractMacro {

    public static ForagingMacro instance = new ForagingMacro();
    public static ForagingMacro getInstance() { return instance; }

    @Getter
    @Setter
    private BlockPos targetBlockPos;

    @Getter
    @Setter
    private String currentForagingMode = "OAK";

    private ForagingMacroState currentState;

    private final Map<BlockPos, Long> blacklistedBlocks = new ConcurrentHashMap<>();
    public final Clock blacklistClearClock = new Clock();
    private final Clock jumpReleaseClock = new Clock();
    public long lastLogBreakTime = 0;

    private Vec3 lastPlayerPos = null;
    private long lastMoveCheckTime = 0;
    private int stuckCount = 0;

    @Override
    public List<String> getNecessaryItems() {
        return List.of("Treecapitator", "Jungle Axe", "Axe");
    }

    public boolean isBlockBlacklisted(BlockPos pos) {
        if (pos == null) return false;
        Long expiry = blacklistedBlocks.get(pos);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            blacklistedBlocks.remove(pos);
            return false;
        }
        return true;
    }

    public void blacklistBlock(BlockPos pos, long durationMs) {
        if (pos != null) {
            blacklistedBlocks.put(pos, System.currentTimeMillis() + durationMs);
        }
    }

    public void clearBlacklist() {
        blacklistedBlocks.clear();
    }

    public boolean isBlacklistEmpty() {
        return blacklistedBlocks.isEmpty();
    }

    /**
     * Blacklists an entire tree cluster when any log of the tree is broken,
     * ensuring Treecapitator / Jungle Axe only ever hits one log per tree.
     */
    public void blacklistTreeCluster(BlockPos rootPos) {
        if (rootPos == null || Minecraft.getInstance().level == null) return;
        var level = Minecraft.getInstance().level;
        long durationMs = 12000L; // 12 second respawn cycle blacklist

        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(rootPos);
        visited.add(rootPos);
        blacklistBlock(rootPos, durationMs);

        // BFS flood-fill all connected log blocks in this tree up to MAX_TREE_LOGS (50)
        while (!queue.isEmpty() && visited.size() <= MAX_TREE_LOGS) {
            BlockPos current = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 2; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos neighbor = current.offset(dx, dy, dz);
                        if (!visited.contains(neighbor) && visited.size() <= MAX_TREE_LOGS && neighbor.distSqr(rootPos) <= 144) {
                            visited.add(neighbor);
                            Block b = level.getBlockState(neighbor).getBlock();
                            if (isLogBlock(b, this.currentForagingMode)) {
                                blacklistBlock(neighbor, durationMs);
                                queue.add(neighbor);
                            }
                        }
                    }
                }
            }
        }

        // Also blacklist tree cylinder column to prevent hitting split trunks
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (x * x + z * z <= 12) {
                    for (int y = -2; y <= 16; y++) {
                        BlockPos p = rootPos.offset(x, y, z);
                        blacklistBlock(p, durationMs);
                    }
                }
            }
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        log("Starting Foraging Macro...");
        this.targetBlockPos = null;
        this.clearBlacklist();
        this.blacklistClearClock.schedule(12000L);
        this.lastPlayerPos = null;
        this.stuckCount = 0;
        this.currentState = new StartingState();
        if (this.currentState != null) {
            this.currentState.onStart(this);
        }
    }

    public static boolean isLogBlock(Block block, String mode) {
        if (block == null) return false;
        String id = block.getDescriptionId().toLowerCase(java.util.Locale.ROOT);
        
        // Exclude stripped logs and non-log wooden structures
        if (id.contains("stripped") || id.contains("planks") || id.contains("fence") || 
            id.contains("stairs") || id.contains("slab") || id.contains("door") || 
            id.contains("sign") || id.contains("plate") || id.contains("button") || 
            id.contains("gate") || id.contains("table") || id.contains("chest") || 
            id.contains("barrel") || id.contains("composter") || id.contains("boat")) {
            return false;
        }

        // Must explicitly be a log or wood block
        boolean isLogOrWood = id.contains("log") || id.contains("wood");
        if (!isLogOrWood) return false;

        String m = mode != null ? mode.toLowerCase(java.util.Locale.ROOT) : "";

        if (m.contains("dark")) {
            return id.contains("dark_oak");
        } else if (m.contains("acacia")) {
            return id.contains("acacia");
        } else if (m.contains("jungle") || m.contains("mangrove")) {
            return id.contains("jungle") || id.contains("mangrove");
        } else if (m.contains("spruce")) {
            return id.contains("spruce");
        } else if (m.contains("oak")) {
            return id.contains("oak") && !id.contains("dark");
        } else if (m.contains("birch")) {
            return id.contains("birch");
        }
        return true;
    }

    public static boolean isLeafBlock(Block block) {
        if (block == null) return false;
        String id = block.getDescriptionId().toLowerCase(java.util.Locale.ROOT);
        return id.contains("leaves") || id.contains("leaf");
    }

    public static final int MIN_TREE_LOGS = 5;
    public static final int MAX_TREE_LOGS = 50;

    /**
     * Counts connected logs spreading through the tree trunk and branches.
     * Searches up to maxSearch logs for instant execution.
     */
    public static int countConnectedLogs(net.minecraft.world.level.Level level, BlockPos startPos, String mode, int maxSearch) {
        if (level == null || startPos == null) return 0;
        
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        
        queue.add(startPos);
        visited.add(startPos);
        int count = 0;

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            Block block = level.getBlockState(current).getBlock();
            if (isLogBlock(block, mode)) {
                count++;
                if (count >= maxSearch) {
                    return count;
                }
                
                // Explore 3D spreading neighbors (including diagonals and natural branch forks)
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 2; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) continue;
                            BlockPos next = current.offset(dx, dy, dz);
                            if (!visited.contains(next) && next.distSqr(startPos) <= 144) {
                                visited.add(next);
                                Block nextBlock = level.getBlockState(next).getBlock();
                                if (isLogBlock(nextBlock, mode)) {
                                    queue.add(next);
                                }
                            }
                        }
                    }
                }
            }
        }
        return count;
    }

    public static boolean isFullTree(net.minecraft.world.level.Level level, BlockPos pos, String mode) {
        int count = countConnectedLogs(level, pos, mode, MAX_TREE_LOGS + 1);
        return count >= MIN_TREE_LOGS && count <= MAX_TREE_LOGS;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        log("Stopping Foraging Macro...");
        if (this.currentState != null) {
            this.currentState.onEnd(this);
        }
        this.currentState = null;
        this.targetBlockPos = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            KeyBindUtil.setKeyBindState(mc.options.keyAttack, false);
            KeyBindUtil.setKeyBindState(mc.options.keyJump, false);
            KeyBindUtil.setKeyBindState(mc.options.keyUp, false);
        }
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Auto-purge expired blacklist entries to prevent memory accumulation
        long now = System.currentTimeMillis();
        blacklistedBlocks.entrySet().removeIf(entry -> now > entry.getValue());

        // Handle jump release pulse
        if (jumpReleaseClock.isScheduled() && jumpReleaseClock.passed()) {
            if (mc.options != null) {
                KeyBindUtil.setKeyBindState(mc.options.keyJump, false);
            }
            jumpReleaseClock.reset();
        }

        checkStuck();
        
        if (this.currentState != null) {
            ForagingMacroState nextState = this.currentState.onTick(this);
            if (nextState != null && nextState != this.currentState) {
                this.currentState.onEnd(this);
                this.currentState = nextState;
                this.currentState.onStart(this);
            }
        }
    }

    private void checkStuck() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        long now = System.currentTimeMillis();
        if (now - lastMoveCheckTime > 1200) {
            Vec3 currentPos = mc.player.position();
            if (lastPlayerPos != null && com.vertexai.feature.impl.Pathfinder.getInstance().isRunning()) {
                double dist = currentPos.distanceTo(lastPlayerPos);
                if (dist < 0.25) {
                    stuckCount++;
                    if (stuckCount == 1) {
                        // Pulse jump to hop over 1-block obstacle
                        KeyBindUtil.setKeyBindState(mc.options.keyJump, true);
                        jumpReleaseClock.schedule(150L);
                    } else if (stuckCount >= 3) {
                        log("Player stuck while pathfinding, retargeting new tree...");
                        if (this.targetBlockPos != null) {
                            blacklistTreeCluster(this.targetBlockPos);
                            this.targetBlockPos = null;
                        }
                        com.vertexai.feature.impl.Pathfinder.getInstance().stop();
                        stuckCount = 0;
                    }
                } else {
                    stuckCount = 0;
                    if (mc.options != null && !mc.options.keyJump.isDown()) {
                        KeyBindUtil.setKeyBindState(mc.options.keyJump, false);
                    }
                }
            }
            lastPlayerPos = currentPos;
            lastMoveCheckTime = now;
        }
    }

    @Override
    public String getName() {
        return "Foraging";
    }

    public void setForagingMode(String mode) {
        if (mode != null && !mode.isEmpty()) {
            this.currentForagingMode = mode.toUpperCase(java.util.Locale.ROOT);
            log("Set foraging mode to: " + this.currentForagingMode);
            this.clearBlacklist();
            if (this.currentState != null) {
                this.currentState = new StartingState();
                this.currentState.onStart(this);
            }
        }
    }
}
