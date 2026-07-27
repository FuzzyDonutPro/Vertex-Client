package com.vertexai.feature.impl;

import com.vertexai.Vertex;
import com.vertexai.event.BlockChangeEvent;
import com.vertexai.feature.AbstractFeature;
import com.vertexai.feature.impl.rift.AutoStunSnake;
import com.vertexai.feature.impl.rift.VampireSlayerTracker;
import com.vertexai.handler.GameStateHandler;
import com.vertexai.util.helper.location.SubLocation;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.Packet;

public class Rift extends AbstractFeature {

    private static Rift instance;
    private final VampireSlayerTracker vampireSlayerTracker = VampireSlayerTracker.getInstance();
    private final AutoStunSnake autoStunSnake = AutoStunSnake.getInstance();
    private boolean wasInStillgoreChateau = false;
    private boolean wasInLivingCaveOrStillness = false;

    public static Rift getInstance() {
        if (instance == null) {
            instance = new Rift();
        }
        return instance;
    }

    @Override
    public String getName() {
        return "Rift";
    }

    @Override
    public boolean isEnabled() {
        return Vertex.config().rift.riftEnabled;
    }

    @Override
    public boolean shouldStartAtLaunch() {
        return this.isEnabled();
    }

    @Override
    public void start() {
        this.enabled = true;
        log("Rift module enabled");
    }

    @Override
    public void stop() {
        this.enabled = false;
        log("Rift module disabled");
    }

    @Override
    protected void onTick() {
        if (!Vertex.config().rift.riftEnabled) {
            return;
        }
        SubLocation currentSubLocation = GameStateHandler.getInstance().getCurrentSubLocation();
        boolean inStillgoreChateau = currentSubLocation == SubLocation.STILLGORE_CHATEAU
                || currentSubLocation == SubLocation.OUBLIETTE;
        boolean inLivingCaveOrStillness = currentSubLocation == SubLocation.LIVING_CAVE
                || currentSubLocation == SubLocation.LIVING_STILLNESS;
        if (!inStillgoreChateau) {
            if (wasInStillgoreChateau) {
                log("Left Stillgore Chateau/Oubliette.");
                vampireSlayerTracker.onWorldUnload();
            }
            wasInStillgoreChateau = false;
        } else {
            if (!wasInStillgoreChateau) {
                log("Entered Stillgore Chateau/Oubliette.");
            }
            wasInStillgoreChateau = true;
            vampireSlayerTracker.onTick();
        }
        if (!inLivingCaveOrStillness) {
            if (wasInLivingCaveOrStillness) {
                log("Left Living Cave/Stillness.");
                autoStunSnake.onWorldUnload();
            }
            wasInLivingCaveOrStillness = false;
            return;
        }
        if (!wasInLivingCaveOrStillness) {
            log("Entered Living Cave/Stillness.");
        }
        wasInLivingCaveOrStillness = true;
        autoStunSnake.onTick();
    }

    @Override
    protected void onPacketReceive(Packet<?> packet) {
        if (!Vertex.config().rift.riftEnabled || !isInStillgoreChateau()) {
            return;
        }
        vampireSlayerTracker.onPacketReceive(packet);
    }

    @Override
    protected void onBlockChange(BlockChangeEvent event) {
        if (!Vertex.config().rift.riftEnabled || !isInLivingCaveOrStillness()) {
            return;
        }
        autoStunSnake.onBlockChange(event);
    }

    @Override
    protected void onWorldRender(WorldRenderContext context) {
        if (!Vertex.config().rift.riftEnabled || !isInLivingCaveOrStillness()) {
            return;
        }
        autoStunSnake.onWorldRender(context);
    }

    @Override
    protected void onWorldUnload(ClientLevel world) {
        if (wasInStillgoreChateau || wasInLivingCaveOrStillness) {
            log("Rift world unloaded.");
        }
        vampireSlayerTracker.onWorldUnload();
        autoStunSnake.onWorldUnload();
        wasInStillgoreChateau = false;
        wasInLivingCaveOrStillness = false;
    }

    private boolean isInStillgoreChateau() {
        return GameStateHandler.getInstance().getCurrentSubLocation() == SubLocation.STILLGORE_CHATEAU
                || GameStateHandler.getInstance().getCurrentSubLocation() == SubLocation.OUBLIETTE;
    }

    private boolean isInLivingCaveOrStillness() {
        SubLocation currentSubLocation = GameStateHandler.getInstance().getCurrentSubLocation();
        return currentSubLocation == SubLocation.LIVING_CAVE
                || currentSubLocation == SubLocation.LIVING_STILLNESS;
    }
}
