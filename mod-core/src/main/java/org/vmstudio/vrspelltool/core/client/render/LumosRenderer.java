package org.vmstudio.vrspelltool.core.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public final class LumosRenderer {
    private LumosRenderer() {
    }

    public static void render(@NotNull PoseStack poseStack, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!LumosState.isActive() || minecraft.level == null || minecraft.player == null) {
            return;
        }

        Vec3 tip = LumosState.getTip();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 center = tip.subtract(camera);
        float time = (minecraft.level.getGameTime() + partialTicks) * 0.12F;
        float pulse = 0.92F + Mth.sin(time) * 0.04F;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = poseStack.last().pose();
        drawGlowCross(matrix, center, 0.06F * pulse, 255, 244, 176, 190);
        drawGlowCross(matrix, center, 0.13F * pulse, 255, 232, 150, 84);
        drawGlowCross(matrix, center, 0.22F * pulse, 255, 225, 120, 34);

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    private static void drawGlowCross(Matrix4f matrix,
                                      Vec3 center,
                                      float radius,
                                      int red,
                                      int green,
                                      int blue,
                                      int alpha) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        addQuadXY(buffer, matrix, center, radius, red, green, blue, alpha);
        addQuadXZ(buffer, matrix, center, radius, red, green, blue, alpha);
        addQuadYZ(buffer, matrix, center, radius, red, green, blue, alpha);

        tesselator.end();
    }

    private static void addQuadXY(BufferBuilder buffer,
                                  Matrix4f matrix,
                                  Vec3 center,
                                  float radius,
                                  int red,
                                  int green,
                                  int blue,
                                  int alpha) {
        float x = (float) center.x;
        float y = (float) center.y;
        float z = (float) center.z;

        buffer.vertex(matrix, x - radius, y - radius, z).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + radius, y - radius, z).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + radius, y + radius, z).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x - radius, y + radius, z).color(red, green, blue, alpha).endVertex();
    }

    private static void addQuadXZ(BufferBuilder buffer,
                                  Matrix4f matrix,
                                  Vec3 center,
                                  float radius,
                                  int red,
                                  int green,
                                  int blue,
                                  int alpha) {
        float x = (float) center.x;
        float y = (float) center.y;
        float z = (float) center.z;

        buffer.vertex(matrix, x - radius, y, z - radius).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + radius, y, z - radius).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + radius, y, z + radius).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x - radius, y, z + radius).color(red, green, blue, alpha).endVertex();
    }

    private static void addQuadYZ(BufferBuilder buffer,
                                  Matrix4f matrix,
                                  Vec3 center,
                                  float radius,
                                  int red,
                                  int green,
                                  int blue,
                                  int alpha) {
        float x = (float) center.x;
        float y = (float) center.y;
        float z = (float) center.z;

        buffer.vertex(matrix, x, y - radius, z - radius).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x, y - radius, z + radius).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x, y + radius, z + radius).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x, y + radius, z - radius).color(red, green, blue, alpha).endVertex();
    }
}
