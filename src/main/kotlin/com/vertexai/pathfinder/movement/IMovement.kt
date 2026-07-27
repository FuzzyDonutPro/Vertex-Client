package com.vertexai.pathfinder.movement

import com.vertexai.VertexMain
import net.minecraft.core.BlockPos

interface IMovement {
    val mm: VertexMain
    val source: BlockPos
    val dest: BlockPos
    val costs: Double // plural cuz kotlin gae

    fun getCost(): Double
    fun calculateCost(ctx: CalculationContext, res: MovementResult)
}
