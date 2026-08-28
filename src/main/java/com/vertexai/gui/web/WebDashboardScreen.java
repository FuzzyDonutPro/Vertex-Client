package com.vertexai.gui.web;

import com.vertexai.gui.VertexAIScreen;


/**
 * WebDashboardScreen — Embedded MCEF Chromium Web Screen for Vertex Client.
 * Renders full HTML5/CSS3 web apps using MCEF when loaded, falling back to VertexAIScreen.
 */
public class WebDashboardScreen extends VertexAIScreen {

    public WebDashboardScreen() {
        super();
        System.out.println("[vertexai/DEBUG] WebDashboardScreen instance constructed!");
    }

}
