package com.vertexai.util;

import net.minecraft.client.gui.GuiGraphics;
import java.lang.reflect.Method;
import java.util.Arrays;

public class CheckGuiGraphics {
    public static void printMethods() {
        for (Method m : GuiGraphics.class.getDeclaredMethods()) {
            System.out.println(m.getName() + " -> " + m.getReturnType().getName());
        }
    }
}
