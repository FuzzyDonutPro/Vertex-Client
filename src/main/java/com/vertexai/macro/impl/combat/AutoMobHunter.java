package com.vertexai.macro.impl.combat;

import lombok.Getter;
import com.vertexai.macro.AbstractFeature;
import com.vertexai.handler.RotationHandler;
import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.helper.Angle;
import com.vertexai.util.helper.Clock;
import com.vertexai.util.helper.RotationConfiguration;
import com.vertexai.util.helper.Target;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.stream.StreamSupport;

/**
 * AutoMobHunter â€” Smoothly rotates camera to target nearby Pests / Slayer Mobs and attacks.
 */
public class AutoMobHunter extends AbstractFeature {

    @Getter
    public static final AutoMobHunter instance = new AutoMobHunter();

    private final Clock attackClock = new Clock();
    private LivingEntity currentTarget = null;

    @Override
    public String getName() {
        return "AutoMobHunter";
    }

    @Override
    protected void onTick() {
        if (mc.player == null || mc.level == null) return;

        // Find closest valid living entity within 6 blocks
        currentTarget = StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false)
                .filter(e -> e instanceof LivingEntity && e != mc.player && e.isAlive())
                .map(e -> (LivingEntity) e)
                .filter(e -> (e instanceof Monster || e instanceof Animal || isPestOrSlayer(e)))
                .filter(e -> mc.player.distanceTo(e) <= 6.0f)
                .min(Comparator.comparingDouble(e -> (double) mc.player.distanceTo(e)))
                .orElse(null);

        if (currentTarget == null) {
            return;
        }

        // Aim towards mob eye position smoothly
        Vec3 eyes = currentTarget.getEyePosition();
        double dx = eyes.x - mc.player.getX();
        double dy = eyes.y - mc.player.getEyeY();
        double dz = eyes.z - mc.player.getZ();
        double distXZ = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float targetPitch = (float) Math.toDegrees(-Math.atan2(dy, distXZ));

        RotationHandler.getInstance().easeTo(new RotationConfiguration(
                new Target(new Angle(targetYaw, targetPitch)),
                120, // Fast 120ms snap to target
                null
        ));

        // Attack key trigger
        if (!attackClock.isScheduled() || attackClock.passed()) {
            KeyBindUtil.leftClick();
            attackClock.schedule(150); // ~6.6 CPS towards target
        }
    }

    private boolean isPestOrSlayer(LivingEntity entity) {
        String name = entity.getName().getString().toLowerCase();
        return name.contains("beetle") || name.contains("cricket") || name.contains("fly") ||
               name.contains("locust") || name.contains("mite") || name.contains("slayer");
    }
}
