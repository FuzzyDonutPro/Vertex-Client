package com.vertexai.config;

import com.vertexai.Vertex;
import com.vertexai.VertexClient;
import com.vertexai.ui.screen.HUDEditorScreen;
import com.vertexai.util.Logger;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

public class ConfigActions {

    public static void openHudEditor() {
        Minecraft client = Minecraft.getInstance();
        Screen parent = client.screen;
        client.setScreen(new HUDEditorScreen(parent));
    }

    public static void toggleMacro() {
        com.vertexai.macro.MacroManager.getInstance().toggle();
        Minecraft.getInstance().setScreen(null); // Close GUI after clicking
    }

    public static void setMiningTool() {
        handleItemSet(
                name -> Vertex.config().general.miningTool = name,
                "Mining Tool",
                false,
                "Config GUI"
        );
    }

    public static void setMiningToolCommand() {
        handleItemSet(
                name -> Vertex.config().general.miningTool = name,
                "Mining Tool",
                false,
                "Command /vf set mining-tool"
        );
    }

    public static void setAltMiningTool() {
        handleItemSet(
                name -> Vertex.config().commission.dwarvenCommission.altMiningTool = name,
                "Alternative Mining Tool",
                false,
                "Config GUI"
        );
    }

    public static void setAltMiningToolCommand() {
        handleItemSet(
                name -> Vertex.config().commission.dwarvenCommission.altMiningTool = name,
                "Alternative Mining Tool",
                false,
                "Command /vf set alt-mining-tool"
        );
    }

    public static void setSlayerWeapon() {
        handleItemSet(
                name -> Vertex.config().commission.dwarvenCommission.slayerWeapon = name,
                "Slayer Weapon (Commission)",
                true,
                "Config GUI"
        );
    }

    public static void setGeneralSlayerWeapon() {
        handleItemSet(
                name -> Vertex.config().general.slayerWeapon = name,
                "Slayer Weapon (General)",
                true,
                "Config GUI"
        );
    }

    public static void setSlayerWeaponCommand() {
        handleItemSet(
                name -> Vertex.config().commission.dwarvenCommission.slayerWeapon = name,
                "Slayer Weapon",
                true,
                "Command /vf set slayer-weapon"
        );
    }

    public static void setFishingRod() {
        handleItemSet(
                name -> Vertex.config().fishing.generalFishing.fishingRod = name,
                "Fishing Rod",
                false,
                "Config GUI"
        );
    }

    public static void setFishingRodCommand() {
        handleItemSet(
                name -> Vertex.config().fishing.generalFishing.fishingRod = name,
                "Fishing Rod",
                false,
                "Command /vf set fishing-rod"
        );
    }

    public static void setGalateaAxe() {
        handleItemSet(
                name -> Vertex.config().fishing.galateaFishing.galateaAxe = name,
                "Galatea Axe",
                false,
                "Config GUI"
        );
    }

    public static void setGalateaAxeCommand() {
        handleItemSet(
                name -> Vertex.config().fishing.galateaFishing.galateaAxe = name,
                "Galatea Axe",
                false,
                "Command /vf set fishing-axe"
        );
    }

    public static void setGalateaFishingWeapon() {
        handleItemSet(
                name -> Vertex.config().fishing.galateaFishing.galateaFishingWeapon = name,
                "Galatea Secondary Weapon",
                false,
                "Config GUI"
        );
    }

    public static void setGalateaFishingWeaponCommand() {
        handleItemSet(
                name -> Vertex.config().fishing.galateaFishing.galateaFishingWeapon = name,
                "Galatea Secondary Weapon",
                false,
                "Command /vf set fishing-weapon"
        );
    }

    private static void handleItemSet(
            java.util.function.Consumer<String> setter,
            String toolName,
            boolean strictSanitize,
            String source
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack currentItem = mc.player.getMainHandItem();

        if (currentItem.isEmpty()) {
            Logger.sendMessage("Don't hold an empty hand.");
            return;
        }

        String strippedName = ChatFormatting.stripFormatting(
                currentItem.getHoverName().getString()
        );
        if (strictSanitize) {
            strippedName = strippedName.replaceAll("[^\\x20-\\x7E]", "");
        }

        setter.accept(strippedName);
        VertexClient.configManager.saveConfig();
        Logger.sendMessage(
                toolName + " set to: " +
                        currentItem.getHoverName().getString() +
                        " (via " + source + ", saved config/Vertex/Vertex.json)"
        );
    }
}
