package com.vertexai.util.render;

import net.minecraft.client.renderer.rendertype.RenderType;

public class VFRenderLayers {

    public static final RenderType QUADS_DEPTH = net.minecraft.client.renderer.rendertype.RenderTypes.debugQuads();
    public static final RenderType QUADS_NO_DEPTH = net.minecraft.client.renderer.rendertype.RenderTypes.debugQuads();
    
    // We might need to use lines() or debugLineStrip(), fallback to gui if it fails to compile
    public static final RenderType LINES_NO_DEPTH = net.minecraft.client.renderer.rendertype.RenderTypes.debugQuads();
    public static final RenderType LINES_DEPTH = net.minecraft.client.renderer.rendertype.RenderTypes.debugQuads();
}
