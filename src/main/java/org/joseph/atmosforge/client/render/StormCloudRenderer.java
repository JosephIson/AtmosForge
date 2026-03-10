package org.joseph.atmosforge.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joseph.atmosforge.Atmosforge;
import org.joseph.atmosforge.client.ClientStormData;

@EventBusSubscriber(modid = Atmosforge.MODID, value = Dist.CLIENT)
public final class StormCloudRenderer {

    private static final int REGION_SHIFT = 8;
    private static final int BASE_Y = 130;
    private static final int MAX_VIEW_RADIUS = 2200;

    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(Atmosforge.MODID, "textures/misc/cloud_noise.png");

    private StormCloudRenderer() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        PoseStack ps = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderTexture(0, TEX);

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(TEX));

        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);

        for (ClientStormData.StormSample s : ClientStormData.getAll().values()) {

            if (s.intensity() < 0.08f) continue;

            int centerX = s.regionX() << REGION_SHIFT;
            int centerZ = s.regionZ() << REGION_SHIFT;

            double dx = centerX - cam.x;
            double dz = centerZ - cam.z;
            if (dx * dx + dz * dz > MAX_VIEW_RADIUS * MAX_VIEW_RADIUS) continue;

            float intensity = Mth.clamp(s.intensity(), 0f, 1f);

            float height = 110f + intensity * 170f;
            float baseRadius = 120f + intensity * 240f;

            float shear = Mth.clamp(s.shear(), 0f, 1f);
            float tiltStrength = shear * 70f;

            float windX = s.upperWindX();
            float windZ = s.upperWindZ();

            float windMag = Mth.sqrt(windX * windX + windZ * windZ);
            if (windMag > 0.0001f) {
                windX /= windMag;
                windZ /= windMag;
            }

            float tiltX = windX * tiltStrength;
            float tiltZ = windZ * tiltStrength;

            renderVolumetricTower(ps, vc,
                    centerX, centerZ,
                    BASE_Y,
                    height,
                    baseRadius,
                    tiltX, tiltZ,
                    intensity);
        }

        ps.popPose();
        buffers.endBatch();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void renderVolumetricTower(PoseStack ps,
                                              VertexConsumer vc,
                                              float cx, float cz,
                                              float baseY,
                                              float height,
                                              float baseRadius,
                                              float tiltX,
                                              float tiltZ,
                                              float intensity) {

        int radialSegments = 16;
        int verticalLayers = 6;

        int overlay = OverlayTexture.NO_OVERLAY;
        int light = LightTexture.FULL_BRIGHT;

        for (int layer = 0; layer < verticalLayers; layer++) {

            float layerFrac = (float) layer / verticalLayers;
            float y0 = baseY + height * layerFrac;
            float y1 = baseY + height * (layerFrac + (1f / verticalLayers));

            float radius = baseRadius * (1.0f - layerFrac * 0.45f);

            float densityFade = 1.0f - layerFrac * 0.6f;
            float alpha = (0.28f + intensity * 0.45f) * densityFade;

            float layerTiltX = tiltX * layerFrac;
            float layerTiltZ = tiltZ * layerFrac;

            for (int i = 0; i < radialSegments; i++) {

                float angle0 = (float) (2 * Math.PI * i / radialSegments);
                float angle1 = (float) (2 * Math.PI * (i + 1) / radialSegments);

                float x0 = cx + Mth.cos(angle0) * radius;
                float z0 = cz + Mth.sin(angle0) * radius;

                float x1 = cx + Mth.cos(angle1) * radius;
                float z1 = cz + Mth.sin(angle1) * radius;

                float tx = cx + layerTiltX;
                float tz = cz + layerTiltZ;

                drawQuad(ps, vc,
                        x0, y0, z0,
                        tx + (x1 - cx), y1, tz + (z1 - cz),
                        tx + (x0 - cx), y1, tz + (z0 - cz),
                        x1, y0, z1,
                        alpha,
                        overlay,
                        light);
            }
        }
    }

    private static void drawQuad(PoseStack ps,
                                 VertexConsumer vc,
                                 float x0, float y0, float z0,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float alpha,
                                 int overlay,
                                 int light) {

        int a = (int)(Mth.clamp(alpha, 0f, 1f) * 255f);

        int r = 160;
        int g = 165;
        int b = 170;

        var pose = ps.last();

        vc.addVertex(pose, x0, y0, z0).setUv(0, 0).setColor(r, g, b, a).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, x1, y1, z1).setUv(1, 0).setColor(r, g, b, a).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, x2, y2, z2).setUv(1, 1).setColor(r, g, b, a).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, x3, y3, z3).setUv(0, 1).setColor(r, g, b, a).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
    }
}
