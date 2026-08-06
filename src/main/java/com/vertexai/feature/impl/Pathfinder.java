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
        Deque<Path> paths = new LinkedList<>(pathExecutor.getPathQueue());
        if (pathExecutor.getCurrentPath() != null) paths.add(pathExecutor.getCurrentPath());
        paths.addAll(renderOnlyPathQueue);

        boolean seeThrough = Vertex.config() != null && Vertex.config().utilities != null && Vertex.config().utilities.renderPathfinderThroughWalls;

        if (!paths.isEmpty()) {
            RenderUtil.drawBlock(paths.getFirst().getStart(), new Color(0, 255, 0, 150), seeThrough);
            for (Path path : paths) {
                List<BlockPos> bpath = path.getSmoothedPath();
                for (int i = 0; i < bpath.size(); i++) {
                    BlockPos p = bpath.get(i);
                    if (i != 0) {
                        RenderUtil.drawBlock(p, new Color(0, 255, 0, 150), seeThrough);
                        RenderUtil.drawThinLine(
                                new Vec3(bpath.get(i).getX() + 0.5, bpath.get(i).getY() + 1.0, bpath.get(i).getZ() + 0.5),
                                new Vec3(bpath.get(i - 1).getX() + 0.5, bpath.get(i - 1).getY() + 1.0, bpath.get(i - 1).getZ() + 0.5),
                                new Color(0, 255, 0, 150),
                                seeThrough
                        );
                    }
                }
            }
        }
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
        return this.enabled && !this.renderOnlyMode;
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

    public void stopAndRequeue(BlockPos pos) {
        this.stop("Requeuing new target path");
        this.pathQueue.clear();
        this.pathExecutor.stop("Requeuing new target path");
        this.pathExecutor.clearQueuedPaths();
        this.queue(PlayerUtil.getBlockStandingOn(), pos);
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

        boolean okToPath = this.renderOnlyMode || pathExecutor.onTick();

        // just to let pathexecutor update after path has been found
        if (this.skipTick) {
            this.skipTick = false;
            return;
        }

        if (!this.renderOnlyMode && pathExecutor.failed()) {
            String executorReason = pathExecutor.getStopReason();
            log("pathexecutor failed. reason: " + executorReason);
            this.failed = true;
            this.stop("PathExecutor failed: " + executorReason);
            return;
        }

        if (!okToPath) {
            return;
        }

        if (this.pathQueue.isEmpty()) {
            if (!this.renderOnlyMode && this.pathExecutor.getState() == State.WAITING && !this.pathfinding) {
                this.succeeded = true;
                this.stop("Completed path queue");
                log("pathqueue empty stopping");
            }
            return;
        }

        if (this.pathfinding) {
            return;
        }

        Vertex.executor().execute(() -> {
            log("creating thread. wasPathfinding: " + this.pathfinding);
            if (this.pathfinding) {
                return;
            }
            this.pathfinding = true;
            try {
                Pair<BlockPos, BlockPos> startEnd = this.pathQueue.poll();
                if (startEnd == null) {
                    this.pathfinding = false;
                    return;
                }

                long startedAtMs = System.currentTimeMillis();
                BlockPos start = startEnd.getFirst();
                BlockPos end = startEnd.getSecond();
                double walkSpeed = mc.player.getSpeed();
                CalculationContext ctx = new CalculationContext(walkSpeed * 1.3, walkSpeed, walkSpeed * 0.3);
                Goal goal = new Goal(end.getX(), end.getY(), end.getZ(), ctx);
                Path path;
                AStarPathFinder.SearchTelemetry searchTelemetry = null;
                double searchMs = 0.0;
                boolean sameHeightSegment = start.getY() == end.getY();
                boolean directWalk = false;
                if (sameHeightSegment && com.vertexai.pathfinder.util.BlockUtil.INSTANCE.canWalkBetween(ctx, start, end)) {
                    // Skip A* when the entire segment is directly traversable.
                    directWalk = true;
                    finder = null;
                    PathNode startNode = new PathNode(start.getX(), start.getY(), start.getZ(), goal);
                    PathNode endNode = new PathNode(end.getX(), end.getY(), end.getZ(), goal);
                    endNode.setParentNode(startNode);
                    path = new Path(startNode, endNode, goal, ctx);
                    log("Skipping A*: direct walkable segment from " + start + " to " + end);
                } else {
                    long searchStartNs = System.nanoTime();
                    // Use the overhauled 8-Way PathFinder engine with a massive 1,000,000 node limit
                    List<BlockPos> rawPath = com.vertexai.pathing.PathFinder.findPath(mc.level, start, end, 1000000);
                    
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
                    searchTelemetry = null;
                    log("done pathfinding using new 8-Way Engine");
                }
                if (path != null) {
                    List<BlockPos> smoothedPath = path.getSmoothedPath();
                    int pathLength = path.getPath().size();
                    int smoothedPathLength = smoothedPath.size();
                    int iterations = searchTelemetry != null ? searchTelemetry.getIterations() : 0;
                    int expandedNodes = searchTelemetry != null ? searchTelemetry.getExpandedNodes() : 0;
                    int openSetPeak = searchTelemetry != null ? searchTelemetry.getOpenSetPeak() : 0;
                    this.lastTelemetry = new PathfindingTelemetry(
                            startedAtMs,
                            System.currentTimeMillis(),
                            true,
                            "",
                            searchMs,
                            path.getSmoothingDurationMs(),
                            expandedNodes,
                            openSetPeak,
                            iterations,
                            pathLength,
                            smoothedPathLength,
                            directWalk
                    );
                    if (this.renderOnlyMode) {
                        this.renderOnlyPathQueue.offer(path);
                        this.succeeded = true;
                        send("Render-only preview ready");
                        log("path preview computed and queued for rendering");
                    } else {
                        PathExecutor.getInstance().queuePath(path);
                        log("starting pathexec");
                    }
                } else {
                    log("No Path Found");
                    failed = true;
                    String failureReason = searchTelemetry != null ? searchTelemetry.getTerminationReason() : "no_path";
                    int iterations = searchTelemetry != null ? searchTelemetry.getIterations() : 0;
                    int expandedNodes = searchTelemetry != null ? searchTelemetry.getExpandedNodes() : 0;
                    int openSetPeak = searchTelemetry != null ? searchTelemetry.getOpenSetPeak() : 0;
                    this.lastTelemetry = new PathfindingTelemetry(
                            startedAtMs,
                            System.currentTimeMillis(),
                            false,
                            failureReason,
                            searchMs,
                            0.0,
                            expandedNodes,
                            openSetPeak,
                            iterations,
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
            }
            this.pathfinding = false;
            this.skipTick = true;
        });
    }

    @Override
    protected void onWorldRender(WorldRenderContextWrapper context) {
        renderInWorld(context);
    }

    public void renderInWorld(WorldRenderContextWrapper context) {
        // this.pathExecutor.onRender();
        Deque<Path> paths = new LinkedList<>(this.pathExecutor.getPathQueue());
        if (pathExecutor.getCurrentPath() != null) {
            paths.add(pathExecutor.getCurrentPath());
        }
        paths.addAll(this.renderOnlyPathQueue);

        boolean seeThrough = Vertex.config() != null && Vertex.config().utilities != null && Vertex.config().utilities.renderPathfinderThroughWalls;

        if (!paths.isEmpty()) {
            RenderUtil.drawBlock(paths.getFirst().getStart(), new Color(0, 255, 0, 150), seeThrough);

            for (Path path : paths) {
                List<BlockPos> bpath = path.getSmoothedPath();

                for (int i = 1; i < bpath.size(); i++) {
                    RenderUtil.drawBlock(bpath.get(i), new Color(0, 255, 0, 150), seeThrough);
                    RenderUtil.drawThinLine(
                            new Vec3(bpath.get(i).getX() + 0.5, bpath.get(i).getY() + 1, bpath.get(i).getZ() + 0.5),
                            new Vec3(bpath.get(i - 1).getX() + 0.5, bpath.get(i - 1).getY() + 1, bpath.get(i - 1).getZ() + 0.5),
                            new Color(0, 255, 0, 150),
                            seeThrough
                    );
                }
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
