package org.joseph.atmosforge.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joseph.atmosforge.Atmosforge;
import org.joseph.atmosforge.client.ClientTornadoData;
import org.joseph.atmosforge.client.ClientTornadoData.TornadoSample;

/**
 * Renders active tornadoes as rotating funnel cones descending from the
 * cloud base toward the ground.
 *
 * Geometry overview:
 *   - RING_LAYERS horizontal rings stacked from TOP_Y down to the funnel tip.
 *   - Each ring is offset in rotation by TWIST_PER_RING radians so the
 *     overall shape spirals, mimicking the characteristic twisted funnel.
 *   - A master spin driven by game time keeps the whole funnel rotating.
 *   - Colour darkens and goes brownish toward the tip (debris-laden air).
 *   - A flat debris disk renders at the bottom of the extended funnel.
 */
@EventBusSubscriber(modid = Atmosforge.MODID, value = Dist.CLIENT)
public final class TornadoRenderer {

    // Y of the cloud base where the funnel connects
    private static final float TOP_Y = 148f;

    // Maximum funnel height from cloud base to ground proxy (blocks)
    private static final float FUNNEL_HEIGHT = 132f;

    // Funnel top radius (at cloud base) — scales with intensity
    private static final float TOP_RADIUS_BASE = 20f;
    private static final float TOP_RADIUS_SCALE = 12f;

    // Funnel tip radius — scales with intensity
    private static final float BOTTOM_RADIUS_BASE = 2f;
    private static final float BOTTOM_RADIUS_SCALE = 4f;

    // Geometry resolution
    private static final int RING_LAYERS = 14;
    private static final int ANGLE_SEGS = 24;

    // How much extra twist per ring (radians) — gives the spiral shape
    private static final float TWIST_PER_RING = 0.28f;

    // Rotation speed in radians per game tick
    private static final float SPIN_SPEED = 0.045f;

    // Debris disk at the funnel tip
    private static final float DEBRIS_EXTRA_RADIUS = 18f;

    // Don't render tornados beyond this distance (blocks)
    private static final float MAX_VIEW_DIST = 2500f;

    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(Atmosforge.MODID, "textures/misc/cloud_noise.png");

    private TornadoRenderer() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        var samples = ClientTornadoData.getAll();
        if (samples.isEmpty()) return;

        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();

        // Master spin angle driven by game time (client-side, no network jitter)
        float gameTime = mc.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(true);
        float masterSpin = gameTime * SPIN_SPEED;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(TEX));

        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);

        for (TornadoSample s : samples) {

            if (s.intensity() < 0.02f) continue;

            float dx = s.worldX() - (float) cam.x;
            float dz = s.worldZ() - (float) cam.z;
            if (dx * dx + dz * dz > MAX_VIEW_DIST * MAX_VIEW_DIST) continue;

            renderFunnel(ps, vc, s, masterSpin);
        }

        ps.popPose();
        buffers.endBatch();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    // -------------------------------------------------------------------------

    private static void renderFunnel(PoseStack ps,
                                     VertexConsumer vc,
                                     TornadoSample s,
                                     float masterSpin) {

        float intensity = Mth.clamp(s.intensity(), 0f, 1f);

        // How far down the funnel currently extends (0 = nothing, 1 = full)
        // FORMING: stage 0 — intensity ramps from 0→1, so use it directly
        // MATURE:  stage 1 — always full
        // DISSIPATING: stage 2 — intensity decays
        float extension = intensity;

        float currentHeight = FUNNEL_HEIGHT * extension;
        float bottomY = TOP_Y - currentHeight;

        float topRadius = TOP_RADIUS_BASE + intensity * TOP_RADIUS_SCALE;
        float bottomRadius = BOTTOM_RADIUS_BASE + intensity * BOTTOM_RADIUS_SCALE;

        float cx = s.worldX();
        float cz = s.worldZ();

        int overlay = OverlayTexture.NO_OVERLAY;
        int light = LightTexture.FULL_BRIGHT;

        // --- Draw cone wall rings ---
        for (int ring = 0; ring < RING_LAYERS; ring++) {

            float t0 = (float) ring / RING_LAYERS;
            float t1 = (float) (ring + 1) / RING_LAYERS;

            float y0 = TOP_Y - currentHeight * t0;
            float y1 = TOP_Y - currentHeight * t1;

            float r0 = Mth.lerp(t0, topRadius, bottomRadius);
            float r1 = Mth.lerp(t1, topRadius, bottomRadius);

            // Twist: each ring rotates further by TWIST_PER_RING
            float rot0 = masterSpin + ring * TWIST_PER_RING;
            float rot1 = masterSpin + (ring + 1) * TWIST_PER_RING;

            // Colour: grey at top, darker brownish at bottom
            int grey0 = (int) Mth.lerp(t0, 170f, 80f);
            int grey1 = (int) Mth.lerp(t1, 170f, 80f);
            int red0 = (int) Mth.lerp(t0, 160f, 100f);
            int red1 = (int) Mth.lerp(t1, 160f, 100f);
            int blue0 = (int) Mth.lerp(t0, 175f, 72f);
            int blue1 = (int) Mth.lerp(t1, 175f, 72f);

            float alpha = Mth.clamp(0.55f + intensity * 0.30f, 0f, 0.85f);
            int a = (int) (alpha * 255f);

            for (int seg = 0; seg < ANGLE_SEGS; seg++) {

                float angle0 = (float) (2 * Math.PI * seg / ANGLE_SEGS);
                float angle1 = (float) (2 * Math.PI * (seg + 1) / ANGLE_SEGS);

                // Ring i vertices (top of this strip)
                float ax = cx + Mth.cos(angle0 + rot0) * r0;
                float az = cz + Mth.sin(angle0 + rot0) * r0;
                float bx = cx + Mth.cos(angle1 + rot0) * r0;
                float bz = cz + Mth.sin(angle1 + rot0) * r0;

                // Ring i+1 vertices (bottom of this strip)
                float cx2 = cx + Mth.cos(angle1 + rot1) * r1;
                float cz2 = cz + Mth.sin(angle1 + rot1) * r1;
                float dx2 = cx + Mth.cos(angle0 + rot1) * r1;
                float dz2 = cz + Mth.sin(angle0 + rot1) * r1;

                float u0 = (float) seg / ANGLE_SEGS;
                float u1 = (float) (seg + 1) / ANGLE_SEGS;

                var pose = ps.last();

                vc.addVertex(pose, ax, y0, az).setUv(u0, t0).setColor(red0, grey0, blue0, a).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
                vc.addVertex(pose, bx, y0, bz).setUv(u1, t0).setColor(red0, grey0, blue0, a).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
                vc.addVertex(pose, cx2, y1, cz2).setUv(u1, t1).setColor(red1, grey1, blue1, a).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
                vc.addVertex(pose, dx2, y1, dz2).setUv(u0, t1).setColor(red1, grey1, blue1, a).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
            }
        }

        // --- Debris disk at funnel tip ---
        if (extension > 0.15f) {
            renderDebrisDisk(ps, vc, cx, bottomY, cz,
                    bottomRadius + DEBRIS_EXTRA_RADIUS, masterSpin, intensity, overlay, light);
        }
    }

    private static void renderDebrisDisk(PoseStack ps,
                                         VertexConsumer vc,
                                         float cx, float y, float cz,
                                         float radius,
                                         float spin,
                                         float intensity,
                                         int overlay,
                                         int light) {

        int debrisSegs = 20;
        float alpha = Mth.clamp(0.35f + intensity * 0.25f, 0f, 0.65f);
        int a = (int) (alpha * 255f);

        // Dark brownish debris colour
        int r = 90, g = 70, b = 60;

        var pose = ps.last();

        for (int i = 0; i < debrisSegs; i++) {

            float angle0 = (float) (2 * Math.PI * i / debrisSegs) + spin * 0.6f;
            float angle1 = (float) (2 * Math.PI * (i + 1) / debrisSegs) + spin * 0.6f;

            float innerR = radius * 0.3f;
            float outerR = radius;

            float ix0 = cx + Mth.cos(angle0) * innerR;
            float iz0 = cz + Mth.sin(angle0) * innerR;
            float ox0 = cx + Mth.cos(angle0) * outerR;
            float oz0 = cz + Mth.sin(angle0) * outerR;
            float ix1 = cx + Mth.cos(angle1) * innerR;
            float iz1 = cz + Mth.sin(angle1) * innerR;
            float ox1 = cx + Mth.cos(angle1) * outerR;
            float oz1 = cz + Mth.sin(angle1) * outerR;

            vc.addVertex(pose, ix0, y, iz0).setUv(0, 0).setColor(r, g, b, a).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
            vc.addVertex(pose, ox0, y, oz0).setUv(1, 0).setColor(r, g, b, a).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
            vc.addVertex(pose, ox1, y, oz1).setUv(1, 1).setColor(r, g, b, a).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
            vc.addVertex(pose, ix1, y, iz1).setUv(0, 1).setColor(r, g, b, a).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        }
    }
}