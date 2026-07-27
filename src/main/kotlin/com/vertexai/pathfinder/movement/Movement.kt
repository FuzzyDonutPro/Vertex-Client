package com.vertexai.pathfinder.movement

import com.vertexai.VertexMain
import net.minecraft.core.BlockPos

abstract class Movement(override val mm: VertexMain, override val source: BlockPos, override val dest: BlockPos) :
    IMovement {

    override var costs: Double = 1e6
    override fun getCost() = costs
}
