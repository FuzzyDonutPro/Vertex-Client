package com.vertexai.command;

import com.mojang.brigadier.CommandDispatcher;
import com.vertexai.feature.impl.RouteAutoRecorder;
import com.vertexai.handler.RouteHandler;
import com.vertexai.util.Logger;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class RouteAutoRecorderCommand {

    public void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("routerec")
                .executes(context -> {
                    RouteAutoRecorder.getInstance().toggleRecording();
                    return 1;
                })
                .then(literal("start").executes(context -> {
                    RouteAutoRecorder.getInstance().startRecording();
                    return 1;
                }))
                .then(literal("stop").executes(context -> {
                    RouteAutoRecorder.getInstance().stopRecording();
                    return 1;
                }))
                .then(literal("clear").executes(context -> {
                    RouteHandler.getInstance().getSelectedRoute().clear();
                    Logger.sendMessage("RouteAutoRecorder: Cleared current route waypoints.");
                    return 1;
                }))
                .then(literal("save").executes(context -> {
                    RouteHandler.getInstance().saveData();
                    Logger.sendMessage("RouteAutoRecorder: Saved route waypoints to config.");
                    return 1;
                }))
        );
    }
}
