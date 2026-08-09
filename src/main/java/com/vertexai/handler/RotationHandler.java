package com.vertexai.handler;

import lombok.Getter;
import com.vertexai.Vertex;
import com.vertexai.event.MotionUpdateEvent;
import com.vertexai.util.AngleUtil;
import com.vertexai.util.helper.Angle;
import com.vertexai.util.helper.RotationConfiguration;
import com.vertexai.util.helper.Target;
import com.vertexai.util.WorldRenderContextWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

/**
 * RotationHandler — High-precision, ultra-smooth humanized camera rotation engine.
 * Eliminates sudden snapping and erratic jitter by executing smooth quintic bezier curves.
 */
public class RotationHandler {

    private static final Logger log = LoggerFactory.getLogger(RotationHandler.class);
    private static RotationHandler instance;

    private final Queue<RotationConfiguration> rotations = new LinkedList<>();
    private final Minecraft mc = Minecraft.getInstance();
    private final Angle startRotation = new Angle(0f, 0f);
    private final Random random = new Random();

    private boolean enabled;
    private long startTime;
    private long endTime;
    private Target target = new Target(new Angle(0, 0));
    private float serverSideYaw = 0;
    private float serverSidePitch = 0;
    private boolean followingTarget = false;
    private boolean stopRequested = false;

    @Getter
    private RotationConfiguration configuration;

    public static RotationHandler getInstance() {
        if (instance == null) {
            instance = new RotationHandler();
        }
        return instance;
    }

    public RotationHandler queueRotation(RotationConfiguration... configs) {
        this.rotations.addAll(Arrays.asList(configs));
        return instance;
    }

    public void start() {
        if (this.rotations.isEmpty() || this.enabled) {
            return;
        }
        this.easeTo(rotations.poll());
    }

    public void easeTo(RotationConfiguration configuration) {
        if (configuration == null || mc.player == null) return;
        this.configuration = configuration;
        this.startTime = System.currentTimeMillis();
        this.startRotation.setRotation(configuration.from().orElse(AngleUtil.getPlayerAngle()));
        
        if (configuration.target().isPresent()) {
            this.target = configuration.target().get();
        }

        Angle change = AngleUtil.getNeededChange(this.startRotation, this.target.getTargetAngle());
        double distance = Math.sqrt(change.getYaw() * change.getYaw() + change.getPitch() * change.getPitch());

        // Calculate smooth duration based on distance (default 160ms - 350ms)
        long duration = configuration.time();
        if (duration <= 0) {
            duration = Math.max(160, Math.min(350, (long) (140 + distance * 1.8)));
        }
        this.endTime = this.startTime + duration;

        if (configuration.rotationType() == RotationConfiguration.RotationType.SERVER) {
            if (serverSideYaw == 0 && serverSidePitch == 0) {
                serverSideYaw = mc.player.getYRot();
                serverSidePitch = mc.player.getXRot();
            } else {
                this.startRotation.setYaw(AngleUtil.get360RotationYaw(serverSideYaw));
                this.startRotation.setPitch(serverSidePitch);
            }
        }

        this.stopRequested = false;
        this.enabled = true;
    }

    private void reset() {
        if (this.stopRequested) {
            this.configuration = null;
            this.target = null;
            this.startTime = this.endTime = 0L;
            this.serverSideYaw = this.serverSidePitch = 0;
        }
        this.enabled = false;
        this.followingTarget = false;
        this.stopRequested = false;
    }

    public void stop() {
        this.rotations.clear();
        this.stopRequested = true;
        this.enabled = false;
    }

    public void onTick() {
        // Called from EventManager
    }

    public void onWorldRender(WorldRenderContextWrapper context) {
        if (!enabled || this.configuration == null || this.configuration.rotationType() != RotationConfiguration.RotationType.CLIENT) {
            return;
        }
        if (mc.player == null || target == null || target.getTargetAngle() == null) return;

        long now = System.currentTimeMillis();
        float totalTime = (float) (this.endTime - this.startTime);
        if (totalTime <= 0) totalTime = 1;

        float timeProgress = Math.min(1.0f, Math.max(0.0f, (now - this.startTime) / totalTime));

        // Quintic Ease-In-Out curve: ultra-smooth human acceleration and deceleration
        float t = timeProgress;
        float easedProgress = t < 0.5f ? 16 * t * t * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 5) / 2;

        float neededYaw = AngleUtil.getNeededYawChange(startRotation.getYaw(), target.getTargetAngle().getYaw());
        float neededPitch = target.getTargetAngle().getPitch() - startRotation.getPitch();

        float currentYaw = startRotation.getYaw() + (neededYaw * easedProgress);
        float currentPitch = startRotation.getPitch() + (neededPitch * easedProgress);

        // Micro human jitter for anticheat safety when randomizedRotations is enabled
        if (Vertex.config() != null && Vertex.config().general.randomizedRotations && t < 0.95f) {
            float yawJitter = (random.nextFloat() - 0.5f) * 0.03f;
            float pitchJitter = (random.nextFloat() - 0.5f) * 0.02f;
            currentYaw += yawJitter;
            currentPitch += pitchJitter;
        }

        mc.player.setYRot(currentYaw);
        mc.player.setXRot(Mth.clamp(currentPitch, -90f, 90f));

        if (now >= this.endTime || this.stopRequested) {
            handleRotationEnd();
        }
    }

    public void onMotionUpdate(MotionUpdateEvent event) {
        if (!enabled || this.configuration == null || this.configuration.rotationType() != RotationConfiguration.RotationType.SERVER) {
            return;
        }
        if (target == null || target.getTargetAngle() == null) return;

        long now = System.currentTimeMillis();
        float totalTime = (float) (this.endTime - this.startTime);
        if (totalTime <= 0) totalTime = 1;
        float timeProgress = Math.min(1.0f, Math.max(0.0f, (now - this.startTime) / totalTime));

        float t = timeProgress;
        float easedProgress = t < 0.5f ? 16 * t * t * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 5) / 2;

        float neededYaw = AngleUtil.getNeededYawChange(startRotation.getYaw(), target.getTargetAngle().getYaw());
        float neededPitch = target.getTargetAngle().getPitch() - startRotation.getPitch();

        serverSideYaw = startRotation.getYaw() + (neededYaw * easedProgress);
        serverSidePitch = startRotation.getPitch() + (neededPitch * easedProgress);

        event.yaw = serverSideYaw;
        event.pitch = serverSidePitch;

        if (now >= this.endTime || this.stopRequested) {
            handleRotationEnd();
        }
    }

    private void handleRotationEnd() {
        if (!this.stopRequested) {
            if (this.configuration != null && this.configuration.rotationType() == RotationConfiguration.RotationType.CLIENT && this.target != null && this.target.getTargetAngle() != null) {
                mc.player.setYRot(this.target.getTargetAngle().getYaw());
                mc.player.setXRot(this.target.getTargetAngle().getPitch());
            }

            if (this.configuration != null && this.configuration.followTarget()) {
                this.easeTo(configuration);
                this.followingTarget = true;
                return;
            }

            if (configuration != null) {
                configuration.callback().ifPresent(Runnable::run);
            }

            if (!this.rotations.isEmpty()) {
                this.easeTo(this.rotations.poll());
                return;
            }
        }
        this.reset();
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean isFollowingTarget() {
        return this.followingTarget;
    }

    public void stopFollowingTarget() {
        this.followingTarget = false;
        this.stop();
    }
}
