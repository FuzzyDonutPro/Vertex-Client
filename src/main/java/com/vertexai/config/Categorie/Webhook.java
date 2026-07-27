package com.vertexai.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class Webhook {

    @ConfigOption(
            name = "Discord Webhook URL",
            desc = "The URL of the Discord Webhook to send notifications to"
    )
    @ConfigEditorText
    public String webhookUrl = "";

    @ConfigOption(
            name = "Discord User ID",
            desc = "Your Discord User ID so the webhook can @mention you"
    )
    @ConfigEditorText
    public String discordId = "";

    @Category(name = "General Alerts", desc = "General webhook alert settings")
    public GeneralAlerts generalAlerts = new GeneralAlerts();

    @Category(name = "Failsafe Alerts", desc = "Specific failsafe webhook alert settings")
    public FailsafeAlerts failsafeAlerts = new FailsafeAlerts();

    // Backwards compatibility delegate for FailsafeManager
    public boolean pingOnFailsafe = true;

    public static class GeneralAlerts {
        @ConfigOption(
                name = "Ping on Rare Drop",
                desc = "Sends a message to the webhook when a rare drop is obtained"
        )
        @ConfigEditorBoolean
        public boolean pingOnRareDrop = false;

        @ConfigOption(
                name = "Ping on Disconnect",
                desc = "Sends a message to the webhook when you are disconnected from the server"
        )
        @ConfigEditorBoolean
        public boolean pingOnDisconnect = false;
    }

    public static class FailsafeAlerts {
        @ConfigOption(
                name = "Enable Failsafe Webhooks",
                desc = "Master toggle to send Discord webhook alerts when any failsafe triggers"
        )
        @ConfigEditorBoolean
        public boolean enableFailsafeAlerts = true;

        @ConfigOption(
                name = "Ping on Knockback Failsafe",
                desc = "Alert when Knockback Failsafe triggers"
        )
        @ConfigEditorBoolean
        public boolean pingOnKnockback = true;

        @ConfigOption(
                name = "Ping on Flag / Rubberband Failsafe",
                desc = "Alert when Flag / Rubberband Failsafe triggers"
        )
        @ConfigEditorBoolean
        public boolean pingOnFlag = true;

        @ConfigOption(
                name = "Ping on Teleport Failsafe",
                desc = "Alert when Teleport / Movement Failsafe triggers"
        )
        @ConfigEditorBoolean
        public boolean pingOnTeleport = true;

        @ConfigOption(
                name = "Ping on Name Mention Failsafe",
                desc = "Alert when someone mentions your name in chat"
        )
        @ConfigEditorBoolean
        public boolean pingOnNameMention = true;

        @ConfigOption(
                name = "Ping on Player Check Failsafe",
                desc = "Alert when another player or staff checks you"
        )
        @ConfigEditorBoolean
        public boolean pingOnPlayerCheck = true;

        @ConfigOption(
                name = "Ping on Bedrock Check Failsafe",
                desc = "Alert when Bedrock / World barrier check triggers"
        )
        @ConfigEditorBoolean
        public boolean pingOnBedrockCheck = true;

        @ConfigOption(
                name = "Ping on Rotation Failsafe",
                desc = "Alert when a sudden rotation change occurs"
        )
        @ConfigEditorBoolean
        public boolean pingOnRotation = true;

        @ConfigOption(
                name = "Ping on Item Change Failsafe",
                desc = "Alert when item or inventory slot unexpectedly changes"
        )
        @ConfigEditorBoolean
        public boolean pingOnItemChange = true;
    }
}
