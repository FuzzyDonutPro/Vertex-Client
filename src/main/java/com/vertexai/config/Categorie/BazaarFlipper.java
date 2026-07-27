package com.vertexai.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class BazaarFlipper {

    @ConfigOption(name = "Min Profit Margin (%)", desc = "Minimum profit percentage required to place a flip order.")
    @ConfigEditorSlider(minValue = 1.0f, maxValue = 25.0f, minStep = 0.5f)
    public float minProfitPercent = 5.0f;

    @ConfigOption(name = "Max Order Spend (M)", desc = "Maximum coins (in Millions) spent per Bazaar order.")
    @ConfigEditorSlider(minValue = 1.0f, maxValue = 100.0f, minStep = 1.0f)
    public float maxSpendCoinsM = 10.0f;

    @ConfigOption(name = "Auto Claim Orders", desc = "Automatically claim filled buy and sell orders.")
    @ConfigEditorBoolean
    public boolean autoClaimOrders = true;

    @ConfigOption(name = "Relist Undercut Orders", desc = "Automatically cancel and relist orders if undercut by competitors.")
    @ConfigEditorBoolean
    public boolean relistUndercutOrders = true;
}
