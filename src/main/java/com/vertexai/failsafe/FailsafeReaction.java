package com.vertexai.failsafe;

import com.vertexai.Vertex;
import com.vertexai.failsafe.reaction.RecordedReactionManager;
import com.vertexai.macro.MacroManager;
import com.vertexai.util.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

public class FailsafeReaction {

    public static void executeReaction(String failsafeName) {
        Minecraft mc = Minecraft.getInstance();

        // Always disable macro first
        MacroManager.getInstance().disable();

        String normalizedName = failsafeName.toLowerCase().replace(" ", "_");
        List<String> savedReactions = RecordedReactionManager.getInstance().getSavedReactionNames();

        // If a recorded reaction preset matching the specific failsafe name exists, play it!
        if (savedReactions.contains(normalizedName)) {
            Logger.sendWarning("Executing custom recorded reaction for failsafe: §e" + failsafeName);
            RecordedReactionManager.getInstance().playReaction(normalizedName);
            return;
        }

        // Fall back to default configured reaction action if no specific recording exists
        int reactionChoice = Vertex.config().failsafe.failsafeReaction;
        switch (reactionChoice) {
            case 0 -> { // Disable Macro
                Logger.sendWarning("[" + failsafeName + "] Failsafe reaction: Disabled Macro.");
            }
            case 1 -> { // Disconnect
                Logger.sendWarning("[" + failsafeName + "] Failsafe reaction: Disconnecting player!");
                if (mc.player != null && mc.player.connection != null) {
                    mc.player.connection.getConnection().disconnect(Component.literal("§c[Vertex] Failsafe Triggered: " + failsafeName));
                }
            }
            case 2 -> { // Warp Hub
                Logger.sendWarning("[" + failsafeName + "] Failsafe reaction: Warping to Hub!");
                if (mc.player != null && mc.player.connection != null) {
                    mc.player.connection.sendChat("/hub");
                }
            }
            case 3 -> { // Warp Island
                Logger.sendWarning("[" + failsafeName + "] Failsafe reaction: Warping to Island!");
                if (mc.player != null && mc.player.connection != null) {
                    mc.player.connection.sendChat("/is");
                }
            }
            case 4 -> { // Play Recording
                Logger.sendWarning("[" + failsafeName + "] Failsafe reaction: Playing generic recording 'failsafe_default'!");
                RecordedReactionManager.getInstance().playReaction("failsafe_default");
            }
        }
    }
}
