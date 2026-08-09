package com.vertexai.macro.features.misc.AutoGetStats.tasks.impl;

import com.vertexai.Vertex;
import com.vertexai.macro.features.misc.AutoGetStats.tasks.AbstractInventoryTask;
import com.vertexai.macro.features.misc.AutoGetStats.tasks.TaskStatus;
import com.vertexai.util.TablistUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A task that retrieves the player's total SkyBlock Mining Speed, taking into account
 * custom config overrides, TabList stats, held tool Efficiency, and item lore.
 */
public class MiningSpeedRetrievalTask extends AbstractInventoryTask<Integer> {

    private static final Pattern SPEED_PATTERN = Pattern.compile("Mining Speed:\\s*\\+?([\\d,]+)");
    private final Minecraft mc = Minecraft.getInstance();
    private Integer miningSpeed;

    @Override
    public void init() {
        taskStatus = TaskStatus.SUCCESS;
        miningSpeed = detectMiningSpeed();
    }

    @Override
    public void onTick() {
        taskStatus = TaskStatus.SUCCESS;
        if (miningSpeed == null || miningSpeed <= 0) {
            miningSpeed = detectMiningSpeed();
        }
    }

    @Override
    public void end() {
    }

    @Override
    public Integer getResult() {
        return miningSpeed != null && miningSpeed > 0 ? miningSpeed : detectMiningSpeed();
    }

    public static int detectMiningSpeed() {
        // 1. Config override check
        if (Vertex.config() != null && Vertex.config().miningMacro != null && Vertex.config().miningMacro.customMiningSpeed > 0) {
            return Vertex.config().miningMacro.customMiningSpeed;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 2500;

        // 2. Parse TabList for "Mining Speed: 2,450"
        try {
            List<String> tabList = TablistUtil.getCachedTablist();
            for (String line : tabList) {
                if (line == null) continue;
                String clean = ChatFormatting.stripFormatting(line);
                Matcher matcher = SPEED_PATTERN.matcher(clean);
                if (matcher.find()) {
                    int val = Integer.parseInt(matcher.group(1).replace(",", ""));
                    if (val > 0) return val;
                }
            }
        } catch (Exception ignored) {}

        // 3. Parse held tool lore & Efficiency enchantment
        int itemSpeed = 0;
        try {
            ItemStack held = mc.player.getMainHandItem();
            if (!held.isEmpty()) {
                // Check Efficiency enchantment
                int effLevel = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                        mc.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.EFFICIENCY), held);
                if (effLevel > 0) {
                    int effSpeed = effLevel * 20;
                    if (effLevel >= 6) effSpeed += (effLevel - 5) * 10;
                    itemSpeed += effSpeed;
                }

                // Check item lore
                ItemLore lore = held.get(DataComponents.LORE);
                if (lore != null) {
                    for (Component line : lore.lines()) {
                        String str = ChatFormatting.stripFormatting(line.getString());
                        Matcher matcher = SPEED_PATTERN.matcher(str);
                        if (matcher.find()) {
                            int val = Integer.parseInt(matcher.group(1).replace(",", ""));
                            itemSpeed += val;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        if (itemSpeed > 0) {
            return Math.max(1000, itemSpeed);
        }

        return 2500;
    }
}
