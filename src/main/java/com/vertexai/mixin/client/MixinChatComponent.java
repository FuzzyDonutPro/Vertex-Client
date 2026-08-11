package com.vertexai.mixin.client;

import com.vertexai.util.NickHiderUtil;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatComponent.class)
public class MixinChatComponent {
    
    @ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Component onAddMessage(Component message) {
        return NickHiderUtil.replaceName(message);
    }
}
