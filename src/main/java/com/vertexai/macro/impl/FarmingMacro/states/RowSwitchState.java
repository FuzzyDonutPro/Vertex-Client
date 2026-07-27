package com.vertexai.macro.impl.FarmingMacro.states;

import com.vertexai.Vertex;
import com.vertexai.macro.impl.FarmingMacro.FarmingMacro;
import com.vertexai.macro.impl.FarmingMacro.FarmingMacroState;
import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.helper.Clock;
import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.helper.Clock;
import com.vertexai.macro.impl.FarmingMacro.util.CropEnum;
import net.minecraft.client.Minecraft;

public class RowSwitchState implements FarmingMacroState {

    private final boolean nextDirectionLeft;
    private final CropEnum crop;
    private final Clock shiftTimer = new Clock();

    public RowSwitchState(boolean nextDirectionLeft, CropEnum crop) {
        this.nextDirectionLeft = nextDirectionLeft;
        this.crop = crop;
    }

    @Override
    public void onStart(FarmingMacro macro) {
        int shiftDuration = Vertex.config().farming.laneShiftTime;
        shiftTimer.schedule(shiftDuration);
    }

    @Override
    public FarmingMacroState onTick(FarmingMacro macro) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;

        // Keep swinging while turning
        KeyBindUtil.setKeyBindState(mc.options.keyAttack, true);

        // Hold the lane shift key (Default: W/Forward to advance to next row)
        KeyBindUtil.setKeyBindState(mc.options.keyUp, true);

        if (shiftTimer.passed()) {
            return new FarmingState(nextDirectionLeft, crop);
        }

        return this;
    }

    @Override
    public void onEnd(FarmingMacro macro) {
        Minecraft mc = Minecraft.getInstance();
        KeyBindUtil.setKeyBindState(mc.options.keyUp, false);
        KeyBindUtil.setKeyBindState(mc.options.keyDown, false);
    }
}
