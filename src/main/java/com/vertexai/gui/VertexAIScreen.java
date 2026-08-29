package com.vertexai.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import com.vertexai.gui.cef.VertexCEFBrowser;
import com.vertexai.gui.cef.VertexUIServer;
import com.vertexai.gui.web.MCEFBridge;
import com.vertexai.macro.MacroManager;
import com.vertexai.macro.AbstractMacro;
import com.vertexai.ui.screen.HUDEditorScreen;
import com.vertexai.util.Logger;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class VertexAIScreen extends Screen {

    private VertexCEFBrowser cefBrowser;

    // Native GUI State
    private int selectedTab = 0;
    private final String[] tabs = {"🌾 Farming", "⛏️ Mining", "⚔️ Combat", "🛡️ Failsafes", "🪟 HUD & UI", "⚙️ General"};
    private int scrollOffset = 0;

    public VertexAIScreen() {
        super(Component.literal("Vertex AI Dashboard"));
    }

    @Override
    protected void init() {
        super.init();
        // Automatically stop any running macro when opening the GUI
        if (MacroManager.getInstance().isRunning()) {
            MacroManager.getInstance().disable();
        }

        cefBrowser = VertexCEFBrowser.getInstance();
        if (cefBrowser != null) {
            cefBrowser.resize(this.width, this.height);
        }
        MCEFBridge.isConfigScreenOpen = true;
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (cefBrowser != null) {
            cefBrowser.resize(width, height);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.fill(0, 0, this.width, this.height, 0x99000000); // 60% background overlay

        boolean rendered = false;
        if (cefBrowser != null) {
            rendered = cefBrowser.render(context, mouseX, mouseY, delta);
        }

        if (!rendered) {
            renderNativeGui(context, mouseX, mouseY);
        }
    }

    private void renderNativeGui(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        int panelW = Math.min(560, this.width - 40);
        int panelH = Math.min(360, this.height - 40);
        int startX = (this.width - panelW) / 2;
        int startY = (this.height - panelH) / 2;

        // Panel Background & Border
        context.fill(startX, startY, startX + panelW, startY + panelH, 0xEE0F172A); // Slate 900
        context.fill(startX, startY, startX + panelW, startY + 1, 0xFF38BDF8); // Top cyan border
        context.fill(startX, startY + panelH - 1, startX + panelW, startY + panelH, 0xFF1E293B);
        context.fill(startX, startY, startX + 1, startY + panelH, 0xFF1E293B);
        context.fill(startX + panelW - 1, startY, startX + panelW, startY + panelH, 0xFF1E293B);

        // Header
        context.fill(startX, startY, startX + panelW, startY + 36, 0xFF1E293B);
        context.text(this.font, "⚡ VERTEX CLIENT", startX + 14, startY + 13, 0xFF38BDF8, false);
        context.text(this.font, "v26.1", startX + 120, startY + 14, 0xFF64748B, false);

        // Open Web UI Button in Header
        int webBtnX = startX + panelW - 170;
        int webBtnY = startY + 8;
        int webBtnW = 120;
        int webBtnH = 20;
        boolean webHover = mouseX >= webBtnX && mouseX <= webBtnX + webBtnW && mouseY >= webBtnY && mouseY <= webBtnY + webBtnH;
        context.fill(webBtnX, webBtnY, webBtnX + webBtnW, webBtnY + webBtnH, webHover ? 0xFF0284C7 : 0xFF0369A1);
        context.centeredText(this.font, "🌐 Open Web UI", webBtnX + webBtnW / 2, webBtnY + 6, 0xFFFFFFFF);

        // Close button (X)
        int closeBtnX = startX + panelW - 32;
        int closeBtnY = startY + 8;
        boolean closeHover = mouseX >= closeBtnX && mouseX <= closeBtnX + 22 && mouseY >= closeBtnY && mouseY <= closeBtnY + 20;
        context.fill(closeBtnX, closeBtnY, closeBtnX + 22, closeBtnY + 20, closeHover ? 0xFFEF4444 : 0xFF334155);
        context.centeredText(this.font, "✕", closeBtnX + 11, closeBtnY + 6, 0xFFFFFFFF);

        // Sidebar Navigation
        int sidebarW = 125;
        int sidebarX = startX;
        int sidebarY = startY + 36;
        int contentX = startX + sidebarW;
        int contentY = sidebarY;
        int contentW = panelW - sidebarW;
        int contentH = panelH - 36;

        context.fill(sidebarX, sidebarY, sidebarX + sidebarW, startY + panelH, 0xFF090D16);
        context.fill(contentX - 1, sidebarY, contentX, startY + panelH, 0xFF1E293B);

        int tabY = sidebarY + 8;
        for (int i = 0; i < tabs.length; i++) {
            int currentTabY = tabY + (i * 32);
            boolean isSelected = (i == selectedTab);
            boolean isHovered = (mouseX >= sidebarX && mouseX < sidebarX + sidebarW && mouseY >= currentTabY && mouseY < currentTabY + 28);

            if (isSelected) {
                context.fill(sidebarX, currentTabY, sidebarX + sidebarW, currentTabY + 28, 0x3338BDF8);
                context.fill(sidebarX, currentTabY, sidebarX + 3, currentTabY + 28, 0xFF38BDF8);
            } else if (isHovered) {
                context.fill(sidebarX, currentTabY, sidebarX + sidebarW, currentTabY + 28, 0x1A38BDF8);
            }

            int textColor = isSelected ? 0xFF38BDF8 : (isHovered ? 0xFFF1F5F9 : 0xFF94A3B8);
            context.text(this.font, tabs[i], sidebarX + 12, currentTabY + 9, textColor, false);
        }

        // Render Tab Content
        renderCategoryContent(context, selectedTab, contentX + 14, contentY + 12, contentW - 28, contentH - 24, mouseX, mouseY);
    }

    private void renderCategoryContent(GuiGraphicsExtractor context, int tab, int x, int y, int w, int h, int mouseX, int mouseY) {
        List<MacroControlItem> items = getItemsForTab(tab);

        int itemY = y - scrollOffset;
        for (MacroControlItem item : items) {
            if (itemY + 44 < y) {
                itemY += 50;
                continue;
            }
            if (itemY > y + h - 10) break;

            // Card background
            boolean cardHover = mouseX >= x && mouseX <= x + w && mouseY >= itemY && mouseY <= itemY + 42;
            context.fill(x, itemY, x + w, itemY + 42, cardHover ? 0xFF1E293B : 0xFF131C2E);
            context.fill(x, itemY, x + w, itemY + 1, 0xFF334155);

            // Title & Description
            context.text(this.font, item.name, x + 10, itemY + 8, 0xFFF8FAFC, false);
            context.text(this.font, item.description, x + 10, itemY + 22, 0xFF64748B, false);

            // Action Button / Status
            int btnW = 75;
            int btnH = 22;
            int btnX = x + w - btnW - 10;
            int btnY = itemY + 10;

            boolean btnHover = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
            int btnColor = item.actionColor != 0 ? item.actionColor : (item.active ? (btnHover ? 0xFFDC2626 : 0xFFEF4444) : (btnHover ? 0xFF059669 : 0xFF10B981));

            context.fill(btnX, btnY, btnX + btnW, btnY + btnH, btnColor);
            context.centeredText(this.font, item.actionText, btnX + btnW / 2, btnY + 7, 0xFFFFFFFF);

            itemY += 50;
        }
    }

    private List<MacroControlItem> getItemsForTab(int tab) {
        List<MacroControlItem> list = new ArrayList<>();
        MacroManager mm = MacroManager.getInstance();

        switch (tab) {
            case 0 -> { // Farming
                list.add(new MacroControlItem("Crop / Farming Macro", "Automated S-shape farming & harvesting", isMacroRunning(com.vertexai.macro.impl.FarmingMacro.FarmingMacro.class), () -> toggleMacro(com.vertexai.macro.impl.FarmingMacro.FarmingMacro.getInstance())));
                list.add(new MacroControlItem("Sugar Cane / Cactus", "Automated vertical cane & cactus macro", isMacroRunning(com.vertexai.macro.impl.CaneCactusMacro.CaneCactusMacro.class), () -> toggleMacro(com.vertexai.macro.impl.CaneCactusMacro.CaneCactusMacro.getInstance())));
                list.add(new MacroControlItem("Farm Builder Macro", "Auto farm layer builder with schematics", isMacroRunning(com.vertexai.macro.impl.FarmBuilderMacro.FarmBuilderMacro.class), () -> toggleMacro(com.vertexai.macro.impl.FarmBuilderMacro.FarmBuilderMacro.getInstance())));
                list.add(new MacroControlItem("Garden Visitor Handler", "Auto-interact & trade with visitors", isMacroRunning(com.vertexai.macro.impl.GardenVisitorMacro.GardenVisitorMacro.class), () -> toggleMacro(com.vertexai.macro.impl.GardenVisitorMacro.GardenVisitorMacro.getInstance())));
                list.add(new MacroControlItem("Pest Hunter Macro", "Automated vacuum & pest extermination", isMacroRunning(com.vertexai.macro.impl.PestHunterMacro.PestHunterMacro.class), () -> toggleMacro(com.vertexai.macro.impl.PestHunterMacro.PestHunterMacro.getInstance())));
            }
            case 1 -> { // Mining
                list.add(new MacroControlItem("Mining / Commission Macro", "Mines Dwarven Mines & Glacite commissions", isMacroRunning(com.vertexai.macro.impl.CommissionMacro.CommissionMacro.class), () -> toggleMacro(com.vertexai.macro.impl.CommissionMacro.CommissionMacro.getInstance())));
                list.add(new MacroControlItem("Route Gemstone Miner", "Follows pathfinding routes & mines gems", isMacroRunning(com.vertexai.macro.impl.RouteMiner.RouteMinerMacro.class), () -> toggleMacro(com.vertexai.macro.impl.RouteMiner.RouteMinerMacro.getInstance())));
                list.add(new MacroControlItem("Mithril / Ore Miner", "Automated general block & vein miner", isMacroRunning(com.vertexai.macro.impl.MiningMacro.MiningMacro.class), () -> toggleMacro(com.vertexai.macro.impl.MiningMacro.MiningMacro.getInstance())));
                list.add(new MacroControlItem("Powder Macro", "Automated treasure chest & powder grinding", isMacroRunning(com.vertexai.macro.impl.PowderMacro.PowderMacro.class), () -> toggleMacro(com.vertexai.macro.impl.PowderMacro.PowderMacro.getInstance())));
                list.add(new MacroControlItem("Glacial Vein Miner", "Auto mines Glacite tunnels & frozen veins", isMacroRunning(com.vertexai.macro.impl.GlacialMacro.GlacialMacro.class), () -> toggleMacro(com.vertexai.macro.impl.GlacialMacro.GlacialMacro.getInstance())));
                list.add(new MacroControlItem("Nuker Macro", "High-speed block nuker with path verification", isMacroRunning(com.vertexai.macro.impl.NukerMacro.NukerMacro.class), () -> toggleMacro(com.vertexai.macro.impl.NukerMacro.NukerMacro.getInstance())));
            }
            case 2 -> { // Combat
                list.add(new MacroControlItem("Slayer Macro", "Auto Slayer boss spawning & killing", isMacroRunning(com.vertexai.macro.impl.SlayerMacro.SlayerMacro.class), () -> toggleMacro(com.vertexai.macro.impl.SlayerMacro.SlayerMacro.getInstance())));
                list.add(new MacroControlItem("Mob Killer Macro", "Entity target rotation & automatic attacks", isMacroRunning(com.vertexai.macro.impl.CombatMacro.CombatMacro.class), () -> toggleMacro(com.vertexai.macro.impl.CombatMacro.CombatMacro.getInstance())));
                list.add(new MacroControlItem("Zealot Macro", "Automated End Zealot & Bruiser grinding", isMacroRunning(com.vertexai.macro.impl.ZealotMacro.ZealotMacro.class), () -> toggleMacro(com.vertexai.macro.impl.ZealotMacro.ZealotMacro.getInstance())));
                list.add(new MacroControlItem("Fishing Macro", "Automated rod casting, reeling, & trophy fishing", isMacroRunning(com.vertexai.macro.impl.FishingMacro.FishingMacro.class), () -> toggleMacro(com.vertexai.macro.impl.FishingMacro.FishingMacro.getInstance())));
            }
            case 3 -> { // Failsafes
                list.add(new MacroControlItem("Knockback Failsafe", "Instantly disables on player velocity change", true, () -> Logger.sendMessage("Knockback Failsafe is active.")));
                list.add(new MacroControlItem("Staff Detector", "Detects invisible GM / admin watchers", true, () -> Logger.sendMessage("Staff Detector is active.")));
                list.add(new MacroControlItem("Lag Detector", "Pauses automation during server lag spikes", true, () -> Logger.sendMessage("Lag Detector is active.")));
                list.add(new MacroControlItem("Captcha Detector", "Alerts & halts on Hypixel map / chat captchas", true, () -> Logger.sendMessage("Captcha Detector is active.")));
            }
            case 4 -> { // HUD & UI
                list.add(new MacroControlItem("HUD Element Editor", "Drag, scale, and reposition all HUD widgets", false, "Open Editor", 0xFF6366F1, () -> Minecraft.getInstance().setScreen(new HUDEditorScreen(this))));
                list.add(new MacroControlItem("Status HUD", "Displays active macro runtime & analytics", true, () -> Logger.sendMessage("Status HUD toggled.")));
                list.add(new MacroControlItem("Inventory HUD", "Displays inventory item slots on screen", true, () -> Logger.sendMessage("Inventory HUD toggled.")));
                list.add(new MacroControlItem("Commission Tracker HUD", "Displays Dwarven & Glacite commission steps", true, () -> Logger.sendMessage("Commission HUD toggled.")));
            }
            case 5 -> { // General & Web Dashboard
                list.add(new MacroControlItem("Launch Web Dashboard", "Opens HTML5/Svelte UI at http://127.0.0.1:" + VertexUIServer.PORT, false, "Open URL", 0xFF0284C7, this::openWebUiInBrowser));
                list.add(new MacroControlItem("Stop All Macros", "Emergency stop for all running background tasks", false, "Stop All", 0xFFDC2626, () -> MacroManager.getInstance().disable()));
            }
        }

        return list;
    }

    private boolean isMacroRunning(Class<? extends AbstractMacro> macroClass) {
        AbstractMacro current = MacroManager.getInstance().getCurrentMacro();
        return current != null && macroClass.isInstance(current) && current.isEnabled();
    }

    private void toggleMacro(AbstractMacro macro) {
        if (macro == null) return;
        if (macro.isEnabled()) {
            macro.disable();
            Logger.sendMessage("§cDisabled " + macro.getName());
        } else {
            macro.enable();
            Logger.sendMessage("§aEnabled " + macro.getName());
            this.onClose(); // Close GUI so player is back in game while macro runs
        }
    }

    private void openWebUiInBrowser() {
        try {
            String url = "http://127.0.0.1:" + VertexUIServer.PORT + "/index.html";
            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(URI.create(url));
            } else {
                net.minecraft.util.Util.getPlatform().openUri(URI.create(url));
            }
            Logger.sendMessage("§bOpened Web Dashboard in browser: " + url);
        } catch (Throwable t) {
            Logger.sendError("Failed to open browser: " + t.getMessage());
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isAction) {
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();

        int panelW = Math.min(560, this.width - 40);
        int panelH = Math.min(360, this.height - 40);
        int startX = (this.width - panelW) / 2;
        int startY = (this.height - panelH) / 2;

        // Open Web UI button
        int webBtnX = startX + panelW - 170;
        int webBtnY = startY + 8;
        if (mouseX >= webBtnX && mouseX <= webBtnX + 120 && mouseY >= webBtnY && mouseY <= webBtnY + 20) {
            openWebUiInBrowser();
            return true;
        }

        // Close button (X)
        int closeBtnX = startX + panelW - 32;
        int closeBtnY = startY + 8;
        if (mouseX >= closeBtnX && mouseX <= closeBtnX + 22 && mouseY >= closeBtnY && mouseY <= closeBtnY + 20) {
            this.onClose();
            return true;
        }

        // Tab selection
        int sidebarW = 125;
        int sidebarX = startX;
        int sidebarY = startY + 36;
        int tabY = sidebarY + 8;
        for (int i = 0; i < tabs.length; i++) {
            int currentTabY = tabY + (i * 32);
            if (mouseX >= sidebarX && mouseX < sidebarX + sidebarW && mouseY >= currentTabY && mouseY < currentTabY + 28) {
                this.selectedTab = i;
                this.scrollOffset = 0;
                return true;
            }
        }

        // Action buttons inside content
        int contentX = startX + sidebarW;
        int contentW = panelW - sidebarW;
        int contentH = panelH - 36;
        int listX = contentX + 14;
        int listY = sidebarY + 12;
        int listW = contentW - 28;

        List<MacroControlItem> items = getItemsForTab(selectedTab);
        int itemY = listY - scrollOffset;
        for (MacroControlItem item : items) {
            if (itemY + 44 >= listY && itemY <= listY + contentH - 10) {
                int btnW = 75;
                int btnH = 22;
                int btnX = listX + listW - btnW - 10;
                int btnY = itemY + 10;

                if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                    item.action.run();
                    return true;
                }
            }
            itemY += 50;
        }

        if (cefBrowser != null) {
            cefBrowser.injectMouseButton(mouseX, mouseY, 0, event.button(), true, 1);
        }
        return super.mouseClicked(event, isAction);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount != 0) {
            this.scrollOffset = Math.max(0, this.scrollOffset - (int) (verticalAmount * 20));
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onClose() {
        MCEFBridge.isConfigScreenOpen = false;
        super.onClose();
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private static class MacroControlItem {
        final String name;
        final String description;
        final boolean active;
        final String actionText;
        final int actionColor;
        final Runnable action;

        MacroControlItem(String name, String description, boolean active, Runnable action) {
            this.name = name;
            this.description = description;
            this.active = active;
            this.actionText = active ? "DISABLE" : "ENABLE";
            this.actionColor = 0;
            this.action = action;
        }

        MacroControlItem(String name, String description, boolean active, String actionText, int actionColor, Runnable action) {
            this.name = name;
            this.description = description;
            this.active = active;
            this.actionText = actionText;
            this.actionColor = actionColor;
            this.action = action;
        }
    }
}

