package com.vertexai.gui;

import java.lang.reflect.Method;
import net.minecraft.client.input.MouseButtonEvent;

public class TestSignatures2 {
    public static void printMethods() {
        System.out.println("--- MouseButtonEvent ---");
        for (Method m : MouseButtonEvent.class.getMethods()) {
            System.out.println("METHOD: " + m.getName() + " RETURNS: " + m.getReturnType().getName());
        }
    }
}
