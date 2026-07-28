package com.vertexai.gui.cef;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import com.vertexai.util.Logger;

import java.io.InputStream;

public class VertexCEFBrowser {

    private static VertexCEFBrowser instance;
    private MCEFBrowser browser;
    private final String url = "http://127.0.0.1:" + VertexUIServer.PORT + "/index.html";

    public static synchronized VertexCEFBrowser getInstance() {
        if (instance == null) {
            instance = new VertexCEFBrowser();
            instance.init();
        }
        return instance;
    }

    public void init() {
        if (browser != null) return;
        
        VertexUIServer.start();

        try {
            if (!MCEF.isInitialized()) {
                MCEF.initialize();
            }
            browser = MCEF.createBrowser(url, true);
            if (browser != null) {
                double scale = Minecraft.getInstance().getWindow().getGuiScale();
                int width = (int) (Minecraft.getInstance().getWindow().getGuiScaledWidth() * scale);
                int height = (int) (Minecraft.getInstance().getWindow().getGuiScaledHeight() * scale);
                browser.resize(width, height);
                Logger.sendLog("[VertexCEFBrowser] Initialized MCEF Browser instance at " + url + " with HDPI scale: " + scale);
            } else {
                Logger.sendLog("[VertexCEFBrowser] MCEF.createBrowser returned null (CEF initializing or unavailable).");
            }
        } catch (Throwable t) {
            Logger.sendLog("[VertexCEFBrowser] Error initializing CEF browser: " + t.getMessage());
            t.printStackTrace();
        }
    }

    public void resize(int width, int height) {
        if (browser != null) {
            double scale = Minecraft.getInstance().getWindow().getGuiScale();
            browser.resize((int) (width * scale), (int) (height * scale));
            browser.executeJavaScript("document.body.style.zoom = '" + scale + "';", url, 0);
        }
    }

    public boolean render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (browser == null) {
            init();
            if (browser == null) return false;
        }
        
        if (!browser.isTextureReady()) {
            return false;
        }

        Identifier texture = (Identifier) (Object) browser.getTextureIdentifier();
        if (texture != null) {
            int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            
            try {
                // Ensure the web body is zoomed dynamically in case scale changes
                double scale = Minecraft.getInstance().getWindow().getGuiScale();
                browser.executeJavaScript("document.body.style.zoom = '" + scale + "';", url, 0);

                context.blit(texture, 0, 0, width, height, 0.0F, 1.0F, 0.0F, 1.0F);
                return true;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return false;
    }

    public void close() {
        // Intentionally keep browser instance alive for instant re-opening
    }

    public void injectMouseMove(int mouseX, int mouseY, int modifiers, boolean isLeave) {
        if (browser != null) {
            double scale = Minecraft.getInstance().getWindow().getGuiScale();
            browser.sendMouseMove((int) (mouseX * scale), (int) (mouseY * scale));
        }
    }

    public void injectMouseButton(int mouseX, int mouseY, int modifiers, int button, boolean pressed, int clickCount) {
        if (browser != null) {
            double scale = Minecraft.getInstance().getWindow().getGuiScale();
            if (pressed) {
                browser.sendMousePress((int) (mouseX * scale), (int) (mouseY * scale), button);
            } else {
                browser.sendMouseRelease((int) (mouseX * scale), (int) (mouseY * scale), button);
            }
        }
    }

    public void injectMouseWheel(int mouseX, int mouseY, int modifiers, int scrollAmount, int rotation) {
        if (browser != null) {
            double scale = Minecraft.getInstance().getWindow().getGuiScale();
            browser.sendMouseWheel((int) (mouseX * scale), (int) (mouseY * scale), (double) scrollAmount, modifiers);
        }
    }

    public void injectKeyPressed(char c, int keyCode, int modifiers) {
        if (browser != null) {
            browser.sendKeyPress(keyCode, 0, modifiers);
            if (c != 0) {
                browser.sendKeyTyped(c, modifiers);
            }
        }
    }
    
    public void injectKeyReleased(char c, int keyCode, int modifiers) {
        if (browser != null) {
            browser.sendKeyRelease(keyCode, 0, modifiers);
        }
    }

    public MCEFBrowser getBrowser() {
        return browser;
    }
}
