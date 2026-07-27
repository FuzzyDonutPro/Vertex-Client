package com.vertexai.gui;

import net.minecraft.client.gui.screens.Screen;
import java.lang.reflect.Method;

public class TestSignatures {
    public static void printMethods() {
        for (Method m : Screen.class.getMethods()) {
            if (m.getName().toLowerCase().contains("mouse")) {
                System.out.println("METHOD: " + m.getName());
                for (Class<?> p : m.getParameterTypes()) {
                    System.out.println("  PARAM: " + p.getName());
                }
            }
        }
    }
}
