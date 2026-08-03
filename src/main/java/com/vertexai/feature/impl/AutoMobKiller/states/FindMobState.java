package com.vertexai.feature.impl.AutoMobKiller.states;

import com.vertexai.Vertex;
import com.vertexai.feature.impl.AutoMobKiller.AutoMobKiller;
import com.vertexai.feature.impl.Pathfinder;
import com.vertexai.util.AngleUtil;
import com.vertexai.util.BlockUtil;
import com.vertexai.util.EntityUtil;
import com.vertexai.util.helper.Clock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class FindMobState implements AutoMobKillerState {

    private static final long SEARCH_ANCHOR_DELAY_MS = 2_500L;
    private static final long SEARCH_ANCHOR_REPATH_MS = 1_250L;

    private final Minecraft mc = Minecraft.getInstance();
    private final Clock noMobLogTimer = new Clock();
    private final Clock noMobAnchorTimer = new Clock();
    private final Clock anchorRepathTimer = new Clock();

    @Override
    public void onStart(AutoMobKiller mobKiller) {
        log("Entering Find Mob State");
        noMobLogTimer.reset();
        noMobAnchorTimer.reset();
        anchorRepathTimer.reset();
    }

    @Override
    public AutoMobKillerState onTick(AutoMobKiller mobKiller) {

        LivingEntity mob = findBestMob(mobKiller);

        if (mob == null) {
            if (!noMobLogTimer.isScheduled()) {
                noMobLogTimer.schedule(2_000);
            }

            if (noMobLogTimer.passed()) {
                log("No mobs found yet, continuing to search...");
                noMobLogTimer.schedule(2_000);
            }

            if (!noMobAnchorTimer.isScheduled()) {
                noMobAnchorTimer.schedule(SEARCH_ANCHOR_DELAY_MS);
            }

            if (noMobAnchorTimer.passed()) {
                moveTowardSearchAnchor(mobKiller);
            }

            mobKiller.setError(AutoMobKiller.MKError.NO_ENTITIES);
            return this;
        }

        noMobAnchorTimer.reset();
        anchorRepathTimer.reset();
        mobKiller.setError(AutoMobKiller.MKError.NONE);
        mobKiller.updateTargetMob(mob);
        mobKiller.setTargetMobOriginalPos(mob.position());
        return new PathfindingState();
    }

    private LivingEntity findBestMob(AutoMobKiller mobKiller) {
        if (mc.level == null || mc.player == null) return null;

        AutoMobKiller.SlayerProfile slayerProfile = mobKiller.getSlayerProfile();

        List<LivingEntity> mobs = EntityUtil.getEntities(mobKiller.getMobsToKill(), mobKiller.getBlacklistedMobs());
        if (mobs.isEmpty()) {
            return null;
        }

        double bestDistanceSq = Double.MAX_VALUE;
        LivingEntity bestMob = null;

        for (LivingEntity mob : mobs) {
            if (mob == null || !mob.isAlive()) continue;
            double distanceSq = mc.player.distanceToSqr(mob);
            if (distanceSq >= (42 * 42)) continue;
            if (!slayerProfile.isTargetInPreferredZone(mob)) continue;

            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                bestMob = mob;
            }
        }

        return bestMob;
    }


    @Override
    public void onEnd(AutoMobKiller mobKiller) {
        log("Exiting Find Mob State");
    }

    private void moveTowardSearchAnchor(AutoMobKiller mobKiller) {
        AutoMobKiller.SlayerProfile profile = mobKiller.getSlayerProfile();
        if (!profile.hasAnchorPoint() || mc.player == null) {
            return;
        }

        BlockPos anchor = profile.getAnchorPoint();
        if (mc.player.blockPosition().distSqr(anchor) <= 25.0) {
            return;
        }

        if (anchorRepathTimer.isScheduled() && !anchorRepathTimer.passed()) {
            return;
        }

        Pathfinder pathfinder = Pathfinder.getInstance();
        pathfinder.setSprintState(Vertex.config().commission.dwarvenCommission.mobKillerSprint);
        pathfinder.setInterpolationState(Vertex.config().commission.dwarvenCommission.mobKillerInterpolate);
        pathfinder.stopAndRequeue(anchor);
        if (!pathfinder.isRunning()) {
            pathfinder.start();
        }

        anchorRepathTimer.schedule(SEARCH_ANCHOR_REPATH_MS);
    }

    private boolean isCrowdedByOtherPlayer(LivingEntity mob, double crowdRadiusSq) {
        for (Player player : mc.level.players()) {
            if (player == null || player == mc.player) continue;
            if (EntityUtil.isNpc(player)) continue;
            if (player.distanceToSqr(mob) < crowdRadiusSq) {
                return true;
            }
        }
        return false;
    }

}
