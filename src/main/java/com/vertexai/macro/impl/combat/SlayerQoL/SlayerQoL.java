package com.vertexai.macro.impl.combat.SlayerQoL;

import lombok.Getter;
import com.vertexai.macro.AbstractFeature;
import com.vertexai.util.InventoryUtil;
import com.vertexai.util.UseItemAbility;
import com.vertexai.util.helper.Clock;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * SlayerQoL â€” Automatically claims Maddox Slayer rewards and restarts Slayer quests
 * immediately upon slaying a boss. Includes lore-scanning fallback item classification.
 */
public class SlayerQoL extends AbstractFeature {

    private static final SlayerQoL instance = new SlayerQoL();
    public static SlayerQoL getInstance() { return instance; }

    private final Clock delayClock = new Clock();
    private boolean awaitingClaim = false;
    private boolean awaitingRestart = false;

    @Override
    public String getName() {
        return "Slayer QoL";
    }

    public void onBossSlain() {
        if (!isRunning()) return;
        log("SlayerQoL: Slayer Boss Slain detected! Auto-opening Maddox...");
        awaitingClaim = true;
        awaitingRestart = false;
        openMaddoxMenu();
    }

    private void openMaddoxMenu() {
        if (mc.player == null) return;
        int abiphone = InventoryUtil.getHotbarSlotOfItem("Abiphone");
        int batphone = InventoryUtil.getHotbarSlotOfItem("Maddox Batphone");

        if (abiphone != -1) {
            UseItemAbility.useItemAbility("Abiphone", abiphone);
        } else if (batphone != -1) {
            UseItemAbility.useItemAbility("Maddox Batphone", batphone);
        } else {
            mc.player.connection.sendCommand("slayer");
        }
        delayClock.schedule(800);
    }

    @Override
    public void onTick() {
        if (!isRunning() || mc.player == null) return;
        if (delayClock.isScheduled() && !delayClock.passed()) return;

        if (mc.screen instanceof AbstractContainerScreen<?> containerScreen) {
            String title = containerScreen.getTitle().getString().toLowerCase();

            if (title.contains("maddox") || title.contains("slayer")) {
                var menu = containerScreen.getMenu();

                // 1. Auto-Claim Reward
                if (awaitingClaim) {
                    for (Slot slot : menu.slots) {
                        ItemStack stack = slot.getItem();
                        if (!stack.isEmpty()) {
                            String name = stack.getHoverName().getString().toLowerCase();
                            String tooltip = stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.EMPTY, mc.player, net.minecraft.world.item.TooltipFlag.Default.NORMAL).toString().toLowerCase();

                            if (name.contains("claim") || name.contains("reward") || tooltip.contains("reward ready") || tooltip.contains("click to claim")) {
                                log("SlayerQoL: Claiming Slayer reward...");
                                if (mc.gameMode != null) {
                                    mc.gameMode.handleInventoryMouseClick(menu.containerId, slot.index, 0, net.minecraft.world.inventory.ClickType.PICKUP, mc.player);
                                }
                                awaitingClaim = false;
                                awaitingRestart = true;
                                delayClock.schedule(600);
                                return;
                            }
                        }
                    }
                }

                // 2. Auto-Restart Previous Quest
                if (awaitingRestart) {
                    for (Slot slot : menu.slots) {
                        ItemStack stack = slot.getItem();
                        if (!stack.isEmpty()) {
                            String name = stack.getHoverName().getString().toLowerCase();
                            String tooltip = stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.EMPTY, mc.player, net.minecraft.world.item.TooltipFlag.Default.NORMAL).toString().toLowerCase();

                            if (name.contains("start") || name.contains("repeat") || name.contains("slayer") || tooltip.contains("click to start")) {
                                log("SlayerQoL: Restarting Slayer quest...");
                                if (mc.gameMode != null) {
                                    mc.gameMode.handleInventoryMouseClick(menu.containerId, slot.index, 0, net.minecraft.world.inventory.ClickType.PICKUP, mc.player);
                                }
                                awaitingRestart = false;
                                mc.player.closeContainer();
                                delayClock.schedule(400);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean isRunning() {
        return this.enabled && mc.player != null;
    }
}
