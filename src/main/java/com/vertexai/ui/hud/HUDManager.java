package com.vertexai.ui.hud;

import com.vertexai.Vertex;
import com.vertexai.client.overlay.AbstractHUDElement;
import com.vertexai.config.Categorie.HUD;
import com.vertexai.config.VertexConfig;
import com.vertexai.ui.hud.elements.*;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public class HUDManager {

    private static final HUDManager instance = new HUDManager();
    private final List<AbstractHUDElement> elements = new ArrayList<>();
    private boolean positionsLoaded;

    private HUDManager() {
        // Elements will be added here
        registerElements();
    }

    public static HUDManager getInstance() {
        return instance;
    }

    private void registerElements() {
        elements.add(StatusHUD.getInstance());
        elements.add(InventoryHUD.getInstance());
        elements.add(ProfitHUD.getInstance());
        elements.add(CommissionHUD.getInstance());
        elements.add(MacroHUD.getInstance());
        elements.add(GlacialCommissionHUD.getInstance());
        elements.add(RouteBuilderHUD.getInstance());
        elements.add(FishingHUD.getInstance());
        elements.add(MiningHUD.getInstance());
        elements.add(com.vertexai.client.overlay.SpotifyHudElement.getInstance());
    }

    private void ensurePositionsLoaded() {
        if (positionsLoaded) {
            return;
        }
        if (Vertex.config() == null || Vertex.config().hud == null) {
            return;
        }
        loadPositions();
        positionsLoaded = true;
    }

    public void onHudRender(GuiGraphics context) {
        if (net.minecraft.client.Minecraft.getInstance().options.hideGui) return;
        ensurePositionsLoaded();

        for (AbstractHUDElement element : elements) {
            if (element.isEnabled() && isEnabledInConfig(element)) {
                element.render(context, 0); // tickDelta 0 for now or pass it from EventManager
            }
        }
    }

    public List<AbstractHUDElement> getEditableElements() {
        ensurePositionsLoaded();
        List<AbstractHUDElement> editable = new ArrayList<>();
        for (AbstractHUDElement element : elements) {
            if (element.isEnabled() && isEnabledInConfig(element)) {
                editable.add(element);
            }
        }
        return List.copyOf(editable);
    }

    private boolean isEnabledInConfig(AbstractHUDElement element) {
        HUD hud = Vertex.config().hud;
        if (element instanceof StatusHUD) return hud.enableStatusHud;
        if (element instanceof InventoryHUD) return hud.enableInventoryHud;
        if (element instanceof ProfitHUD) return hud.enableProfitHud;
        if (element instanceof CommissionHUD) return hud.enableCommissionHud;
        if (element instanceof GlacialCommissionHUD) return hud.enableGlacialHud;
        if (element instanceof MacroHUD) return hud.enableDebugHud;
        if (element instanceof RouteBuilderHUD) return hud.enableRouteBuilderHud;
        if (element instanceof FishingHUD) return hud.enableFishingHud;
        if (element instanceof MiningHUD) return hud.enableMiningHud;
        if (element instanceof com.vertexai.client.overlay.SpotifyHudElement) return hud.enableSpotifyHud;
        return true;
    }

    public void loadPositions() {
        HUD hud = Vertex.config().hud;
        // Map elements to config fields
        updateElement(StatusHUD.getInstance(), hud.statusHUD);
        updateElement(InventoryHUD.getInstance(), hud.inventoryHUD);
        updateElement(ProfitHUD.getInstance(), hud.profitHUD);
        updateElement(CommissionHUD.getInstance(), hud.commissionHUD);
        updateElement(MacroHUD.getInstance(), hud.debugHUD);
        updateElement(GlacialCommissionHUD.getInstance(), hud.glacialHUD);
        updateElement(RouteBuilderHUD.getInstance(), hud.routeBuilderHUD);
        updateElement(FishingHUD.getInstance(), hud.fishingHUD);
        updateElement(MiningHUD.getInstance(), hud.miningHUD);
        updateElement(com.vertexai.client.overlay.SpotifyHudElement.getInstance(), hud.spotifyHUD);
        positionsLoaded = true;
    }

    private void updateElement(AbstractHUDElement element, VertexConfig.HUDPos pos) {
        element.setX(pos.x);
        element.setY(pos.y);
        element.setAnchor(pos.anchor);
        element.setScale(pos.scale > 0.0f ? pos.scale : 1.0f);
    }

    public void savePositions() {
        ensurePositionsLoaded();
        HUD hud = Vertex.config().hud;
        saveElement(StatusHUD.getInstance(), hud.statusHUD);
        saveElement(InventoryHUD.getInstance(), hud.inventoryHUD);
        saveElement(ProfitHUD.getInstance(), hud.profitHUD);
        saveElement(CommissionHUD.getInstance(), hud.commissionHUD);
        saveElement(MacroHUD.getInstance(), hud.debugHUD);
        saveElement(GlacialCommissionHUD.getInstance(), hud.glacialHUD);
        saveElement(RouteBuilderHUD.getInstance(), hud.routeBuilderHUD);
        saveElement(FishingHUD.getInstance(), hud.fishingHUD);
        saveElement(MiningHUD.getInstance(), hud.miningHUD);
        saveElement(com.vertexai.client.overlay.SpotifyHudElement.getInstance(), hud.spotifyHUD);

        // Save the main config
        com.vertexai.VertexClient.configManager.saveConfig();
    }

    public void resetPositionsToDefaults() {
        // Keep in sync with default values in VertexConfig.HUD
        StatusHUD.getInstance().setX(5);
        StatusHUD.getInstance().setY(5);
        StatusHUD.getInstance().setAnchor(0);
        StatusHUD.getInstance().setScale(1.0f);

        InventoryHUD.getInstance().setX(0);
        InventoryHUD.getInstance().setY(52);
        InventoryHUD.getInstance().setAnchor(2);
        InventoryHUD.getInstance().setScale(1.0f);

        ProfitHUD.getInstance().setX(5);
        ProfitHUD.getInstance().setY(120);
        ProfitHUD.getInstance().setAnchor(0);
        ProfitHUD.getInstance().setScale(1.0f);

        CommissionHUD.getInstance().setX(5);
        CommissionHUD.getInstance().setY(5);
        CommissionHUD.getInstance().setAnchor(0);
        CommissionHUD.getInstance().setScale(1.0f);

        MacroHUD.getInstance().setX(1);
        MacroHUD.getInstance().setY(10);
        MacroHUD.getInstance().setAnchor(0);
        MacroHUD.getInstance().setScale(1.0f);

        GlacialCommissionHUD.getInstance().setX(5);
        GlacialCommissionHUD.getInstance().setY(5);
        GlacialCommissionHUD.getInstance().setAnchor(0);
        GlacialCommissionHUD.getInstance().setScale(1.0f);

        RouteBuilderHUD.getInstance().setX(5);
        RouteBuilderHUD.getInstance().setY(90);
        RouteBuilderHUD.getInstance().setAnchor(0);
        RouteBuilderHUD.getInstance().setScale(1.0f);

        FishingHUD.getInstance().setX(5);
        FishingHUD.getInstance().setY(185);
        FishingHUD.getInstance().setAnchor(0);
        FishingHUD.getInstance().setScale(1.0f);

        MiningHUD.getInstance().setX(5);
        MiningHUD.getInstance().setY(130);
        MiningHUD.getInstance().setAnchor(0);
        MiningHUD.getInstance().setScale(1.0f);

        com.vertexai.client.overlay.SpotifyHudElement.getInstance().setX(5);
        com.vertexai.client.overlay.SpotifyHudElement.getInstance().setY(230);
        com.vertexai.client.overlay.SpotifyHudElement.getInstance().setAnchor(0);
        com.vertexai.client.overlay.SpotifyHudElement.getInstance().setScale(1.0f);

        savePositions();
    }

    private void saveElement(AbstractHUDElement element, VertexConfig.HUDPos pos) {
        pos.x = element.getX();
        pos.y = element.getY();
        pos.anchor = element.getAnchor();
        pos.scale = element.getScale();
    }
}
