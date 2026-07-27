package com.vertexai.pathing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public class PathFinder {
    public static List<BlockPos> findPath(World world, BlockPos start, BlockPos end, int maxNodes) {
        if (start.equals(end)) return Collections.singletonList(end);

        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<BlockPos, Node> allNodes = new HashMap<>();
        
        Node startNode = new Node(start, null, 0, getHeuristic(start, end));
        openSet.add(startNode);
        allNodes.put(start, startNode);
        
        int nodesEvaluated = 0;
        
        while (!openSet.isEmpty() && nodesEvaluated < maxNodes) {
            Node current = openSet.poll();
            nodesEvaluated++;
            
            if (current.pos.getManhattanDistance(end) <= 1 || current.pos.equals(end)) {
                return reconstructPath(current);
            }
            
            for (BlockPos neighborPos : getNeighbors(world, current.pos)) {
                double tentativeG = current.gScore + 1.0; // Assume cost of 1 per block
                
                Node neighbor = allNodes.get(neighborPos);
                if (neighbor == null) {
                    neighbor = new Node(neighborPos, current, tentativeG, getHeuristic(neighborPos, end));
                    allNodes.put(neighborPos, neighbor);
                    openSet.add(neighbor);
                } else if (tentativeG < neighbor.gScore) {
                    neighbor.parent = current;
                    neighbor.gScore = tentativeG;
                    neighbor.fScore = tentativeG + getHeuristic(neighborPos, end);
                    // PriorityQueue doesn't update automatically, remove and re-add
                    openSet.remove(neighbor);
                    openSet.add(neighbor);
                }
            }
        }
        
        // If we ran out of nodes, just return the best we found (the one closest to end)
        Node best = allNodes.values().stream().min(Comparator.comparingDouble(n -> n.hScore)).orElse(startNode);
        return reconstructPath(best);
    }
    
    private static List<BlockPos> getNeighbors(World world, BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>();
        int[][] directions = {{1,0,0}, {-1,0,0}, {0,0,1}, {0,0,-1}, {0,1,0}, {0,-1,0}};
        
        for (int[] dir : directions) {
            BlockPos n = pos.add(dir[0], dir[1], dir[2]);
            // Very rudimentary passability check: if it's air or nonsolid
            if (world.getBlockState(n).getCollisionShape(world, n).isEmpty()) {
                neighbors.add(n);
            }
        }
        return neighbors;
    }

    private static double getHeuristic(BlockPos a, BlockPos b) {
        return Math.sqrt(a.getSquaredDistance(b));
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
