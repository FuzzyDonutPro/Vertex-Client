package com.vertexai.util;

import com.vertexai.Vertex;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class NickHiderUtil {

    private static final String DEFAULT_NICK = "Vertex User";

    public static String getTargetNick() {
        if (Vertex.config() != null && Vertex.config().misc != null) {
            String nick = Vertex.config().misc.nickHiderName;
            if (nick != null && !nick.trim().isEmpty()) {
                return nick;
            }
        }
        return DEFAULT_NICK;
    }

    public static String replaceName(String text) {
        if (text == null) return null;
        if (Vertex.config() != null && Vertex.config().misc.enableNickHider) {
            String name = Minecraft.getInstance().getUser().getName();
            String nick = getTargetNick();
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
            String nick = getTargetNick();
            if (name != null && !name.isEmpty() && !name.equals(nick)) {
                return replaceInComponent(comp, name, nick);
            }
        }
        return comp;
    }

    private static Component replaceInComponent(Component comp, String name, String nick) {
        net.minecraft.network.chat.MutableComponent newComp = comp.plainCopy();
        
        // Handle LiteralContents
        if (comp.getContents() instanceof net.minecraft.network.chat.contents.PlainTextContents literal) {
            newComp = net.minecraft.network.chat.Component.literal(literal.text().replace(name, nick));
        } 
        // Handle TranslatableContents
        else if (comp.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translatable) {
            Object[] args = translatable.getArgs();
            Object[] newArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Component) {
                    newArgs[i] = replaceInComponent((Component) args[i], name, nick);
                } else if (args[i] instanceof String) {
                    newArgs[i] = ((String) args[i]).replace(name, nick);
                } else {
                    newArgs[i] = args[i];
                }
            }
            newComp = net.minecraft.network.chat.Component.translatable(translatable.getKey(), newArgs);
        }

        // Keep the original style
        newComp.setStyle(comp.getStyle());

        // Process all siblings recursively
        for (Component sibling : comp.getSiblings()) {
            newComp.append(replaceInComponent(sibling, name, nick));
        }

        return newComp;
    }
}
