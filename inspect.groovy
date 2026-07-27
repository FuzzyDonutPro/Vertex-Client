import net.minecraft.client.gui.GuiGraphics
import java.lang.reflect.Method

for (Method m : GuiGraphics.class.getDeclaredMethods()) {
    println(m.getName() + " -> " + m.getReturnType().getName())
}
