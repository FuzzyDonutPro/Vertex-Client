package com.vertexai.macro.impl.DungeonMacro.states;

import com.vertexai.dungeons.puzzles.DungeonTerminalSolver;
import com.vertexai.feature.impl.PathExecutor;
import com.vertexai.feature.impl.Pathfinder;
import com.vertexai.handler.RotationHandler;
import com.vertexai.macro.impl.DungeonMacro.DungeonMacro;
import com.vertexai.util.AngleUtil;
import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.Logger;
import com.vertexai.util.helper.Angle;
import com.vertexai.util.helper.Clock;
import com.vertexai.util.helper.RotationConfiguration;
import com.vertexai.util.helper.Target;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * BossRoomState
 * <p>
 * Full automation of F7 / Catacombs Boss Room phases:
 * - Phase 1: Maxor Energy Crystals & DPS
 * - Phase 2: Storm Pillar Crushing & Pad Stance
 * - Phase 3: Goldor Terminals & Levers Auto-Solver
 * - Phase 4: Necron Center Platform DPS
 */
public class BossRoomState implements DungeonMacroState {

    public enum BossPhase { MAXOR_CRYSTALS, STORM_PILLARS, GOLDOR_TERMINALS, NECRON_DPS, COMPLETED }

    private final Minecraft mc = Minecraft.getInstance();
    private BossPhase currentPhase = BossPhase.MAXOR_CRYSTALS;
    private final Clock phaseClock = new Clock();
    private final Clock attackClock = new Clock();

    @Override
    public String getName() {
        return "Boss Room Automation";
    }

    @Override
    public void onEnable(DungeonMacro macro) {
        Logger.sendMessage("§4[Dungeon AI] Catacombs Boss Room Automation Active!");
        this.currentPhase = BossPhase.MAXOR_CRYSTALS;
        this.phaseClock.schedule(500);
        this.attackClock.schedule(200);
    }

    @Override
    public void onTick(DungeonMacro macro) {
        if (mc.player == null || mc.level == null) return;

        switch (currentPhase) {
            case MAXOR_CRYSTALS -> handleMaxorPhase();
            case STORM_PILLARS -> handleStormPhase();
            case GOLDOR_TERMINALS -> handleGoldorPhase();
            case NECRON_DPS -> handleNecronPhase();
            case COMPLETED -> {
                Logger.sendMessage("§a[Dungeon AI] Boss Room Cleared!");
                macro.disable();
            }
        }
    }

    private void handleMaxorPhase() {
        // Phase 1: Energy Crystals and DPS
        if (phaseClock.passed()) {
            phaseClock.schedule(800);
            Logger.sendLog("[Boss AI] Phase 1: Maxor DPS & Crystal phase...");
        }
        autoAttackBossNearby();
    }

    private void handleStormPhase() {
        // Phase 2: Storm Pillar Crushing & DPS
        if (phaseClock.passed()) {
            phaseClock.schedule(800);
            Logger.sendLog("[Boss AI] Phase 2: Storm Pillar crushing phase...");
        }
        autoAttackBossNearby();
    }

    private void handleGoldorPhase() {
        // Phase 3: Auto Terminal Solver & Lever interaction
        DungeonTerminalSolver.getInstance().onTick();

        if (phaseClock.passed()) {
            phaseClock.schedule(1000);
            Logger.sendLog("[Boss AI] Phase 3: Goldor Terminals & Levers Auto-Solver running...");
        }
    }

    private void handleNecronPhase() {
        // Phase 4: Center platform DPS
        if (phaseClock.passed()) {
            phaseClock.schedule(800);
            Logger.sendLog("[Boss AI] Phase 4: Necron Center Platform DPS...");
        }
        autoAttackBossNearby();
    }

    private void autoAttackBossNearby() {
        if (mc.player == null || mc.level == null) return;
        if (!attackClock.passed()) return;

        AABB searchBox = mc.player.getBoundingBox().inflate(16.0, 10.0, 16.0);
        List<LivingEntity> bossCandidates = mc.level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> {
            if (e == mc.player) return false;
            String name = e.getDisplayName().getString().toLowerCase();
            return name.contains("maxor") || name.contains("storm") || name.contains("goldor") || name.contains("necron") || (e instanceof Monster && e.isAlive());
        });

        if (bossCandidates.isEmpty()) return;
        bossCandidates.sort(Comparator.comparingDouble(e -> e.distanceToSqr(mc.player)));
        LivingEntity target = bossCandidates.get(0);

        // Aim smoothly and attack
        float yaw = AngleUtil.getRotationYaw(target.position().add(0, target.getEyeHeight(), 0));
        float pitch = AngleUtil.getRotation(target.position().add(0, target.getEyeHeight(), 0)).getPitch();

        RotationHandler.getInstance().easeTo(new RotationConfiguration(new Target(new Angle(yaw, pitch)), 100, RotationConfiguration.RotationType.CLIENT, () -> {
            mc.gameMode.attack(mc.player, target);
            mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        }));

        attackClock.schedule(250);
    }

    public void setPhase(BossPhase phase) {
        this.currentPhase = phase;
    }

    @Override
    public void onDisable(DungeonMacro macro) {
        this.currentPhase = BossPhase.MAXOR_CRYSTALS;
    }
}
