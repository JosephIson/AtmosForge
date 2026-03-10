package org.joseph.atmosforge.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joseph.atmosforge.Atmosforge;
import org.joseph.atmosforge.client.ClientStormData;

@EventBusSubscriber(modid = Atmosforge.MODID, value = Dist.CLIENT)
public final class StormCloudRenderer {

    private static final int   REGION_SHIFT    = 8;
    private static final int   BASE_Y          = 130;
    private static final int   MAX_VIEW_RADIUS = 2200;

    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(Atmosforge.MODID, "textures/misc/cloud_noise.png");

    private StormCloudRenderer() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Vec3 cam = event.getCamera().getPosition();

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, TEX);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f mat = ps.last().pose();

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        for (ClientStormData.StormSample s : ClientStormData.getAll().values()) {

            if (s.intensity() < 0.08f) continue;

            float centerX = s.regionX() << REGION_SHIFT;
            float centerZ = s.regionZ() << REGION_SHIFT;

            double dx = centerX - cam.x;
            double dz = centerZ - cam.z;
            if (dx * dx + dz * dz > (double) MAX_VIEW_RADIUS * MAX_VIEW_RADIUS) continue;

            float intensity   = Mth.clamp(s.intensity(), 0f, 1f);
            float height      = 110f + intensity * 170f;
            float baseRadius  = 120f + intensity * 240f;
            float shear       = Mth.clamp(s.shear(), 0f, 1f);
            float tiltStrength = shear * 70f;

            float windX = s.upperWindX();
            float windZ = s.upperWindZ();
            float windMag = Mth.sqrt(windX * windX + windZ * windZ);
            if (windMag > 0.0001f) { windX /= windMag; windZ /= windMag; }

            renderVolumetricTower(buf, mat,
                    centerX, centerZ, BASE_Y, height, baseRadius,
                    windX * tiltStrength, windZ * tiltStrength, intensity);
        }

        ps.popPose();

        MeshData mesh = buf.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void renderVolumetricTower(BufferBuilder buf, Matrix4f mat,
                                              float cx, float cz,
                                              float baseY, float height, float baseRadius,
                                              float tiltX, float tiltZ,
                                              float intensity) {

        int radialSegments = 16;
        int verticalLayers = 6;

        for (int layer = 0; layer < verticalLayers; layer++) {

            float layerFrac = (float) layer / verticalLayers;
            float y0 = baseY + height * layerFrac;
            float y1 = baseY + height * (layerFrac + 1f / verticalLayers);
            float radius = baseRadius * (1f - layerFrac * 0.45f);

            float densityFade = 1f - layerFrac * 0.6f;
            float alpha = (0.28f + intensity * 0.45f) * densityFade;

            float layerTiltX = tiltX * layerFrac;
            float layerTiltZ = tiltZ * layerFrac;

            float fr = 160f / 255f;
            float fg = 165f / 255f;
            float fb = 170f / 255f;

            for (int i = 0; i < radialSegments; i++) {

                float angle0 = (float) (2 * Math.PI * i       / radialSegments);
                float angle1 = (float) (2 * Math.PI * (i + 1) / radialSegments);

                float x0 = cx + Mth.cos(angle0) * radius;
                float z0 = cz + Mth.sin(angle0) * radius;
                float x1 = cx + Mth.cos(angle1) * radius;
                float z1 = cz + Mth.sin(angle1) * radius;

                float tx = cx + layerTiltX;
                float tz = cz + layerTiltZ;

                // bottom-left, bottom-right, top-right, top-left of strip
                buf.addVertex(mat, x0,             y0, z0            ).setUv(0, 0).setColor(fr, fg, fb, alpha);
                buf.addVertex(mat, x1,             y0, z1            ).setUv(1, 0).setColor(fr, fg, fb, alpha);
                buf.addVertex(mat, tx+(x1-cx), y1, tz+(z1-cz)).setUv(1, 1).setColor(fr, fg, fb, alpha);
                buf.addVertex(mat, tx+(x0-cx), y1, tz+(z0-cz)).setUv(0, 1).setColor(fr, fg, fb, alpha);
            }
        }
    }
}