package com.vertexai.config;

import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.annotations.Category;

import com.vertexai.VertexClient;
import com.vertexai.config.Categorie.*;

@SuppressWarnings("deprecation")
public class VertexConfig extends Config {

    @Category(name = "General", desc = "General Settings")
    public General general = new General();
    @Category(name = "Commission", desc = "Commission Settings")
    public Commission commission = new Commission();
    @Category(name = "Mining Macro", desc = "Mining Macro Settings")
    public MiningMacro miningMacro = new MiningMacro();
    @Category(name = "Route Miner", desc = "Route Miner Settings")
    public RouteMiner routeMiner = new RouteMiner();
    @Category(name = "Powder Macro", desc = "Powder Macro Settings")
    public PowderMacro powderMacro = new PowderMacro();
    @Category(name = "Fishing", desc = "Fishing Macro Settings")
    public Fishing fishing = new Fishing();
    @Category(name = "Farming", desc = "Farming Macro Settings")
    public Farming farming = new Farming();
    @Category(name = "Farm Builder", desc = "Garden Farm Builder Settings")
    public FarmBuilder farmBuilder = new FarmBuilder();
    @Category(name = "Melon & Pumpkin", desc = "Melon and Pumpkin specific settings")
    public MelonPumpkin melonPumpkin = new MelonPumpkin();
    @Category(name = "Foraging", desc = "Foraging Macro Settings")
    public com.vertexai.config.Categorie.Foraging foraging = new com.vertexai.config.Categorie.Foraging();
    @Category(name = "Combat", desc = "Combat and Slayer Macro Settings")
    public com.vertexai.config.Categorie.Combat combat = new com.vertexai.config.Categorie.Combat();

    @Category(name = "Dungeons", desc = "Dungeons and Catacombs Settings")
    public com.vertexai.config.Categorie.Dungeons dungeons = new com.vertexai.config.Categorie.Dungeons();
    @Category(name = "Rift", desc = "Rift Settings")
    public Rift rift = new Rift();
    @Category(name = "Delays", desc = "Delay Settings")
    public Delays delays = new Delays();
    @Category(name = "Failsafe", desc = "Failsafe Settings")
    public Failsafe failsafe = new Failsafe();
    @Category(name = "Debug", desc = "Debug Settings")
    public Debug debug = new Debug();
    @Category(name = "HUD", desc = "HUD Settings")
    public HUD hud = new HUD();
    @Category(name = "Render", desc = "Render Settings")
    public Render render = new Render();
    @Category(name = "Themes & Styling", desc = "Themes and Styling Settings")
    public GuiConfig gui = new GuiConfig();
    @Category(name = "Animations", desc = "Animation Settings")
    public Animations animations = new Animations();
    @Category(name = "Bazaar Flipper", desc = "Bazaar Order Flipper Settings")
    public BazaarFlipper bazaarFlipper = new BazaarFlipper();
    @Category(name = "Utilities", desc = "Utility and QoL Settings")
    public Utilities utilities = new Utilities();
    @Category(name = "Other", desc = "Other Settings")
    public Misc misc = new Misc();
    @Category(name = "Webhook", desc = "Discord Webhook Settings")
    public Webhook webhook = new Webhook();
    @Category(name = "Spotify", desc = "Spotify Web API Integration")
    public com.vertexai.config.Categorie.Spotify spotify = new com.vertexai.config.Categorie.Spotify();
//   @Expose
//   @Category(name = "Main", desc = "")
//   public MainCategory mainCategory = new MainCategory();

    @Override
    public String getTitle() {
        return "Vertex (v" + VertexClient.instance.VERSION + ")";
    }

    public int getRandomRotationTime() {
        int sampled = (
                delays.rotationTime +
                        (int) (Math.random() * delays.rotationTimeRandomizer)
        );
        int humanFast = (int) Math.round(sampled * 0.8d);
        return Math.max(120, humanFast);
    }

    public int getRandomSneakTime() {
        return (
                delays.sneakTime +
                        (int) (Math.random() * delays.sneakTimeRandomizer)
        );
    }

    public int getRandomGuiWaitDelay() {
        return (
                delays.delaysGuiDelay +
                        (int) (Math.random() * delays.delaysGuiDelayRandomizer)
        );
    }

    public static class HUDPos {
        public float x;
        public float y;
        public int anchor;
        public float scale;

        public HUDPos(float x, float y, int anchor, float scale) {
            this.x = x;
            this.y = y;
            this.anchor = anchor;
            this.scale = scale;
        }
    }
}

