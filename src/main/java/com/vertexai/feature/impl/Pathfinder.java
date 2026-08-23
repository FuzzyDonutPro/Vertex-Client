package com.vertexai.feature.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import kotlin.Pair;
import com.vertexai.Vertex;
import com.vertexai.feature.AbstractFeature;
import com.vertexai.feature.impl.PathExecutor.State;
import com.vertexai.handler.RotationHandler;
import com.vertexai.pathfinder.calculate.Path;
import com.vertexai.pathfinder.calculate.PathNode;
import com.vertexai.pathfinder.calculate.PathfindingTelemetry;
import com.vertexai.pathfinder.calculate.path.AStarPathFinder;
import com.vertexai.pathfinder.goal.Goal;
import com.vertexai.pathfinder.movement.CalculationContext;
import com.vertexai.util.PlayerUtil;
import com.vertexai.util.RenderUtil;
import com.vertexai.util.WorldRenderContextWrapper;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Pathfinder extends AbstractFeature {

    private static final Minecraft mc = Minecraft.getInstance();
    private static Pathfinder instance;
    private final Deque<Pair<BlockPos, BlockPos>> pathQueue = new ConcurrentLinkedDeque<>();
    private final Deque<Path> renderOnlyPathQueue = new ConcurrentLinkedDeque<>();
    private final PathExecutor pathExecutor = PathExecutor.getInstance();
    private AStarPathFinder finder;
    private volatile boolean skipTick = false;
    private volatile boolean pathfinding = false;
    private boolean failed = false;
    private boolean succeeded = false;
    private boolean renderOnlyMode = false;
    private volatile PathfindingTelemetry lastTelemetry = null;

    public static Pathfinder getInstance() {
        if (instance == null) {
            instance = new Pathfinder();
        }
        return instance;
    }

    public void onClientTick() {
        this.onTick();
    }

    public void onRender(PoseStack matrices, Camera camera, Matrix4f projectionMatrix) {
    }


    @Override
    public String getName() {
        return "Pathfinder";
    }

    @Override
    public void start() {
        if (this.pathQueue.isEmpty()) {
            sendError("Pathqueue is empty. Cannot start");
            return;
        }

        this.enabled = true;
        this.succeeded = false;
        this.failed = false;
        this.renderOnlyMode = false;
        this.renderOnlyPathQueue.clear();
        pathExecutor.start();
        send("Started");
    }

    public void startRenderOnly() {
        if (this.pathQueue.isEmpty()) {
            sendError("Pathqueue is empty. Cannot start render-only mode");
            return;
        }

        this.enabled = true;
        this.succeeded = false;
        this.failed = false;
        this.renderOnlyMode = true;
        this.renderOnlyPathQueue.clear();
        pathExecutor.stop("Render-only preview mode");
        send("Started render-only path preview");
    }

    @Override
    public boolean isRunning() {
        return this.enabled && !this.renderOnlyMode && (this.pathfinding || !this.pathQueue.isEmpty() || pathExecutor.isRunning());
    }

    public boolean isRenderOnlyMode() {
        return this.enabled && this.renderOnlyMode;
    }

    public PathfindingTelemetry getLastTelemetry() {
        return this.lastTelemetry;
    }

    @Override
    public void stop() {
        this.stop("No explicit reason");
    }

    public void stop(String reason) {
        String stopReason = (reason == null || reason.trim().isEmpty()) ? "No explicit reason" : reason.trim();
        if (!mc.isSameThread()) {
            mc.execute(() -> this.stop(stopReason));
            return;
        }
        this.enabled = false;
        this.pathfinding = false;
        this.skipTick = false;
        this.renderOnlyMode = false;
        this.renderOnlyPathQueue.clear();
        this.pathQueue.clear();
        this.resetStatesAfterStop();

        send("stopped (" + stopReason + ")");
        log("stopped. reason: " + stopReason + ", executor: " + pathExecutor.getStopReason());
    }

    private void stopOnClientThread(String reason) {
        String stopReason = (reason == null || reason.trim().isEmpty()) ? "No explicit reason" : reason.trim();
        if (mc.isSameThread()) {
            this.stop(stopReason);
            return;
        }
        mc.execute(() -> this.stop(stopReason));
    }

    @Override
    public void resetStatesAfterStop() {
        if (finder != null) {
            finder.requestStop();
        }
        pathExecutor.stop();
        RotationHandler.getInstance().stop();
    }

    public void queue(BlockPos start, BlockPos end) {
        if (!this.pathQueue.isEmpty() && !this.pathQueue.peekLast().getSecond().equals(start)) {
            sendError("This does not start at the end of the previous path. Ignoring!");
            return;
        }

        this.pathQueue.offer(new Pair<>(start, end));
        log("Queued Path");
    }

    private int searchVersion = 0;

    public void stopAndRequeue(BlockPos pos) {
        this.pathQueue.clear();
        this.pathExecutor.clearQueuedPaths();
        this.searchVersion++; // Increment version to discard stale threads

        if (this.finder != null) {
            this.finder.requestStop();
        }

        if (!this.enabled) {
            this.queue(PlayerUtil.getBlockStandingOn(), pos);
            return;
        }

        if (this.pathExecutor.getCurrentPath() != null) {
            this.queue(this.pathExecutor.getCurrentPath().getEnd(), pos);
        } else {
            this.queue(PlayerUtil.getBlockStandingOn(), pos);
        }
    }

    public void queue(BlockPos end) {
        BlockPos start;
        if (this.pathQueue.isEmpty()) {
            if (this.pathExecutor.getCurrentPath() == null) {
                start = PlayerUtil.getBlockStandingOn();
            } else {
                start = this.pathExecutor.getCurrentPath().getEnd();
            }
        } else {
            start = this.pathQueue.peekLast().getSecond();
        }
        this.pathQueue.offer(new Pair<>(start, end));
    }

    public void setSprintState(boolean sprint) {
        pathExecutor.setAllowSprint(sprint);
    }

    public void setInterpolationState(boolean interpolate) {
        pathExecutor.setAllowInterpolation(interpolate);
    }

    @Override
    protected void onTick() {
        if (mc.player == null || mc.level == null) {
            log("Player or World is null, stopping Pathfinder.");
            if (this.enabled) {
                this.stop("Player or world is null");
            }
            return;
        }

        boolean executorRunning = this.renderOnlyMode || pathExecutor.onTick();

        if (!this.renderOnlyMode) {
            if (pathExecutor.failed()) {
                String executorReason = pathExecutor.getStopReason();
                log("pathexecutor failed. reason: " + executorReason);
                this.failed = true;
                this.stop("PathExecutor failed: " + executorReason);
                return;
            }

            if (pathExecutor.succeeded() || !pathExecutor.isRunning()) {
                if (this.pathQueue.isEmpty() && !this.pathfinding) {
                    this.succeeded = true;
                    this.stop("Completed path queue");
                    log("pathqueue empty stopping");
                    return;
                }
            }
        }

        // just to let pathexecutor update after path has been found
        if (this.skipTick) {
            this.skipTick = false;
            return;
        }

        if (!executorRunning) {
            return;
        }

        if (this.pathQueue.isEmpty()) {
            return;
        }

        if (this.pathfinding) {
            return;
        }

        Vertex.executor().execute(() -> {
            log("creating task. wasPathfinding: " + this.pathfinding);
            if (this.pathfinding) {
                return;
            }
            this.pathfinding = true;
            
            Pair<BlockPos, BlockPos> startEnd = this.pathQueue.poll();
            if (startEnd == null) {
                this.pathfinding = false;
                return;
            }
            
            final int currentSearchVersion = this.searchVersion;
            
            try {
                long startedAtMs = System.currentTimeMillis();
                BlockPos start = startEnd.getFirst();
                BlockPos end = startEnd.getSecond();
                double walkSpeed = mc.player.getSpeed();
                CalculationContext ctx = new CalculationContext(walkSpeed * 1.3, walkSpeed, walkSpeed * 0.3);
                Goal goal = new Goal(end.getX(), end.getY(), end.getZ(), ctx);
                Path path;
                double searchMs = 0.0;
                boolean sameHeightSegment = start.getY() == end.getY();
                boolean directWalk = false;
                
                if (currentSearchVersion != this.searchVersion) {
                    log("Aborting stale pathfinding task early");
                    this.pathfinding = false;
                    this.skipTick = true;
                    return;
                }
                
                if (sameHeightSegment && com.vertexai.pathfinder.util.BlockUtil.INSTANCE.canWalkBetween(ctx, start, end)) {
                    directWalk = true;
                    finder = null;
                    PathNode startNode = new PathNode(start.getX(), start.getY(), start.getZ(), goal);
                    PathNode endNode = new PathNode(end.getX(), end.getY(), end.getZ(), goal);
                    endNode.setParentNode(startNode);
                    path = new Path(startNode, endNode, goal, ctx);
                    log("Skipping A*: direct walkable segment from " + start + " to " + end);
                } else {
                    long searchStartNs = System.nanoTime();
                    // Bound to 50,000 nodes to prevent GC churn, CPU lockups, and memory leaks
                    List<BlockPos> rawPath = com.vertexai.pathing.PathFinder.findPath(mc.level, start, end, 50000);
                    
                    if (currentSearchVersion != this.searchVersion) {
                        log("Aborting stale pathfinding task after raw search");
                        this.pathfinding = false;
                        this.skipTick = true;
                        return;
                    }
                    
                    if (rawPath != null && !rawPath.isEmpty()) {
                        PathNode previous = null;
                        PathNode startNode = null;
                        for (BlockPos pos : rawPath) {
                            PathNode node = new PathNode(pos.getX(), pos.getY(), pos.getZ(), goal);
                            if (previous != null) {
                                node.setParentNode(previous);
                            } else {
                                startNode = node;
                            }
                            previous = node;
                        }
                        path = new Path(startNode, previous, goal, ctx);
                        path.getSmoothedPath(); // Pre-calculate smoothing
                    } else {
                        path = null;
                    }
                    
                    searchMs = (System.nanoTime() - searchStartNs) / 1_000_000.0;
                    log("done pathfinding using 8-Way Engine (" + String.format("%.2f", searchMs) + "ms)");
                }
                
                if (currentSearchVersion != this.searchVersion) {
                    log("Aborting stale pathfinding task before queueing");
                    this.pathfinding = false;
                    this.skipTick = true;
                    return;
                }
                
                if (path != null) {
                    List<BlockPos> smoothedPath = path.getSmoothedPath();
                    this.lastTelemetry = new PathfindingTelemetry(
                            startedAtMs,
                            System.currentTimeMillis(),
                            true,
                            "",
                            searchMs,
                            path.getSmoothingDurationMs(),
                            0,
                            0,
                            0,
                            path.getPath().size(),
                            smoothedPath.size(),
                            directWalk
                    );
                    if (this.renderOnlyMode) {
                        this.renderOnlyPathQueue.offer(path);
                        this.succeeded = true;
                        send("Render-only preview ready");
                    } else {
                        PathExecutor.getInstance().queuePath(path);
                    }
                } else {
                    log("No Path Found");
                    failed = true;
                    this.lastTelemetry = new PathfindingTelemetry(
                            startedAtMs,
                            System.currentTimeMillis(),
                            false,
                            "no_path",
                            searchMs,
                            0.0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            directWalk
                    );
                    stopOnClientThread("No path found from " + start + " to " + end);
                }
            } catch (Exception e) {
                Vertex.LOGGER.error("Pathfinding task crashed", e);
                failed = true;
                this.lastTelemetry = new PathfindingTelemetry(
                        System.currentTimeMillis(),
                        System.currentTimeMillis(),
                        false,
                        "exception_" + e.getClass().getSimpleName(),
                        0.0,
                        0.0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        false
                );
                stopOnClientThread("Pathfinding task crashed: " + e.getClass().getSimpleName());
            } finally {
                this.pathfinding = false;
                this.skipTick = true;
            }
        });
    }
    @Override
    protected void onWorldRender(WorldRenderContextWrapper context) {
        renderInWorld(context);
    }

    public void renderInWorld(WorldRenderContextWrapper context) {
        // this.pathExecutor.onRender();
        List<Path> paths = new java.util.ArrayList<>(this.pathExecutor.getPathQueue());
        if (pathExecutor.getCurrentPath() != null) {
            paths.add(pathExecutor.getCurrentPath());
        }
        paths.addAll(this.renderOnlyPathQueue);

        if (!paths.isEmpty()) {
            int themeHex = Vertex.config().gui.getThemeColorInt();
            int r = (themeHex >> 16) & 0xFF;
            int g = (themeHex >> 8) & 0xFF;
            int b = themeHex & 0xFF;

            Color blockFillColor = new Color(r, g, b, 55);
            Color blockOutlineColor = new Color(r, g, b, 210);
            Color destFillColor = new Color(r, g, b, 120);
            Color destOutlineColor = new Color(255, 255, 255, 255);

            java.util.Set<BlockPos> renderedBlocks = new java.util.HashSet<>();
            BlockPos finalDestination = null;

            for (int pIdx = 0; pIdx < paths.size(); pIdx++) {
                Path path = paths.get(pIdx);
                List<BlockPos> fullPath = path.getPath();
                if (fullPath.isEmpty()) continue;

                boolean isLastPath = (pIdx == paths.size() - 1);

                for (int i = 0; i < fullPath.size(); i++) {
                    BlockPos p = fullPath.get(i);
                    // Determine the actual block the node is on
                    BlockPos standBlock = p.below();
                    if (mc.level != null && mc.level.getBlockState(standBlock).isAir() && !mc.level.getBlockState(p).isAir()) {
                        standBlock = p;
                    }

                    if (isLastPath && i == fullPath.size() - 1) {
                        finalDestination = standBlock;
                    } else if (renderedBlocks.add(standBlock)) {
                        RenderUtil.drawBlock(standBlock, blockFillColor);
                        RenderUtil.outlineBlock(standBlock, blockOutlineColor);
                    }
                }
            }

            // Highlight final destination block with special accent
            if (finalDestination != null) {
                RenderUtil.drawBlock(finalDestination, destFillColor);
                RenderUtil.outlineBlock(finalDestination, destOutlineColor);
            }
        }
    }

    public boolean completedPathTo(BlockPos pos) {
        Path prev = pathExecutor.getPreviousPath();
        return prev != null && prev.getGoal().isAtGoal(pos.getX(), pos.getY(), pos.getZ());
    }

    public boolean failed() {
        return !this.enabled && this.failed;
    }

    public boolean succeeded() {
        return !this.enabled && this.succeeded;
    }
}
