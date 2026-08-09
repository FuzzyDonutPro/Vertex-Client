package com.vertexai.macro.impl.combat.AutoMobKiller.states;

import com.vertexai.macro.impl.combat.AutoMobKiller.AutoMobKiller;

public class StartingState implements AutoMobKillerState {

    @Override
    public void onStart(AutoMobKiller mobKiller) {
        log("Entering Starting State");
    }

    @Override
    public AutoMobKillerState onTick(AutoMobKiller mobKiller) {
        return new FindMobState();
    }

    @Override
    public void onEnd(AutoMobKiller mobKiller) {
        log("Exiting Starting State");
    }

}
