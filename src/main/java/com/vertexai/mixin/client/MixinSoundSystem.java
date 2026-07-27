package com.vertexai.mixin.client;

import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Mixin to mute game sounds when macro is running (optional config).
 */
@Mixin(SoundEngine.class)
public class MixinSoundSystem {

    /*
    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
    private void Vertex$muteGameSounds(SoundInstance sound, CallbackInfo ci) {
        var config = Vertex.config();
        if (config != null
                && MacroManager.getInstance().isRunning()
                && config.general.muteGame
                && FailsafeManager.getInstance().emergencyQueue.isEmpty()
                && !FailsafeManager.getInstance().triggeredFailsafe.isPresent()) {
            ci.cancel();
        }
    }
    */
}
