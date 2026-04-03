package org.vmstudio.vrspelltool.core.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

public final class LumosRenderer {
    private static final ResourceLocation LUMOS_TIP_TEXTURE = new ResourceLocation("vrspelltool", "textures/lumos/lumos1.png");

    private LumosRenderer() {
    }

    public static void render(@NotNull PoseStack poseStack, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!LumosState.isActive() || minecraft.level == null || minecraft.player == null) {
            return;
        }

        Vec3 tip = LumosState.getTip();
        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 center = tip.add(0.0D, -0.022D, 0.0D).subtract(cameraPos);

        Quaternionf cameraRotation = new Quaternionf(minecraft.gameRenderer.getMainCamera().rotation());
        Vector3f right = cameraRotation.transform(new Vector3f(0.115F, 0.0F, 0.0F));
        Vector3f up = cameraRotation.transform(new Vector3f(0.0F, 0.115F, 0.0F));

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, LUMOS_TIP_TEXTURE);

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        Matrix4f matrix = poseStack.last().pose();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        addTexturedBillboard(buffer, matrix, center, new Vector3f(right).mul(1.35F), new Vector3f(up).mul(1.35F), 255, 255, 255, 84);
        addTexturedBillboard(buffer, matrix, center, new Vector3f(right).mul(0.92F), new Vector3f(up).mul(0.92F), 255, 252, 240, 128);
        addTexturedBillboard(buffer, matrix, center, right, up, 255, 250, 236, 210);

        Tesselator.getInstance().end();

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    private static void addTexturedBillboard(BufferBuilder buffer,
                                             Matrix4f matrix,
                                             Vec3 center,
                                             Vector3f right,
                                             Vector3f up,
                                             int red,
                                             int green,
                                             int blue,
                                             int alpha) {
        Vector3f c = new Vector3f((float) center.x, (float) center.y, (float) center.z);
        Vector3f v1 = new Vector3f(c).sub(right).sub(up);
        Vector3f v2 = new Vector3f(c).add(right).sub(up);
        Vector3f v3 = new Vector3f(c).add(right).add(up);
        Vector3f v4 = new Vector3f(c).sub(right).add(up);

        buffer.vertex(matrix, v1.x, v1.y, v1.z).uv(0.0F, 1.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, v2.x, v2.y, v2.z).uv(1.0F, 1.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, v3.x, v3.y, v3.z).uv(1.0F, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, v4.x, v4.y, v4.z).uv(0.0F, 0.0F).color(red, green, blue, alpha).endVertex();
    }
}
