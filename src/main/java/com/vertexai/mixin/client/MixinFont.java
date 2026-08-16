package com.vertexai.mixin.client;

import com.vertexai.util.NickHiderUtil;
import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Font.class)
public class MixinFont {
    
    @ModifyVariable(method = "drawInBatch", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String onDrawString(String text) {
        return NickHiderUtil.replaceName(text);
    }
}

