package com.vertexai.util.helper;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;

import java.util.Optional;
import java.util.Random;
import java.util.function.Function;

public class RotationConfiguration {
    private final Minecraft mc = Minecraft.getInstance();
    private Optional<Angle> from = Optional.empty();
    private Optional<Angle> to = Optional.empty();
    private Optional<Target> target = Optional.empty();
    private Optional<Runnable> callback = Optional.empty();
    private long time;
    private boolean easeBackToClientSide = false;
    private boolean followTarget = false;
    private RotationType rotationType = RotationType.CLIENT;
    private Ease easeFunction = Ease.values()[new Random().nextInt(Ease.values().length - 1)];
    private boolean randomness = false;

    public Optional<Angle> from() { return from; }
    public RotationConfiguration from(Optional<Angle> from) { this.from = from; return this; }
    public Optional<Angle> to() { return to; }
    public RotationConfiguration to(Optional<Angle> to) { this.to = to; return this; }
    public Optional<Target> target() { return target; }
    public RotationConfiguration target(Optional<Target> target) { this.target = target; return this; }
    public Optional<Runnable> callback() { return callback; }
    public RotationConfiguration callback(Optional<Runnable> callback) { this.callback = callback; return this; }
    public long time() { return time; }
    public RotationConfiguration time(long time) { this.time = time; return this; }
    public boolean easeBackToClientSide() { return easeBackToClientSide; }
    public RotationConfiguration easeBackToClientSide(boolean easeBackToClientSide) { this.easeBackToClientSide = easeBackToClientSide; return this; }
    public boolean followTarget() { return followTarget; }
    public RotationConfiguration followTarget(boolean followTarget) { this.followTarget = followTarget; return this; }
    public RotationType rotationType() { return rotationType; }
    public RotationConfiguration rotationType(RotationType rotationType) { this.rotationType = rotationType; return this; }
    public boolean randomness() { return randomness; }
    public RotationConfiguration randomness(boolean randomness) { this.randomness = randomness; return this; }

    public RotationConfiguration(Angle from, Angle to, long time, RotationType rotationType, Runnable callback) {
        this.from = Optional.of(from);
        this.to = Optional.ofNullable(to);
        this.target = Optional.of(new Target(to));
        this.time = time;
        this.rotationType = rotationType;
        this.callback = Optional.ofNullable(callback);
    }

    public RotationConfiguration(Angle from, Target target, long time, RotationType rotationType, Runnable callback) {
        this.from = Optional.of(from);
        this.time = time;
        this.target = Optional.ofNullable(target);
        this.rotationType = rotationType;
        this.callback = Optional.ofNullable(callback);
    }

    public RotationConfiguration(Angle to, long time, Runnable callback) {
        this.to = Optional.ofNullable(to);
        this.target = Optional.of(new Target(to));
        this.time = time;
        this.callback = Optional.ofNullable(callback);
    }

    public RotationConfiguration(Angle to, long time, RotationType rotationType, Runnable callback) {
        this.to = Optional.ofNullable(to);
        this.target = Optional.of(new Target(to));
        this.time = time;
        this.rotationType = rotationType;
        this.callback = Optional.ofNullable(callback);
    }

    public RotationConfiguration(Target target, long time, Runnable callback) {
        this.time = time;
        this.target = Optional.ofNullable(target);
        this.callback = Optional.ofNullable(callback);
    }

    public RotationConfiguration(Target target, long time, RotationType rotationType, Runnable callback) {
        this.time = time;
        this.target = Optional.ofNullable(target);
        this.rotationType = rotationType;
        this.callback = Optional.ofNullable(callback);
    }

    public RotationConfiguration setTarget(final Target target) {
        this.target = Optional.of(target);
        return this;
    }

    public RotationConfiguration easeFunction(Ease ease) {
        this.easeFunction = ease;
        return this;
    }

    public Ease easeFunction() {
        return this.easeFunction;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public enum RotationType {
        SERVER,
        CLIENT
    }

    public enum Ease {
        //        EASE_OUT_BACK(x -> 1 + (1 + 2 * (x - 1)) * (x - 1) * (x - 1)),
        EASE_OUT_SINE(x -> (float) Math.sin((x * Math.PI) / 2)),
        EASE_IN_OUT_SINE(x -> (float) (-(Math.cos(x * Math.PI) - 1) / 2)),
        EASE_OUT_QUAD(x -> 1 - (1 - x) * (1 - x)),
        EASE_OUT_CUBIC(x -> 1 - (1 - x) * (1 - x) * (1 - x)),
        EASE_OUT_CIRC(x -> (float) Math.sqrt(1 - (x - 1) * (x - 1)));
//        EASE_OUT_MIN_JERK(x -> (float) (6 * Math.pow(x, 5) - 15 * Math.pow(x, 4) + 10 * Math.pow(x, 3)));

        private final Function<Float, Float> easingFunction;

        Ease(Function<Float, Float> easingFunction) {
            this.easingFunction = easingFunction;
        }

        public float invoke(float x) {
            return easingFunction.apply(x);
        }
    }
}
