package com.vertexai.pathing;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import com.mojang.blaze3d.vertex.PoseStack;
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
        WorldRenderEvents.LAST.register(PathRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        if (targetBlock == null || exactTarget == null) return;

        Minecraft client = Minecraft.getInstance();
        if (Minecraft.getInstance().player == null) return;

        Camera camera = context.camera();
        Vec3 cameraPos = camera.position();
        PoseStack matrices = context.matrixStack();
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();

        // 1. Draw bounding box around target block (Red)
        AABB blockAABB = new AABB(targetBlock).expand(0.01);
        drawAABB(buffer, matrix, blockAABB, 1.0f, 0.0f, 0.0f, 1.0f);

        // 2. Draw 1/16th blue box at random sub-coordinate target
        double s = 1.0 / 16.0 / 2.0; // half size
        AABB smallAABB = new AABB(
                exactTarget.x - s, exactTarget.y - s, exactTarget.z - s,
                exactTarget.x + s, exactTarget.y + s, exactTarget.z + s
        );
        drawAABB(buffer, matrix, smallAABB, 0.0f, 0.0f, 1.0f, 1.0f);

        // 3. Draw path line (Green)
        if (!currentPath.isEmpty()) {
            Vec3 start = Minecraft.getInstance().player.position();
            buffer.vertex(matrix, (float)start.x, (float)start.y + 1.0f, (float)start.z).color(0.0f, 1.0f, 0.0f, 1.0f);
            
            for (BlockPos pos : currentPath) {
                buffer.vertex(matrix, pos.x + 0.5f, pos.y + 0.5f, pos.z + 0.5f).color(0.0f, 1.0f, 0.0f, 1.0f);
                buffer.vertex(matrix, pos.x + 0.5f, pos.y + 0.5f, pos.z + 0.5f).color(0.0f, 1.0f, 0.0f, 1.0f);
            }
            
            buffer.vertex(matrix, (float)exactTarget.x, (float)exactTarget.y, (float)exactTarget.z).color(0.0f, 1.0f, 0.0f, 1.0f);
        }

        BufferRenderer.drawWithShader(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private static void drawAABB(BufferBuilder buffer, Matrix4f matrix, AABB box, float r, float g, float b, float a) {
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        // Bottom
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a); buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a); buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a); buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a); buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);

        // Top
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a); buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a); buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a); buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a); buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);

        // Pillars
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a); buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a); buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a); buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a); buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
    }
}
