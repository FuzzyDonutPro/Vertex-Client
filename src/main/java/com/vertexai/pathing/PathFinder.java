package com.vertexai.pathing;

import com.vertexai.Vertex;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

import java.util.*;

import com.vertexai.handler.RouteHandler;
import com.vertexai.handler.GameStateHandler;
import com.vertexai.util.helper.location.Location;
import com.vertexai.util.helper.route.Route;
import com.vertexai.util.helper.route.RouteWaypoint;

public class PathFinder {
    public static List<BlockPos> findPath(Level world, BlockPos start, BlockPos end, int maxNodes) {
        if (start.equals(end)) return Collections.singletonList(end);

        Map<BlockPos, List<NeighborResult>> highways = buildHighwayMap();

        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<BlockPos, Node> allNodes = new HashMap<>();
        Set<BlockPos> closedSet = new HashSet<>();
        
        Node startNode = new Node(start, null, 0, getHeuristic(start, end));
        openSet.add(startNode);
        allNodes.put(start, startNode);
        
        int nodesEvaluated = 0;
        
        while (!openSet.isEmpty() && nodesEvaluated < maxNodes) {
            Node current = openSet.poll();
            
            if (closedSet.contains(current.pos)) continue; // Skip if already visited optimally
            closedSet.add(current.pos);
            nodesEvaluated++;
            
            if (current.pos.distManhattan(end) <= 1 || current.pos.equals(end)) {
                return smoothPath(world, reconstructPath(current));
            }
            
            for (NeighborResult neighborResult : getNeighbors(world, current.pos, highways)) {
                BlockPos neighborPos = neighborResult.pos;
                if (closedSet.contains(neighborPos)) continue;

                double tentativeG = current.gScore + neighborResult.cost;
                
                // Add penalty for turning to prefer straight lines and avoid zigzags
                if (current.parent != null) {
                    int prevDx = current.pos.getX() - current.parent.pos.getX();
                    int prevDy = current.pos.getY() - current.parent.pos.getY();
                    int prevDz = current.pos.getZ() - current.parent.pos.getZ();
                    
                    int currDx = neighborPos.getX() - current.pos.getX();
                    int currDy = neighborPos.getY() - current.pos.getY();
                    int currDz = neighborPos.getZ() - current.pos.getZ();
                    
                    if (prevDx != currDx || prevDy != currDy || prevDz != currDz) {
                        tentativeG += 1.0; // Turn penalty
                    }
                }
                
                Node neighbor = allNodes.get(neighborPos);
                
                if (neighbor == null || tentativeG < neighbor.gScore) {
                    Node newNode = new Node(neighborPos, current, tentativeG, getHeuristic(neighborPos, end));
                    allNodes.put(neighborPos, newNode);
                    openSet.add(newNode);
                }
            }
        }
        
        // If we ran out of nodes or the open set is empty, return the best partial path found so far.
        // This is strictly required because destinations farther than the simulation distance (e.g. 192 blocks)
        // are in unloaded chunks, making them mathematically unreachable. By returning a partial path to the chunk border,
        // the bot can walk there and load the new chunks to continue pathfinding.
        Node best = allNodes.values().stream().min(Comparator.comparingDouble(n -> n.hScore)).orElse(startNode);
        return smoothPath(world, reconstructPath(best));
    }
    
    private static List<BlockPos> smoothPath(Level world, List<BlockPos> path) {
        // Disable aggressive string pulling. 8-way diagonal A* is already naturally smooth,
        // and string pulling causes the bot to cut corners and clip into fences/walls.
        return path;
    }

    private static boolean hasClearLineOfSight(Level world, BlockPos start, BlockPos end) {
        return false; // Deprecated by removing smoothPath
    }

    private static Map<BlockPos, List<NeighborResult>> buildHighwayMap() {
        Map<BlockPos, List<NeighborResult>> map = new HashMap<>();
        
        Location currentLocation = GameStateHandler.getInstance().getCurrentLocation();
        if (currentLocation == null) return map;
        
        String locationPrefix = currentLocation.getName().split(" ")[0].toLowerCase();
        
        if (RouteHandler.getInstance() == null || RouteHandler.getInstance().getPathfinderRoutes() == null) return map;
        
        for (Map.Entry<String, Route> entry : RouteHandler.getInstance().getPathfinderRoutes().entrySet()) {
            String routeName = entry.getKey();
            Route route = entry.getValue();
            
            if (route == null || route.isEmpty()) continue;
            
            // Only inject routes that start with the location prefix (e.g. "Dwarven" for "Dwarven Mines")
            if (!routeName.toLowerCase().startsWith(locationPrefix)) continue;
            
            for (int i = 0; i < route.size(); i++) {
                BlockPos current = route.get(i).toBlockPos();
                List<NeighborResult> connections = map.computeIfAbsent(current, k -> new ArrayList<>());
                
                if (i > 0) {
                    BlockPos prev = route.get(i - 1).toBlockPos();
                    double dist = Math.sqrt(current.distSqr(prev));
                    connections.add(new NeighborResult(prev, dist));
                }
                
                if (i < route.size() - 1) {
                    BlockPos next = route.get(i + 1).toBlockPos();
                    double dist = Math.sqrt(current.distSqr(next));
                    connections.add(new NeighborResult(next, dist));
                }
            }
        }
        return map;
    }

    private static List<NeighborResult> getNeighbors(Level world, BlockPos pos, Map<BlockPos, List<NeighborResult>> highways) {
        List<NeighborResult> neighbors = new ArrayList<>();
        // Inject fast highway connections directly
        if (highways.containsKey(pos)) {
            neighbors.addAll(highways.get(pos));
        }

        // 8-way directional movement
        int[][] directions = {
            {1,0}, {-1,0}, {0,1}, {0,-1},
            {1,1}, {1,-1}, {-1,1}, {-1,-1}
        };
        
        boolean ignoreFallDamage = Vertex.config() != null && Vertex.config().general.ignoreFallDamageInPathfinding;
        int maxDrop = ignoreFallDamage ? 17 : 3;

        for (int[] dir : directions) {
            int dx = dir[0];
            int dz = dir[1];
            double baseCost = (dx != 0 && dz != 0) ? 1.414 : 1.0; // Diagonal cost
            boolean isDiag = (dx != 0 && dz != 0);

            // Walk (y)
            BlockPos walkPos = pos.offset(dx, 0, dz);
            if (isWalkable(world, walkPos)) {
                boolean clear = true;
                if (isDiag) {
                    if (!isAir(world, pos.offset(dx, 0, 0)) || !isAir(world, pos.offset(0, 0, dz)) ||
                        !isAir(world, pos.offset(dx, 1, 0)) || !isAir(world, pos.offset(0, 1, dz))) {
                        clear = false;
                    }
                }
                if (clear) {
                    neighbors.add(new NeighborResult(walkPos, baseCost));
                }
            }
            
            // Jump (y+1)
            BlockPos jumpPos = pos.offset(dx, 1, dz);
            if (isWalkable(world, jumpPos) && isAir(world, pos.above(2))) {
                boolean clear = true;
                if (isDiag) {
                    // Check corners for Y=0, Y=1 (body) and Y=2 (head during jump)
                    if (!isAir(world, pos.offset(dx, 0, 0)) || !isAir(world, pos.offset(0, 0, dz)) ||
                        !isAir(world, pos.offset(dx, 1, 0)) || !isAir(world, pos.offset(0, 1, dz)) ||
                        !isAir(world, pos.offset(dx, 2, 0)) || !isAir(world, pos.offset(0, 2, dz))) {
                        clear = false;
                    }
                }
                if (clear) {
                    neighbors.add(new NeighborResult(jumpPos, baseCost + 2.0));
                }
            }
            
            // Fall (y-1 to y-maxDrop)
            for (int drop = 1; drop <= maxDrop; drop++) {
                BlockPos fallPos = pos.offset(dx, -drop, dz);
                if (isWalkable(world, fallPos)) {
                    boolean clear = true;
                    // Check direct vertical clearance at the destination column
                    for (int dy = 0; dy < drop; dy++) {
                        if (!isAir(world, pos.offset(dx, -dy, dz))) clear = false;
                    }
                    // If falling diagonally, check all corner blocks on the way down to avoid clipping
                    if (clear && isDiag) {
                        for (int dy = -drop; dy <= 1; dy++) { // Check from floor to head height
                            if (!isAir(world, pos.offset(dx, dy, 0)) || !isAir(world, pos.offset(0, dy, dz))) {
                                clear = false;
                                break;
                            }
                        }
                    }
                    if (clear) {
                        neighbors.add(new NeighborResult(fallPos, baseCost + (drop * 0.2)));
                        break;
                    }
                }
            }
        }
        return neighbors;
    }

    private static boolean isWalkable(Level world, BlockPos pos) {
        if (!isAir(world, pos) || !isAir(world, pos.above())) return false;
        
        BlockPos below = pos.below();
        if (isHazard(world, pos) || isHazard(world, pos.above()) || isHazard(world, below)) {
            return false;
        }

        BlockState belowState = world.getBlockState(below);
        net.minecraft.world.phys.shapes.VoxelShape shape = belowState.getCollisionShape(world, below);
        
        // Exclude fences, fence gates, walls, iron bars, and blocks with weird collision boxes
        if (belowState.is(net.minecraft.tags.BlockTags.FENCES) || 
            belowState.is(net.minecraft.tags.BlockTags.FENCE_GATES) || 
            belowState.is(net.minecraft.tags.BlockTags.WALLS) || 
            belowState.getBlock() == Blocks.IRON_BARS ||
            belowState.getBlock() instanceof net.minecraft.world.level.block.DoorBlock) {
            return false;
        }

        // The block below must have a solid collision shape to stand on
        return !shape.isEmpty();
    }

    private static boolean isAir(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        // Treat signs, torches, etc., as non-air if they obstruct movement or have collision
        if (!state.getCollisionShape(world, pos).isEmpty()) {
            // Open fence gates are safe to walk through
            if (state.is(net.minecraft.tags.BlockTags.FENCE_GATES) && state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN) && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN)) {
                // Treated as air
            } else {
                return false;
            }
        }
        
        // Specifically treat torches and other small non-collidable blocks as solid to avoid clipping or getting stuck on them
        if (state.getBlock() == Blocks.TORCH || state.getBlock() == Blocks.WALL_TORCH || state.getBlock() == Blocks.SOUL_TORCH || state.getBlock() == Blocks.SOUL_WALL_TORCH || state.getBlock() == Blocks.REDSTONE_TORCH || state.getBlock() == Blocks.REDSTONE_WALL_TORCH) {
             return false;
        }
        
        return !isHazard(world, pos);
    }

    private static boolean isHazard(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.is(Blocks.LAVA) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) 
            || state.is(Blocks.CACTUS) || state.is(Blocks.MAGMA_BLOCK) 
            || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)
            || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.COBWEB)
            || state.is(Blocks.POWDER_SNOW);
    }

    private static double getHeuristic(BlockPos a, BlockPos b) {
        // Octile distance heuristic for 8-way grid
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        int dz = Math.abs(a.getZ() - b.getZ());
        int max_xz = Math.max(dx, dz);
        int min_xz = Math.min(dx, dz);
        
        // Multiply by 1.5 to make it a slightly Greedy A* search.
        // This provides a good balance between performance and natural, organic pathing.
        // Extreme values (like 5.0) cause the bot to aggressively hug walls and zigzag over rough terrain.
        return ((min_xz * 1.414) + (max_xz - min_xz) + dy) * 1.5;
    }

    private static List<BlockPos> reconstructPath(Node node) {
        List<BlockPos> path = new ArrayList<>();
        Node current = node;
        while (current != null) {
            path.add(0, current.pos);
            current = current.parent;
        }
        return path;
    }

    private static class NeighborResult {
        BlockPos pos;
        double cost;
        NeighborResult(BlockPos pos, double cost) {
            this.pos = pos;
            this.cost = cost;
        }
    }

    private static class Node {
        BlockPos pos;
        Node parent;
        double gScore; // Cost from start
        double hScore; // Heuristic to end
        double fScore; // g + h

        Node(BlockPos pos, Node parent, double gScore, double hScore) {
            this.pos = pos;
            this.parent = parent;
            this.gScore = gScore;
            this.hScore = hScore;
            this.fScore = gScore + hScore;
        }
    }
}
