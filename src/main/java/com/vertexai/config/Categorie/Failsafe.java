package com.vertexai.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class Failsafe {

    @ConfigOption(
            name = "Enable Failsafe Trigger Sound",
            desc = "Makes a sound when a failsafe has been triggered"
    )
    @ConfigEditorBoolean
    public boolean enableFailsafeSound = true;

    @ConfigOption(
            name = "Time to wait before toggling failsafe (in ms)",
            desc = ""
    )
    @ConfigEditorSlider(minValue = 0, maxValue = 15000, minStep = 100)
    public int failsafeToggleDelay = 3000;

    @ConfigOption(
            name = "Failsafe Reaction",
            desc = "The action to take when a failsafe is triggered"
    )
    @ConfigEditorDropdown(values = {"Disable Macro", "Disconnect", "Warp Hub", "Warp Island", "Play Recording"})
    public int failsafeReaction = 0;

    @ConfigOption(name = "Vertical Knockback Threshold", desc = "")
    @ConfigEditorSlider(minValue = 3000, maxValue = 10000, minStep = 100)
    public int verticalKnockbackThreshold = 4000;

    @ConfigOption(
            name = "Enable Rotation Failsafe",
            desc = "Triggers a failsafe when your player rotation is suddenly changed by a server check or admin"
    )
    @ConfigEditorBoolean
    public boolean enableRotationFailsafe = true;

    @ConfigOption(
            name = "Rotation Threshold (degrees)",
            desc = "Minimum rotation difference in degrees to trigger the rotation failsafe"
    )
    @ConfigEditorSlider(minValue = 1, maxValue = 90, minStep = 1)
    public int rotationThreshold = 10;

    @ConfigOption(
            name = "Enable Flag / Rubberband Failsafe",
            desc = "Triggers a failsafe if repeated movement lagbacks or rubberbands occur in a short window"
    )
    @ConfigEditorBoolean
    public boolean enableFlagFailsafe = true;

    @ConfigOption(
            name = "Max Rubberbands Before Failsafe",
            desc = "Number of rubberbands required within the time window to trigger the failsafe"
    )
    @ConfigEditorSlider(minValue = 2, maxValue = 10, minStep = 1)
    public int flagThreshold = 3;

    @ConfigOption(
            name = "Flag Time Window (seconds)",
            desc = "Time window in seconds to count rubberbands"
    )
    @ConfigEditorSlider(minValue = 1, maxValue = 15, minStep = 1)
    public int flagTimeWindow = 5;

    @ConfigOption(
            name = "Name Mention Failsafe Behaviour",
            desc = "The action Name Mention Failsafe will take when your name is mentioned in chat"
    )
    @ConfigEditorDropdown(values = {"Pause Macro", "Change Lobby"})
    public int nameMentionFailsafeBehaviour = 0;

    @ConfigOption(
            name = "Failsafe Sound Type",
            desc = "The failsafe sound type to play when a failsafe has been triggered"
    )
    @ConfigEditorDropdown(values = {"Minecraft", "Custom"})
    public int failsafeSoundType = 0;

    @ConfigOption(
            name = "Minecraft Sound",
            desc = "The Minecraft sound to play when a failsafe has been triggered"
    )
    @ConfigEditorDropdown(values = {"Ping", "Anvil"})
    public int failsafeMcSoundSelected = 1;

    @ConfigOption(
            name = "Custom Sound",
            desc = "The custom sound to play when a failsafe has been triggered"
    )
    @ConfigEditorDropdown(
            values = {
                    "Custom", "Voice", "Metal Pipe", "AAAAAAAAAA", "Loud Buzz",
            }
    )
    public int failsafeSoundSelected = 1;

    @ConfigOption(
            name = "Number of times to play custom sound",
            desc = "The number of times to play custom sound when a failsafe has been triggered"
    )
    @ConfigEditorSlider(minValue = 1, maxValue = 10, minStep = 1)
    public int failsafeSoundTimes = 10;

    @ConfigOption(
            name = "Failsafe Sound Volume (in %)",
            desc = "The volume of the failsafe sound"
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f)
    public float failsafeSoundVolume = 50.0f;

    @ConfigOption(
            name = "Max out Minecraft sounds while pinging",
            desc = "Maxes out the sounds while failsafe"
    )
    @ConfigEditorBoolean
    public boolean maxOutMinecraftSounds = false;

    @ConfigOption(
            name = "Enable Crop Change Failsafe",
            desc = "Triggers a failsafe reaction if the crop type unexpectedly changes while farming"
    )
    @ConfigEditorBoolean
    public boolean enableCropChangeFailsafe = true;

    @ConfigOption(
            name = "Crop Change Reaction Preset",
            desc = "Recorded reaction preset to play when crop type changes unexpectedly"
    )
    public String cropChangeReactionName = "default";

    @ConfigOption(
            name = "Crop Change Reaction Delay (ms)",
            desc = "Delay in milliseconds before triggering reaction when crop change is detected (default: 2000ms)"
    )
    @ConfigEditorSlider(minValue = 0, maxValue = 10000, minStep = 250)
    public int cropChangeReactionDelay = 2000;

    @ConfigOption(
            name = "Enable Admin / Staff Detector",
            desc = "Automatically evacuates lobby (/l) if a Hypixel Admin or Staff member is detected"
    )
    @ConfigEditorBoolean
    public boolean enableStaffDetector = true;

    @ConfigOption(
            name = "Enable Player Stare Failsafe",
            desc = "Triggers a failsafe if another player stands near and stares directly at you"
    )
    @ConfigEditorBoolean
    public boolean enablePlayerStareFailsafe = true;

    @ConfigOption(
            name = "Player Stare Threshold (ms)",
            desc = "Duration in milliseconds a player must stare at you to trigger a failsafe"
    )
    @ConfigEditorSlider(minValue = 500, maxValue = 5000, minStep = 250)
    public int playerStareThresholdMs = 1000;

    @ConfigOption(
            name = "Manual Failsafe Stop Keybind",
            desc = "Pressing this key (Default: G) immediately stops active failsafe alerts and reaction playbacks"
    )
    @io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind(defaultKey = org.lwjgl.glfw.GLFW.GLFW_KEY_G)
    public int failsafeStopKeybind = org.lwjgl.glfw.GLFW.GLFW_KEY_G;
}
