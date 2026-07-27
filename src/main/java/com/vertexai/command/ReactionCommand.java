package com.vertexai.command;

import com.vertexai.failsafe.reaction.RecordedReactionManager;
import com.vertexai.util.Logger;

import java.util.List;

public class ReactionCommand {

    public void main() {
        Logger.sendMessage("§e[Vertex Reactions] Usage: /reaction <record|stop|play|list|delete> <name> [ticks]");
    }

    public void record(String name, int ticks) {
        if (name == null || name.trim().isEmpty()) {
            Logger.sendError("Please provide a name for the reaction recording. Usage: /reaction record <name> [ticks]");
            return;
        }
        RecordedReactionManager.getInstance().startRecording(name.trim(), ticks);
    }

    public void stop() {
        RecordedReactionManager.getInstance().stopRecording();
    }

    public void play(String name) {
        if (name == null || name.trim().isEmpty()) {
            Logger.sendError("Please specify a reaction name to play. Usage: /reaction play <name>");
            return;
        }
        RecordedReactionManager.getInstance().playReaction(name.trim());
    }

    public void list() {
        List<String> names = RecordedReactionManager.getInstance().getSavedReactionNames();
        if (names.isEmpty()) {
            Logger.sendMessage("§e[Vertex Reactions] No recorded reactions found in config/vertex/reactions.");
        } else {
            Logger.sendMessage("§a[Vertex Reactions] Saved Reactions: §e" + String.join(", ", names));
        }
    }

    public void delete(String name) {
        if (name == null || name.trim().isEmpty()) {
            Logger.sendError("Please specify a reaction name to delete. Usage: /reaction delete <name>");
            return;
        }
        RecordedReactionManager.getInstance().deleteReaction(name.trim());
    }
}
