package com.vertexai.feature.impl.SlayerQoL;

import lombok.Getter;
import com.vertexai.Vertex;
import com.vertexai.feature.AbstractFeature;
import com.vertexai.util.ChatPacketUtil;
import com.vertexai.util.InventoryUtil;
import com.vertexai.util.UseItemAbility;
import com.vertexai.util.helper.Clock;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * SlayerQoL — Automatically interacts with the Maddox Batphone or Abiphone to:
 * 1. Click the dark green [OPEN MENU] chat component if prompted.
 * 2. Claim pending Slayer rewards as soon as a boss is killed.
 * 3. Select the configured Slayer boss and tier (Tier 1-5).
 * 4. Auto-confirm and start the new Slayer quest.
 */
public class SlayerQoL extends AbstractFeature {

    private static final SlayerQoL instance = new SlayerQoL();
    public static SlayerQoL getInstance() { return instance; }

    private final Clock delayClock = new Clock();
    private boolean awaitingClaim = false;
    private boolean awaitingBossSelect = false;
    private boolean awaitingTierSelect = false;
    private boolean awaitingConfirm = false;
    private long lastCallTime = 0;

    public SlayerQoL() {
        this.enabled = true;
    }

    @Override
    public String getName() {
        return "Slayer QoL";
    }

    @Override
    public boolean shouldStartAtLaunch() {
        return true;
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public void stop() {
        // Persistent background feature
        this.enabled = true;
    }

    @Override
    public void onPacketReceive(Packet<?> packet) {
        if (mc.player == null) return;

        // Auto-click [OPEN MENU] chat link from Maddox call packets
        if (packet instanceof ClientboundSystemChatPacket systemChat) {
            Component content = systemChat.content();
            String raw = content.getString();
            String lower = ChatFormatting.stripFormatting(raw).toLowerCase();

            if (lower.contains("[open menu]") || lower.contains("open menu") || 
               (lower.contains("calling maddox") && lower.contains("menu")) ||
               (lower.contains("maddox") && lower.contains("phone"))) {

                net.minecraft.network.chat.Style style = ChatPacketUtil.findStyleWithClickEvent(content, "open menu");
                if (style == null) {
                    style = ChatPacketUtil.findStyleWithClickEvent(content, null);
                }

                if (style != null) {
                    log("SlayerQoL: Auto-clicking [OPEN MENU] chat link...");
                    ChatPacketUtil.executeStyleClick(style);
                    delayClock.schedule(600L);
                } else if (mc.screen == null && (System.currentTimeMillis() - lastCallTime < 5000)) {
                    // Fallback command if ClickEvent object not found
                    log("SlayerQoL: Fallback sending /cb maddox...");
                    mc.player.connection.sendCommand("cb maddox");
                    delayClock.schedule(600L);
                }
            }
        }
    }

    @Override
    public void onChat(String message) {
        if (mc.player == null || message == null) return;
        String clean = ChatFormatting.stripFormatting(message).trim();

        // 1. Detect Slayer Boss Slain or Quest Complete
        if (clean.contains("SLAYER QUEST COMPLETE!") || 
            clean.contains("Talk to Maddox to claim your reward!") ||
            clean.contains("Boss slain in") ||
            clean.contains("NICE! SLAYER BOSS SLAIN!")) {
            
            log("Slayer Boss Slain detected in chat! Auto-opening Maddox Batphone...");
            this.awaitingClaim = true;
            this.awaitingBossSelect = true;
            this.awaitingTierSelect = true;
            this.awaitingConfirm = true;
            openMaddoxMenu();
        } 
        // 2. Detect failed quest
        else if (clean.contains("SLAYER QUEST FAILED") || clean.contains("Slayer Quest Failed!")) {
            log("Slayer Quest Failed detected. Auto-restarting quest via Maddox Batphone...");
            this.awaitingClaim = false;
            this.awaitingBossSelect = true;
            this.awaitingTierSelect = true;
            this.awaitingConfirm = true;
            openMaddoxMenu();
        }
        // 3. Fallback text match for [OPEN MENU]
        else if ((clean.contains("[OPEN MENU]") || clean.contains("OPEN MENU")) && mc.screen == null) {
            if (System.currentTimeMillis() - lastCallTime < 5000) {
                log("SlayerQoL: Chat contained [OPEN MENU], ensuring GUI opens...");
                mc.player.connection.sendCommand("cb maddox");
                delayClock.schedule(600L);
            }
        }
    }

    public void onBossSlain() {
        log("SlayerQoL: Manual trigger onBossSlain, opening Maddox Batphone...");
        this.awaitingClaim = true;
        this.awaitingBossSelect = true;
        this.awaitingTierSelect = true;
        this.awaitingConfirm = true;
        openMaddoxMenu();
    }

    public void startQuestIfNeeded() {
        log("SlayerQoL: Requesting Slayer quest start via Maddox Batphone...");
        this.awaitingClaim = true;
        this.awaitingBossSelect = true;
        this.awaitingTierSelect = true;
        this.awaitingConfirm = true;
        openMaddoxMenu();
    }

    public void openMaddoxMenu() {
        if (mc.player == null) return;
        this.lastCallTime = System.currentTimeMillis();

        // Ensure Batphone / Abiphone is ready in hotbar
        int abiphone = InventoryUtil.getHotbarSlotOfItem("Abiphone");
        int batphone = InventoryUtil.getHotbarSlotOfItem("Maddox Batphone");

        if (batphone != -1) {
            UseItemAbility.useItemAbility("Maddox Batphone", batphone);
        } else if (abiphone != -1) {
            UseItemAbility.useItemAbility("Abiphone", abiphone);
        } else {
            // Check main inventory (slots 9-35)
            int invBatphone = InventoryUtil.getSlotOfItemInMainInventory("Maddox Batphone");
            int invAbiphone = InventoryUtil.getSlotOfItemInMainInventory("Abiphone");

            if (invBatphone != -1 && mc.gameMode != null) {
                mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId, invBatphone, 8, net.minecraft.world.inventory.ContainerInput.SWAP, mc.player);
                UseItemAbility.useItemAbility("Maddox Batphone", 8);
            } else if (invAbiphone != -1 && mc.gameMode != null) {
                mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId, invAbiphone, 8, net.minecraft.world.inventory.ContainerInput.SWAP, mc.player);
                UseItemAbility.useItemAbility("Abiphone", 8);
            } else {
                // Fallback to /slayer command
                mc.player.connection.sendCommand("slayer");
            }
        }
        delayClock.schedule(700L);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;
        if (delayClock.isScheduled() && !delayClock.passed()) return;

        // If the GUI is open (regardless of whether it opened from direct right-click or chat click), process it seamlessly!
        if (mc.screen instanceof AbstractContainerScreen<?> containerScreen) {
            String title = ChatFormatting.stripFormatting(containerScreen.getTitle().getString()).toLowerCase();
            var menu = containerScreen.getMenu();

            // 1. Claim Reward (if present)
            if (awaitingClaim) {
                for (Slot slot : menu.slots) {
                    if (slot.hasItem()) {
                        ItemStack stack = slot.getItem();
                        String name = ChatFormatting.stripFormatting(stack.getHoverName().getString()).toLowerCase();
                        String tooltip = stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.EMPTY, mc.player, net.minecraft.world.item.TooltipFlag.Default.NORMAL).toString().toLowerCase();

                        if (name.contains("claim") || name.contains("reward") || tooltip.contains("reward ready") || tooltip.contains("click to claim")) {
                            log("SlayerQoL: Clicking claim reward in slot " + slot.index);
                            clickSlot(menu.containerId, slot.index);
                            awaitingClaim = false;
                            delayClock.schedule(600L);
                            return;
                        }
                    }
                }
                // If no claim button found, proceed to boss select
                awaitingClaim = false;
            }

            // 2. Select Boss Category in main Maddox / Slayer overview menu
            if (awaitingBossSelect && (title.contains("maddox") || title.contains("slayer"))) {
                String targetBossKeyword = getTargetBossKeyword();

                for (Slot slot : menu.slots) {
                    if (slot.hasItem()) {
                        ItemStack stack = slot.getItem();
                        String name = ChatFormatting.stripFormatting(stack.getHoverName().getString()).toLowerCase();
                        String tooltip = stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.EMPTY, mc.player, net.minecraft.world.item.TooltipFlag.Default.NORMAL).toString().toLowerCase();

                        if (name.contains(targetBossKeyword) || tooltip.contains(targetBossKeyword)) {
                            log("SlayerQoL: Selecting boss (" + targetBossKeyword + ") in slot " + slot.index);
                            clickSlot(menu.containerId, slot.index);
                            awaitingBossSelect = false;
                            delayClock.schedule(600L);
                            return;
                        }
                    }
                }
            }

            // 3. Select Configured Tier (Tier 1 to 5 / Tier I to V)
            if (awaitingTierSelect && (title.contains("revenant") || title.contains("tarantula") || title.contains("sven") || title.contains("voidgloom") || title.contains("slayer") || title.contains("horror") || title.contains("broodfather") || title.contains("packmaster") || title.contains("seraph"))) {
                int configuredTier = Vertex.config().combat.getSlayerTier();
                String romanTier = Vertex.config().combat.getSlayerTierRoman(); // "I", "II", "III", "IV", "V"

                for (Slot slot : menu.slots) {
                    if (slot.hasItem()) {
                        ItemStack stack = slot.getItem();
                        String name = ChatFormatting.stripFormatting(stack.getHoverName().getString()).toLowerCase();
                        String tooltip = stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.EMPTY, mc.player, net.minecraft.world.item.TooltipFlag.Default.NORMAL).toString().toLowerCase();

                        boolean matchesTier = name.contains("tier " + romanTier.toLowerCase()) || 
                                              name.contains("tier " + configuredTier) || 
                                              tooltip.contains("tier " + romanTier.toLowerCase()) ||
                                              tooltip.contains("tier " + configuredTier);

                        if (matchesTier) {
                            log("SlayerQoL: Selecting Tier " + romanTier + " (Tier " + configuredTier + ") in slot " + slot.index);
                            clickSlot(menu.containerId, slot.index);
                            awaitingTierSelect = false;
                            delayClock.schedule(600L);
                            return;
                        }
                    }
                }
            }

            // 4. Confirm / Start Quest
            if (awaitingConfirm || title.contains("confirm")) {
                for (Slot slot : menu.slots) {
                    if (slot.hasItem()) {
                        ItemStack stack = slot.getItem();
                        String name = ChatFormatting.stripFormatting(stack.getHoverName().getString()).toLowerCase();
                        String tooltip = stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.EMPTY, mc.player, net.minecraft.world.item.TooltipFlag.Default.NORMAL).toString().toLowerCase();

                        if (name.contains("confirm") || name.contains("start") || tooltip.contains("click to start") || tooltip.contains("start quest")) {
                            log("SlayerQoL: Confirming quest in slot " + slot.index);
                            clickSlot(menu.containerId, slot.index);
                            awaitingConfirm = false;
                            awaitingBossSelect = false;
                            awaitingTierSelect = false;
                            mc.player.closeContainer();
                            delayClock.schedule(400L);
                            return;
                        }
                    }
                }
            }
        }
    }

    private void clickSlot(int containerId, int slotIndex) {
        if (mc.gameMode != null && mc.player != null) {
            mc.gameMode.handleContainerInput(containerId, slotIndex, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, mc.player);
        }
    }

    private String getTargetBossKeyword() {
        return switch (Vertex.config().combat.slayerTarget) {
            case 0 -> "revenant";
            case 1 -> "tarantula";
            case 2 -> "sven";
            case 3 -> "voidgloom";
            default -> "revenant";
        };
    }
}
