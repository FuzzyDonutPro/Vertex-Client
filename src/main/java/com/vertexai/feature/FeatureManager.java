package com.vertexai.feature;

import com.vertexai.failsafe.AbstractFailsafe.Failsafe;
import com.vertexai.feature.impl.*;
import com.vertexai.feature.impl.AutoDrillRefuel.AutoDrillRefuel;
import com.vertexai.feature.impl.AutoGetStats.AutoGetStats;
import com.vertexai.feature.impl.AutoMobKiller.AutoMobKiller;
import com.vertexai.feature.impl.BlockMiner.BlockMiner;

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
                com.vertexai.feature.impl.AutoMobHunter.instance,
                com.vertexai.feature.impl.AutoFisher.instance,
                com.vertexai.render.WorldESP.instance,
                com.vertexai.feature.impl.AutoPestExchange.instance,
                com.vertexai.feature.impl.PetSwapper.instance,
                com.vertexai.feature.impl.PerformanceMode.instance,
                com.vertexai.feature.impl.LagDetector.instance,
                com.vertexai.feature.impl.Humanizer.instance,
                com.vertexai.feature.impl.StaffDetector.instance,
                com.vertexai.feature.impl.BreakScheduler.instance,
                com.vertexai.feature.impl.AutoResponder.instance,
                com.vertexai.feature.impl.AutoWarp.getInstance(),
                com.vertexai.feature.impl.CaptchaDetector.instance,
                com.vertexai.feature.impl.RancherSpeedSetter.instance,
                com.vertexai.feature.impl.WardrobeSwapper.instance,
                com.vertexai.feature.impl.SackManager.instance,
                com.vertexai.feature.impl.RefuelEngineSwapper.instance,
                com.vertexai.feature.impl.SlayerQoL.SlayerQoL.getInstance(),
                com.vertexai.feature.impl.RouteAutoRecorder.getInstance(),
                com.vertexai.feature.impl.FastBreak.instance,
                com.vertexai.feature.impl.PerspectiveMod.getInstance()
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
