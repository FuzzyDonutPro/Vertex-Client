package com.vertexai.macro;

import com.vertexai.failsafe.AbstractFailsafe.Failsafe;
import com.vertexai.macro.impl.combat.*;
import com.vertexai.macro.impl.dungeon.*;
import com.vertexai.macro.impl.farming.*;
import com.vertexai.macro.impl.fishing.*;
import com.vertexai.macro.impl.mining.*;
import com.vertexai.macro.impl.mining.AutoDrillRefuel.AutoDrillRefuel;
import com.vertexai.macro.impl.misc.*;
import com.vertexai.macro.impl.misc.AutoGetStats.AutoGetStats;
import com.vertexai.macro.impl.combat.AutoMobKiller.AutoMobKiller;
import com.vertexai.macro.impl.mining.BlockMiner.BlockMiner;
import com.vertexai.macro.impl.navigation.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class FeatureManager {

    private static FeatureManager instance;
    public final Set<AbstractFeature> allFeatures = new LinkedHashSet<>();

    public FeatureManager() {
        this.allFeatures.addAll(Arrays.asList(
                AutoCommissionClaim.getInstance(),
                AutoGetStats.getInstance(),
                AutoMobKiller.getInstance(),
                AutoWarp.getInstance(),
                BlockMiner.getInstance(),
                CommissionDebugMode.getInstance(),
                MouseUngrab.getInstance(),
                Pathfinder.getInstance(),
                RouteBuilder.getInstance(),
                RouteNavigator.getInstance(),
                AutoDrillRefuel.getInstance(),
                AutoChestUnlocker.instance,
                WorldScanner.getInstance(),
                AutoSell.getInstance(),
                Rift.getInstance(),
                AutoClicker.getInstance(),
                AutoBuffs.instance,
                KillSwitch.instance,
                AutoMobHunter.instance,
                AutoFisher.instance,
                com.vertexai.render.WorldESP.instance,
                AutoPestExchange.instance,
                PetSwapper.instance,
                PerformanceMode.instance,
                LagDetector.instance,
                Humanizer.instance,
                StaffDetector.instance,
                BreakScheduler.instance,
                AutoResponder.instance,
                AutoWarp.getInstance(),
                CaptchaDetector.instance,
                RancherSpeedSetter.instance,
                WardrobeSwapper.instance,
                SackManager.instance,
                RefuelEngineSwapper.instance,
                com.vertexai.macro.impl.combat.SlayerQoL.SlayerQoL.getInstance(),
                RouteAutoRecorder.getInstance(),
                FastBreak.instance,
                PerspectiveMod.getInstance(),
                AutoSprint.getInstance(),
                ExperimentationSolver.getInstance(),
                DungeonSolver.getInstance(),
                SessionAnalytics.getInstance()
        ));
    }

    public static FeatureManager getInstance() {
        if (instance == null) {
            instance = new FeatureManager();
        }
        return instance;
    }

    public void enableAll() {
        this.allFeatures.forEach(it -> {
            if (it.shouldStartAtLaunch()) {
                it.start();
            }
        });
    }

    public void disableAll() {
        this.allFeatures.forEach(it -> {
            if (it.isRunning()) {
                it.stop();
            }
        });
        com.vertexai.handler.RotationHandler.getInstance().stop();
    }

    public void pauseAll() {
        this.allFeatures.forEach(it -> {
            if (it.isRunning()) {
                it.pause();
            }
        });
    }

    public void resumeAll() {
        this.allFeatures.forEach(it -> {
            if (it.isRunning()) {
                it.resume();
            }
        });
    }

    private AbstractFeature[] activeCache = new AbstractFeature[0];
    private boolean activeDirty = true;

    public void markActiveDirty() {
        this.activeDirty = true;
    }

    public AbstractFeature[] getActiveFeatures() {
        if (activeDirty) {
            java.util.List<AbstractFeature> list = new java.util.ArrayList<>();
            for (AbstractFeature feature : allFeatures) {
                if (feature.isRunning()) {
                    list.add(feature);
                }
            }
            activeCache = list.toArray(new AbstractFeature[0]);
            activeDirty = false;
        }
        return activeCache;
    }

    public boolean shouldNotCheckForFailsafe() {
        return this.allFeatures.stream().filter(AbstractFeature::isRunning).anyMatch(AbstractFeature::shouldNotCheckForFailsafe);
    }

    public Set<Failsafe> getFailsafesToIgnore() {
        Set<Failsafe> failsafes = new HashSet<>();
        this.allFeatures.forEach(it -> {
            if (it.isRunning()) {
                failsafes.addAll(it.getFailsafesToIgnore());
            }
        });
        return failsafes;
    }
}
