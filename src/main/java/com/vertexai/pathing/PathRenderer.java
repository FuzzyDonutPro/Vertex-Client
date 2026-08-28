package com.vertexai.pathing;

import com.mojang.blaze3d.systems.RenderSystem;
import com.vertexai.event.EventManager;
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
        EventManager.registerLevelRenderEventSafe(ctx -> PathRenderer.render(new com.vertexai.util.WorldRenderContextWrapper(ctx)));
    }

    private static void render(WorldRenderContextWrapper context) {
        if (targetBlock == null || exactTarget == null) return;

        Minecraft client = Minecraft.getInstance();
        if (Minecraft.getInstance().player == null) return;

        net.minecraft.client.Camera camera = context.camera();
        Vec3 cameraPos = camera.position();
        PoseStack matrices = context.matrixStack();
        matrices.pushPose();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f matrix = matrices.last().pose();

        com.mojang.blaze3d.vertex.Tesselator tessellator = com.mojang.blaze3d.vertex.Tesselator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        
        // RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        // RenderSystem.disableDepthTest();
        // RenderSystem.disableCull();
        // RenderSystem.enableBlend();

        // 1. Draw bounding box around target block (Red)
        AABB blockAABB = new AABB(targetBlock).inflate(0.01);
        drawAABB(buffer, matrix, blockAABB, 1.0f, 0.0f, 0.0f, 1.0f);

        // 2. Draw 1/16th blue box at random sub-coordinate target
        double s = 1.0 / 16.0 / 2.0; // half size
        AABB smallAABB = new AABB(
                exactTarget.x() - s, exactTarget.y() - s, exactTarget.z() - s,
                exactTarget.x() + s, exactTarget.y() + s, exactTarget.z() + s
        );
        drawAABB(buffer, matrix, smallAABB, 0.0f, 0.0f, 1.0f, 1.0f);

        // 3. Draw path line (Theme Color)
        if (!currentPath.isEmpty()) {
            int theme = com.vertexai.Vertex.config().gui.getThemeColorInt();
            int r = (theme >> 16) & 0xFF;
            int g = (theme >> 8) & 0xFF;
            int b = theme & 0xFF;
            
            Vec3 start = Minecraft.getInstance().player.position();
            buffer.addVertex(matrix, (float)start.x, (float)start.y + 1.0f, (float)start.z).setColor(r, g, b, 255);
            
            for (BlockPos pos : currentPath) {
                buffer.addVertex(matrix, pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f).setColor(r, g, b, 255);
                buffer.addVertex(matrix, pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f).setColor(r, g, b, 255);
            }
            
            buffer.addVertex(matrix, (float)exactTarget.x(), (float)exactTarget.y(), (float)exactTarget.z()).setColor(r, g, b, 255);
        }

        // BufferUploader is removed in 1.21.11, need to use RenderType or VertexConsumer natively
        // com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(buffer.buildOrThrow());

        // RenderSystem.enableDepthTest();
        // RenderSystem.enableCull();
        // RenderSystem.disableBlend();
        matrices.popPose();
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
