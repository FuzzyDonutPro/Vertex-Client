package com.vertexai.macro.states;

import com.vertexai.macro.FishingMacro;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public interface FishingMacroState {

    void onStart(FishingMacro macro);

    FishingMacroState onTick(FishingMacro macro);

    void onEnd(FishingMacro macro);

    default void log(String message) {
        System.out.println("[" + this.getClass().getSimpleName() + "] " + message);
    }

    default void send(String message) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(Component.literal("§a[" + this.getClass().getSimpleName() + "] " + message), false);
        }
    }
}
