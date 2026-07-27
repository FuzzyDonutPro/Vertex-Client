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
        elements.add(CommissionHUD.getInstance());
        elements.add(DebugHUD.getInstance());
        elements.add(GlacialCommissionHUD.getInstance());
        elements.add(RouteBuilderHUD.getInstance());
        elements.add(FishingHUD.getInstance());
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
        if (Vertex.mc().gui.hud.isHidden()) return;
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
        if (element instanceof CommissionHUD) return hud.enableCommissionHud;
        if (element instanceof GlacialCommissionHUD) return hud.enableGlacialHud;
        if (element instanceof DebugHUD) return hud.enableDebugHud;
        if (element instanceof RouteBuilderHUD) return hud.enableRouteBuilderHud;
        if (element instanceof FishingHUD) return hud.enableFishingHud;
        return true;
    }

    public void loadPositions() {
        HUD hud = Vertex.config().hud;
        // Map elements to config fields
        updateElement(CommissionHUD.getInstance(), hud.commissionHUD);
        updateElement(DebugHUD.getInstance(), hud.debugHUD);
        updateElement(GlacialCommissionHUD.getInstance(), hud.glacialHUD);
        updateElement(RouteBuilderHUD.getInstance(), hud.routeBuilderHUD);
        updateElement(FishingHUD.getInstance(), hud.fishingHUD);
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
        saveElement(CommissionHUD.getInstance(), hud.commissionHUD);
        saveElement(DebugHUD.getInstance(), hud.debugHUD);
        saveElement(GlacialCommissionHUD.getInstance(), hud.glacialHUD);
        saveElement(RouteBuilderHUD.getInstance(), hud.routeBuilderHUD);
        saveElement(FishingHUD.getInstance(), hud.fishingHUD);

        // Save the main config
        com.vertexai.VertexClient.configManager.saveConfig();
    }

    public void resetPositionsToDefaults() {
        // Keep in sync with default values in VertexConfig.HUD
        CommissionHUD.getInstance().setX(5);
        CommissionHUD.getInstance().setY(5);
        CommissionHUD.getInstance().setAnchor(0);
        CommissionHUD.getInstance().setScale(1.0f);

        DebugHUD.getInstance().setX(1);
        DebugHUD.getInstance().setY(10);
        DebugHUD.getInstance().setAnchor(0);
        DebugHUD.getInstance().setScale(1.0f);

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

        savePositions();
    }

    private void saveElement(AbstractHUDElement element, VertexConfig.HUDPos pos) {
        pos.x = element.getX();
        pos.y = element.getY();
        pos.anchor = element.getAnchor();
        pos.scale = element.getScale();
    }
}
