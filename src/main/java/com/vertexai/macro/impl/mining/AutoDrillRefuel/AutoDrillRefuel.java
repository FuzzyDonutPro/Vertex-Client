package com.vertexai.macro.impl.mining.AutoDrillRefuel;

import lombok.Getter;
import lombok.Setter;
import com.vertexai.failsafe.AbstractFailsafe.Failsafe;
import com.vertexai.macro.AbstractFeature;
import com.vertexai.macro.impl.mining.AutoDrillRefuel.states.AutoDrillRefuelState;
import com.vertexai.macro.impl.mining.AutoDrillRefuel.states.StartingState;

public class AutoDrillRefuel extends AbstractFeature {
    private static final AutoDrillRefuel instance = new AutoDrillRefuel();
    private AutoDrillRefuelError error = AutoDrillRefuelError.NONE;
    private FuelType fuelType;
    private String drillName;
    private AutoDrillRefuelState currentState;

    public static AutoDrillRefuel getInstance() { return instance; }
    public AutoDrillRefuelError getError() { return error; }
    public void setError(AutoDrillRefuelError error) { this.error = error; }
    public FuelType getFuelType() { return fuelType; }
    public String getDrillName() { return drillName; }

    @Override
    public String getName() {
        return "AutoDrillRefuel";
    }

    @Override
    public void resetStatesAfterStop() {
        this.failsafesToIgnore.remove(Failsafe.ITEM_CHANGE);
    }

    public void start(String drillName, FuelType fuelType) {
        this.enabled = true;
        this.drillName = drillName;
        this.fuelType = fuelType;
        this.error = AutoDrillRefuelError.NONE;
        currentState = new StartingState();
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    protected void onTick() {
        if (!this.enabled) {
            return;
        }

        if (currentState == null)
            return;

        AutoDrillRefuelState nextState = currentState.onTick(this);
        transitionTo(nextState);
    }

    private void transitionTo(AutoDrillRefuelState nextState) {
        // Skip if no state change
        if (currentState == nextState)
            return;

        currentState.onEnd(this);
        currentState = nextState;

        if (currentState == null) {
            log("null state, returning");
            return;
        }

        currentState.onStart(this);
    }

    public enum AutoDrillRefuelError {
        NONE,
        NO_DRILL,
        NO_FUEL,
        NO_ABIPHONE,
        NO_GREATFORGE_CONTACT
    }

    public enum FuelType {
        VOLTA("Volta"),
        OIL_BARREL("Oil Barrel"),
        SUNFLOWER_OIL("Sunflower Oil");

        private final String name;

        FuelType(String name) {
            this.name = name;
        }

        public String getName() { return name; }
    }

}
