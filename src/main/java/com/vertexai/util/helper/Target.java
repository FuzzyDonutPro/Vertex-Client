package com.vertexai.util.helper;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import com.vertexai.util.AngleUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class Target {

    private Vec3 vec;
    private Entity entity;
    private BlockPos blockPos;
    private Angle angle;
    private float additionalY = (float) (1 + Math.random()) * 0.75f;

    public Entity getEntity() { return entity; }
    public BlockPos getBlockPos() { return blockPos; }
    public Angle getAngle() { return angle; }
    public float additionalY() { return additionalY; }
    public Target additionalY(float additionalY) {
        this.additionalY = additionalY;
        return this;
    }

    public Target(Vec3 vec) {
        this.vec = vec;
    }

    public Target(Entity entity) {
        this.entity = entity;
    }

    public Target(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public Target(Angle angle) {
        this.angle = angle;
    }

    // Ensures Rotation Always Ends with organic, humanized aimpoint offset
    public Angle getTargetAngle() {
        if (blockPos != null) {
            return AngleUtil.getRotation(blockPos);
        }

        if (vec != null) {
            return AngleUtil.getRotation(vec);
        }

        if (entity != null) {
            long now = System.currentTimeMillis();
            // Slow organic aim point drift within the mob's hitbox instead of robotic dead-center lock
            double swayX = Math.sin(now * 0.0023) * (entity.getBbWidth() * 0.22);
            double swayZ = Math.cos(now * 0.0029) * (entity.getBbWidth() * 0.22);
            double swayY = Math.sin(now * 0.0017) * (entity.getBbHeight() * 0.14);

            double targetY = entity.getY() + (entity.getBbHeight() * 0.52) + swayY;
            return AngleUtil.getRotation(new Vec3(entity.getX() + swayX, targetY, entity.getZ() + swayZ));
        }

        return angle;
    }

    @Override
    public String toString() {
        return "Vec3: " + this.vec + ", Ent: " + (this.entity != null ? this.entity.getId() : "null") + ", Pos: " + this.blockPos + ", Angle: " + this.angle;
    }
}
