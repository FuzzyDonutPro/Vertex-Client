package com.vertexai.pathing;

import com.mojang.blaze3d.systems.RenderSystem;
import com.vertexai.event.EventManager;
import com.vertexai.util.RenderUtil;
import com.vertexai.util.WorldRenderContextWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class PathRenderer {
    public static BlockPos targetBlock = null;
    public static Vec3 exactTarget = null;
    public static List<BlockPos> currentPath = new ArrayList<>();

    public static void init() {
        // PathRenderer is now called directly from EventManager's main render callback
        // inside the beginWorldRender/endWorldRender window
    }

    public static void render(WorldRenderContextWrapper context) {
        if (targetBlock == null || exactTarget == null) return;
        if (Minecraft.getInstance().player == null) return;

        com.vertexai.config.Categorie.Render renderConfig = com.vertexai.Vertex.config().render;

        // 1. Draw bounding box around target block (Red)
        if (renderConfig.targetBlockESP) {
            AABB blockAABB = new AABB(targetBlock).inflate(0.01);
            RenderUtil.drawAABB(blockAABB, new java.awt.Color(255, 60, 60, 200), true);
        }

        // 2. Draw 1/16th blue box at random sub-coordinate target
        if (renderConfig.targetPointESP) {
            double s = 1.0 / 16.0 / 2.0; // half size
            AABB smallAABB = new AABB(
                    exactTarget.x() - s, exactTarget.y() - s, exactTarget.z() - s,
                    exactTarget.x() + s, exactTarget.y() + s, exactTarget.z() + s
            );
            RenderUtil.drawAABB(smallAABB, new java.awt.Color(56, 189, 248, 255), true);
        }

        // 3. Draw path line (Theme Color)
        if (renderConfig.pathESP && !currentPath.isEmpty()) {
            int theme = com.vertexai.Vertex.config().gui.getThemeColorInt();
            java.awt.Color pathColor = new java.awt.Color((theme >> 16) & 0xFF, (theme >> 8) & 0xFF, theme & 0xFF, 240);
            
            List<Vec3> points = new ArrayList<>();
            points.add(Minecraft.getInstance().player.position().add(0, 0.8, 0));
            for (BlockPos pos : currentPath) {
                points.add(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
            }
            points.add(exactTarget);
            RenderUtil.drawPolyline(points, pathColor, true);
        }
    }

    private static void drawAABB(BufferBuilder buffer, Matrix4f matrix, AABB box, float r, float g, float b, float a) {
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        // Bottom
        buffer.addVertex(matrix, minX, minY, minZ).setColor((int)(r*255), (int)(g*255), (int)(b*255), (int)(a*255)); buffer.addVertex(matrix, maxX, minY, minZ).setColor((int)(r*255), (int)(g*255), (int)(b*255), (int)(a*255));
        buffer.addVertex(matrix, maxX, minY, minZ).setColor((int)(r*255), (int)(g*255), (int)(b*255), (int)(a*255)); buffer.addVertex(matrix, maxX, minY, maxZ).setColor((int)(r*255), (int)(g*255), (int)(b*255), (int)(a*255));
        buffer.addVertex(matrix, maxX, minY, maxZ).setColor((int)(r*255), (int)(g*255), (int)(b*255), (int)(a*255)); buffer.addVertex(matrix, minX, minY, maxZ).setColor((int)(r*255), (int)(g*255), (int)(b*255), (int)(a*255));
        buffer.addVertex(matrix, minX, minY, maxZ).setColor((int)(r*255), (int)(g*255), (int)(b*255), (int)(a*255)); buffer.addVertex(matrix, minX, minY, minZ).setColor((int)(r*255), (int)(g*255), (int)(b*255), (int)(a*255));

        // Top
        buffer.addVertex(matrix, minX, maxY, minZ).setColor((int)(r*255), (int)(g*255), (int)(b*255), (int)(a*255)); buffer.addVertex(matrix, maxX, maxY, minZ).setColor((int)(r*255), (int)(g*255), (int)(b*255), (int)(a*255));
        buffer.addVertex(matrix, maxX, maxY, minZ).setColor((int)(r*255), (int)(g*255), (int)(b*255), (int)(a*255)); buffer.addVertex(matrix, maxX, maxY, maxZ).setColor((int)(r*255), (int)(g*255), (int)(b*255), (int)(a*255));
        buffer.addVertex(matrix, maxX, maxY, maxZ).setColor((int)(r*255), (int)(g*255), (int)(b*255), (int)(a*255)); buffer.addVertex(matrix, minX, maxY, maxZ).setColor((int)(r*255), (int)(g*255), (int)(b*255), (int)(a*255));
        buffer.addVertex(matrix, minX, maxY, maxZ).setColor((int)(r*255), (int)(g*255), (int)(b*255), (int)(a*255)); buffer.addVertex(matrix, minX, maxY, minZ).setColor((int)(r*255), (int)(g*255), (int)(b*255), (int)(a*255));

        // Pillars
        buffer.addVertex(matrix, minX, minY, minZ).setColor((int)(r*255), (int)(g*255), (int)(b*255), (int)(a*255)); buffer.addVertex(matrix, minX, maxY, minZ).setColor((int)(r*255), (int)(g*255), (int)(b*255), (int)(a*255));
        buffer.addVertex(matrix, maxX, minY, minZ).setColor((int)(r*255), (int)(g*255), (int)(b*255), (int)(a*255)); buffer.addVertex(matrix, maxX, maxY, minZ).setColor((int)(r*255), (int)(g*255), (int)(b*255), (int)(a*255));
        buffer.addVertex(matrix, maxX, minY, maxZ).setColor((int)(r*255), (int)(g*255), (int)(b*255), (int)(a*255)); buffer.addVertex(matrix, maxX, maxY, maxZ).setColor((int)(r*255), (int)(g*255), (int)(b*255), (int)(a*255));
        buffer.addVertex(matrix, minX, minY, maxZ).setColor((int)(r*255), (int)(g*255), (int)(b*255), (int)(a*255)); buffer.addVertex(matrix, minX, maxY, maxZ).setColor((int)(r*255), (int)(g*255), (int)(b*255), (int)(a*255));
    }
}
