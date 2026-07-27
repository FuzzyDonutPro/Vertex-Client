package com.vertexai.pathfinder.calculate

import com.vertexai.pathfinder.goal.Goal
import com.vertexai.pathfinder.movement.CalculationContext
import com.vertexai.pathfinder.util.BlockUtil
import com.vertexai.pathfinder.util.toVec3
import com.vertexai.pathfinder.util.world
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import java.util.*

class Path(start: PathNode, end: PathNode, val goal: Goal, val ctx: CalculationContext) {
    var start: BlockPos = BlockPos(start.x, start.y, start.z)
    var end: BlockPos = BlockPos(end.x, end.y, end.z)
    var path: List<BlockPos>
    var node: List<PathNode>
    var smoothPath: List<BlockPos> = listOf()
    var smoothingDurationMs: Double = -1.0
        private set

    init {
        var temp: PathNode? = end
        val listOfBlocks = LinkedList<BlockPos>()
        val listOfNodes = LinkedList<PathNode>()
        while (temp != null) {
            listOfNodes.addFirst(temp)
            listOfBlocks.addFirst(BlockPos(temp.x, temp.y, temp.z))
            temp = temp.parentNode
        }
        path = listOfBlocks.toList()
        node = listOfNodes.toList()
    }

    // english is not my strong suit
    fun getSmoothedPath(): List<BlockPos> {
        if (smoothPath.isNotEmpty()) return smoothPath

        val startedNs = System.nanoTime()
        val smooth = mutableListOf<BlockPos>()
        if (path.isNotEmpty()) {
            smooth.add(path[0])
            if (path.size > 1) {
                var prevDx = path[1].x - path[0].x
                var prevDy = path[1].y - path[0].y
                var prevDz = path[1].z - path[0].z
                var lastPreservedY = path[0].y
                
                for (i in 1 until path.size - 1) {
                    val curr = path[i]
                    val next = path[i + 1]
                    val dx = next.x - curr.x
                    val dy = next.y - curr.y
                    val dz = next.z - curr.z
                    
                    val hasJumpBoost = net.minecraft.client.Minecraft.getInstance().player?.hasEffect(net.minecraft.world.effect.MobEffects.JUMP_BOOST) == true
                    var shouldPreserve = dx != prevDx || dy != prevDy || dz != prevDz
                    
                    if (dy != 0 || prevDy != 0) {
                        if (hasJumpBoost) {
                            if (Math.abs(curr.y - lastPreservedY) >= 2) {
                                shouldPreserve = true
                            }
                        } else {
                            shouldPreserve = true
                        }
                    }
                    
                    if (shouldPreserve) {
                        smooth.add(curr)
                        lastPreservedY = curr.y
                        prevDx = dx
                        prevDy = dy
                        prevDz = dz
                    }
                }
                smooth.add(path.last())
            }
        }
        smoothPath = smooth.toList()
        smoothingDurationMs = (System.nanoTime() - startedNs) / 1_000_000.0
        return smoothPath
    }

    fun reconstructPath(end: PathNode): List<BlockPos> {
        val path = mutableListOf<BlockPos>()
        var currentNode: PathNode? = end
        while (currentNode != null) {
            path.add(0, currentNode.getBlock())
            currentNode = currentNode.parentNode
        }
        
        val smooth = mutableListOf<BlockPos>()
        if (path.isNotEmpty()) {
            smooth.add(path[0])
            if (path.size > 1) {
                var prevDx = path[1].x - path[0].x
                var prevDy = path[1].y - path[0].y
                var prevDz = path[1].z - path[0].z
                var lastPreservedY = path[0].y
                
                for (i in 1 until path.size - 1) {
                    val curr = path[i]
                    val next = path[i + 1]
                    val dx = next.x - curr.x
                    val dy = next.y - curr.y
                    val dz = next.z - curr.z
                    
                    val hasJumpBoost = net.minecraft.client.Minecraft.getInstance().player?.hasEffect(net.minecraft.world.effect.MobEffects.JUMP_BOOST) == true
                    var shouldPreserve = dx != prevDx || dy != prevDy || dz != prevDz
                    
                    if (dy != 0 || prevDy != 0) {
                        if (hasJumpBoost) {
                            if (Math.abs(curr.y - lastPreservedY) >= 2) {
                                shouldPreserve = true
                            }
                        } else {
                            shouldPreserve = true
                        }
                    }
                    
                    if (shouldPreserve) {
                        smooth.add(curr)
                        lastPreservedY = curr.y
                        prevDx = dx
                        prevDy = dy
                        prevDz = dz
                    }
                }
                smooth.add(path.last())
            }
        }
        return smooth.toList()
    }

}
