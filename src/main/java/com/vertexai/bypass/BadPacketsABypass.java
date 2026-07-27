package com.vertexai.bypass;

import net.minecraft.client.Minecraft;

/**
 * BadPacketsA Bypass — prevents sending duplicate HeldItemChange packets.
 * Grim flags if the same slot ID is sent twice in a row.
 * Call canSendSlotChange(newSlot) before switching slots; it returns false if it's a duplicate.
 */
public class BadPacketsABypass {

    private static final Minecraft mc = Minecraft.getInstance();
    private static int lastSentSlot = -1;

    /**
     * Returns true if it is safe to send a slot change to the given slot.
     * Also updates the internal last slot tracker.
     */
    public static boolean canSendSlotChange(int newSlot) {
        if (newSlot == lastSentSlot) {
            return false; // Duplicate — would trigger BadPacketsA
        }
        lastSentSlot = newSlot;
        return true;
    }

    /**
     * Sync the tracker with the player's current selected slot (call on login / world change).
     */
    public static void sync() {
        if (mc.player != null) {
            lastSentSlot = mc.player.getInventory().getSelectedSlot();
        }
    }

    public static int getLastSentSlot() {
        return lastSentSlot;
    }
}
