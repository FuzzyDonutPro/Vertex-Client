package com.vertexai.ui.hud.elements;

import lombok.Getter;
import com.vertexai.client.overlay.AbstractHUDElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * InventoryHUD — Renders player inventory (slots 9 to 35) directly on top of the hotbar.
 */
public class InventoryHUD extends AbstractHUDElement {

    private static final InventoryHUD instance = new InventoryHUD();

    private static final int SLOT_SIZE = 18;
    private static final int PADDING = 4;
    private static final int COLS = 9;
    private static final int ROWS = 3;

    private static final int WIDTH = (COLS * SLOT_SIZE) + (PADDING * 2);
    private static final int HEIGHT = (ROWS * SLOT_SIZE) + (PADDING * 2) + 2;

    public InventoryHUD() {
        super();
        this.x = 0;
        this.y = 52; // 52px above bottom screen (top of hotbar)
        this.anchor = 2; // Bottom-Left / Bottom-Center
        this.enabled = false;
    }

    public static InventoryHUD getInstance() {
        return instance;
    }

    @Override
    public int getWidth() {
        return Math.round(WIDTH * scale);
    }

    @Override
    public int getHeight() {
        return Math.round(HEIGHT * scale);
    }

    @Override
    public int getEditorWidth() {
        return getWidth();
    }

    @Override
    public int getEditorHeight() {
        return getHeight();
    }

    @Override
    public void render(GuiGraphics context, float tickDelta) {
        renderInternal(context, false);
    }

    @Override
    public void renderForEditor(GuiGraphics context, float tickDelta) {
        renderInternal(context, true);
    }

    private void renderInternal(GuiGraphics context, boolean example) {
        if (!enabled) return;
        if (!example && (mc.player == null || mc.level == null || !com.vertexai.macro.MacroManager.getInstance().isRunning())) return;

        float ax = getActualX(getWidth());
        float ay = getActualY(getHeight());

        context.pose().pushMatrix();
        context.pose().translate(ax, ay);
        context.pose().scale(scale, scale);

        // 1. Draw plastic panel background & border
        context.fill(0, 0, WIDTH, HEIGHT, 0xD00F172A); // Dark slate backdrop
        context.fill(0, 0, WIDTH, 2, 0xFF3B82F6); // Top blue accent line
        context.fill(0, HEIGHT - 1, WIDTH, HEIGHT, 0xFF1E293B); // Bottom edge shadow

        // 2. Draw slots and items
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int slotIndex = 9 + (r * COLS) + c;
                int slotX = PADDING + (c * SLOT_SIZE);
                int slotY = PADDING + 2 + (r * SLOT_SIZE);

                // Slot cell background
                context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x601E293B);
                context.fill(slotX, slotY, slotX + 16, slotY + 1, 0x40000000);

                ItemStack stack = ItemStack.EMPTY;
                if (!example && mc.player != null) {
                    stack = mc.player.getInventory().getItem(slotIndex);
                } else if (example) {
                    stack = getExampleStack(slotIndex);
                }

                if (!stack.isEmpty()) {
                    context.renderItem(stack, slotX, slotY);
                    context.renderItemDecorations(mc.font, stack, slotX, slotY);
                }
            }
        }

        context.pose().popMatrix();
    }

    private ItemStack getExampleStack(int index) {
        return switch (index) {
            case 9 -> new ItemStack(Items.DIAMOND_PICKAXE);
            case 10 -> new ItemStack(Items.GOLDEN_APPLE, 16);
            case 11 -> new ItemStack(Items.COOKED_BEEF, 64);
            case 12 -> new ItemStack(Items.NETHER_WART, 64);
            case 13 -> new ItemStack(Items.WHEAT, 64);
            case 14 -> new ItemStack(Items.ENDER_PEARL, 16);
            case 15 -> new ItemStack(Items.EXPERIENCE_BOTTLE, 64);
            default -> ItemStack.EMPTY;
        };
    }
}
