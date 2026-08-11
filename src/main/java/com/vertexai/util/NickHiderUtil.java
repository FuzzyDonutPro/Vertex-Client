package com.vertexai.util;

import com.vertexai.Vertex;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class NickHiderUtil {
    public static String replaceName(String text) {
        if (text == null) return null;
        if (Vertex.config() != null && Vertex.config().misc.enableNickHider) {
            String name = Minecraft.getInstance().getUser().getName();
            String nick = Vertex.config().misc.nickHiderName;
            if (name != null && !name.isEmpty() && !name.equals(nick) && text.contains(name)) {
                return text.replace(name, nick);
            }
        }
        return text;
    }
    
    public static Component replaceName(Component comp) {
        if (comp == null) return null;
        if (Vertex.config() != null && Vertex.config().misc.enableNickHider) {
            String name = Minecraft.getInstance().getUser().getName();
            String nick = Vertex.config().misc.nickHiderName;
            if (name != null && !name.isEmpty() && !name.equals(nick) && comp.getString().contains(name)) {
                return net.minecraft.network.chat.Component.literal(comp.getString().replace(name, nick));
            }
        }
        return comp;
    }
}
