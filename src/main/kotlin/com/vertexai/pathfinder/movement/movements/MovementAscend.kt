package com.vertexai.pathfinder.movement.movements

import com.vertexai.VertexMain
import com.vertexai.pathfinder.movement.CalculationContext
import com.vertexai.pathfinder.movement.Movement
import com.vertexai.pathfinder.movement.MovementHelper
import com.vertexai.pathfinder.movement.MovementResult
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.StairBlock
import net.minecraft.world.level.block.state.properties.Half

class MovementAscend(mm: VertexMain, from: BlockPos, to: BlockPos) : Movement(mm, from, to) {
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
            res.set(destX, y + 1, destZ)
            cost(ctx, x, y, z, destX, destZ, res)
        }

        private fun cost(
            ctx: CalculationContext,
            x: Int,
            y: Int,
            z: Int,
            destX: Int,
            destZ: Int,
            res: MovementResult
        ) {
            val destState = ctx.get(destX, y + 1, destZ)
            if (!MovementHelper.canStandOn(ctx.bsa, destX, y + 1, destZ, destState)) return
            if (!MovementHelper.canWalkThrough(ctx.bsa, destX, y + 3, destZ)) return
            if (!MovementHelper.canWalkThrough(ctx.bsa, destX, y + 2, destZ)) return
            if (!MovementHelper.canWalkThrough(ctx.bsa, x, y + 3, z)) return

            val sourceState = ctx.get(x, y, z)
            if (MovementHelper.isLadder(sourceState)) return
            if (MovementHelper.isLadder(destState) && !MovementHelper.canWalkIntoLadder(
                    destState,
                    destX - x,
                    destZ - z
                )
            ) return

            val sourceCollision = MovementHelper.collisionMaxY(sourceState, ctx.world, BlockPos(x, y, z))
            val destCollision = MovementHelper.collisionMaxY(destState, ctx.world, BlockPos(destX, y + 1, destZ))

            val sourceWorldY = y + sourceCollision
            val destWorldY = (y + 1) + destCollision
            val diff = destWorldY - sourceWorldY

            val isAscendingStairs = destState.block is StairBlock && destState.getValue(StairBlock.HALF) == Half.BOTTOM

            res.cost = when {
                diff <= 0.6 || isAscendingStairs -> ctx.cost.ONE_BLOCK_SPRINT_COST
                diff <= 1.25 -> ctx.cost.JUMP_ONE_BLOCK_COST
                else -> ctx.cost.INF_COST // Cannot jump higher than 1.25 blocks
            }
        }
    }
}
