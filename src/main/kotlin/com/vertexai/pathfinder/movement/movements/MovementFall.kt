package com.vertexai.pathfinder.movement.movements

import com.vertexai.VertexMain
import com.vertexai.pathfinder.movement.CalculationContext
import com.vertexai.pathfinder.movement.Movement
import com.vertexai.pathfinder.movement.MovementResult
import net.minecraft.core.BlockPos

class MovementFall(mm: VertexMain, source: BlockPos, dest: BlockPos) : Movement(mm, source, dest) {
    override fun calculateCost(ctx: CalculationContext, res: MovementResult) {
        calculateCost(ctx, source.x, source.y, source.z, dest.x, dest.z, res)
        costs = res.cost
    }

    companion object {
        fun calculateCost(
            ctx: CalculationContext,
            x: Int,
            y: Int,
            z: Int,
            destX: Int,
            destZ: Int,
            res: MovementResult
        ) {
            res.set(destX, y - 1, destZ)
            MovementDescend.calculateCost(ctx, x, y, z, destX, destZ, res)
        }
    }
}
