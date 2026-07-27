package com.vertexai.bypass;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

/**
 * SprintBypass — evaluates all Grim AntiCheat sprint checks before allowing sprint.
 *
 * Covers:
 *  - SprintA: hunger <= 6
 *  - SprintB: sneaking / slow movement
 *  - SprintC: using item (eating, blocking)
 *  - SprintD: blindness effect active
 *  - SprintE: hard horizontal wall collision
 *  - SprintG: touching water but not swimming (unless Depth Strider 3)
 */
public class SprintBypass {

    private static final Minecraft mc = Minecraft.getInstance();

    /**
     * Returns true if sprinting is safe to hold according to Grim's sprint checks.
     */
    public static boolean canSprint() {
        LocalPlayer player = mc.player;
        if (player == null) return false;

        // SprintA — hunger must be above 6
        if (player.getFoodData().getFoodLevel() <= 6) return false;

        // SprintD — cannot start sprinting with blindness
        if (player.hasEffect(MobEffects.BLINDNESS)) return false;

        // SprintB — cannot sprint while sneaking / slow movement
        if (player.isShiftKeyDown()) return false;

        // SprintC — cannot sprint while using an item (eating, blocking, etc.)
        if (player.isUsingItem()) return false;

        // SprintE — cannot sprint while having a hard horizontal wall collision
        if (player.horizontalCollision && !player.verticalCollision) return false;

        // SprintG — cannot sprint while in water but not swimming and not eye in water,
        // UNLESS the player has Depth Strider 3 on their boots.
        if (player.isInWater() && !player.isSwimming() && !player.isEyeInFluid(FluidTags.WATER)) {
            if (!hasDepthStrider3(player)) return false;
        }

        return true;
    }

    /**
     * Returns true if the player's boots have Depth Strider level >= 3.
     * Uses EquipmentSlot and EnchantmentHelper registry lookup (MC 1.21.1).
     */
    private static boolean hasDepthStrider3(LocalPlayer player) {
        ItemStack boots = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET);
        if (boots.isEmpty()) return false;
        if (mc.level == null) return false;

        try {
            var registry = mc.level.registryAccess()
                .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
            var holderOpt = registry.get(net.minecraft.world.item.enchantment.Enchantments.DEPTH_STRIDER);
            if (holderOpt.isEmpty()) return false;
            return net.minecraft.world.item.enchantment.EnchantmentHelper
                .getItemEnchantmentLevel(holderOpt.get(), boots) >= 3;
        } catch (Exception e) {
            return false;
        }
    }
}
