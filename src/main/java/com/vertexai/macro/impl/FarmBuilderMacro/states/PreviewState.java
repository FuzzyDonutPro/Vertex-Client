package com.vertexai.macro.impl.FarmBuilderMacro.states;

import com.mojang.blaze3d.platform.InputConstants;
import com.vertexai.macro.AbstractMacro;
import com.vertexai.util.Logger;
import com.vertexai.util.RenderUtil;
import com.vertexai.util.WorldRenderContextWrapper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;

public class PreviewState extends FarmBuilderState {

    private boolean xWasPressed = false;

    public PreviewState(AbstractMacro macro) {
        super(macro);
    }

    @Override
    public String getName() {
        return "Preview";
    }

    @Override
    public void onEnable() {
        Logger.sendMessage("§b[FarmBuilder] Preview Mode active. Move into position and press 'X' to lock the hologram and start building.");
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        
        boolean xIsPressed = com.vertexai.util.KeyPressUtil.wasPressed(mc.getWindow(), GLFW.GLFW_KEY_X, true);
        
        if (xIsPressed) {
            Logger.sendMessage("§a[FarmBuilder] Hologram locked! Starting build sequence.");
            macro.getStateMachine().transitionTo(new InitState(macro));
        }
    }

    @Override
    public void onWorldRender(AbstractMacro macro, WorldRenderContextWrapper context) {
        if (mc.player == null) return;

        BlockPos pos = mc.player.blockPosition();
        
        // 96x96 plot, 7 blocks tall outline
        AABB box = new AABB(
            pos.getX(), pos.getY(), pos.getZ(),
            pos.getX() + 96, pos.getY() + 7, pos.getZ() + 96
        );

        // Draw a light green semi-transparent outline
        RenderUtil.drawAABB(box, new Color(0, 255, 0, 80), false);
        // Draw a faint filled box to make the volume visible
        RenderUtil.drawAABB(box, new Color(0, 255, 0, 20), true);
    }

    @Override
    public void onOverlayRender(AbstractMacro macro, GuiGraphicsExtractor graphics) {
        RenderUtil.drawCenterTopText(graphics, "Press X to set the hologram to the right spot", new Color(255, 255, 0, 255));
    }
}
