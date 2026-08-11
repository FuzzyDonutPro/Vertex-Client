package com.vertexai.mixin.client;

import com.vertexai.util.NickHiderUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTeam.class)
public class MixinPlayerTeam {
    
    @Inject(method = "formatNameForTeam", at = @At("RETURN"), cancellable = true)
    private static void onFormatNameForTeam(Team team, Component name, CallbackInfoReturnable<Component> cir) {
        Component original = cir.getReturnValue();
        if (original != null) {
            cir.setReturnValue(NickHiderUtil.replaceName(original));
        }
    }
}
