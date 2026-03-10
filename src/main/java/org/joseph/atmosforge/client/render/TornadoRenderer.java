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
import org.joseph.atmosforge.client.ClientTornadoData;
import org.joseph.atmosforge.client.ClientTornadoData.TornadoSample;

/**
 * Renders active tornadoes as rotating funnel cones descending from the
 * cloud base toward the ground, with a swirling debris disk at the tip.
 */
@EventBusSubscriber(modid = Atmosforge.MODID, value = Dist.CLIENT)
public final class TornadoRenderer {

    private static final float TOP_Y              = 148f;
    private static final float FUNNEL_HEIGHT      = 132f;
    private static final float TOP_RADIUS_BASE    = 20f;
    private static final float TOP_RADIUS_SCALE   = 12f;
    private static final float BOTTOM_RADIUS_BASE = 2f;
    private static final float BOTTOM_RADIUS_SCALE = 4f;

    private static final int   RING_LAYERS    = 14;
    private static final int   ANGLE_SEGS     = 24;
    private static final float TWIST_PER_RING = 0.28f;
    private static final float SPIN_SPEED     = 0.045f;
    private static final float DEBRIS_EXTRA_RADIUS = 18f;
    private static final float MAX_VIEW_DIST  = 2500f;

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
        float gameTime = mc.level.getGameTime()
                + event.getPartialTick().getGameTimeDeltaPartialTick(true);
        float masterSpin = gameTime * SPIN_SPEED;

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

        for (TornadoSample s : samples) {
            if (s.intensity() < 0.02f) continue;
            float dx = s.worldX() - (float) cam.x;
            float dz = s.worldZ() - (float) cam.z;
            if (dx * dx + dz * dz > MAX_VIEW_DIST * MAX_VIEW_DIST) continue;
            renderFunnel(buf, mat, s, masterSpin);
        }

        ps.popPose();

        MeshData mesh = buf.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void renderFunnel(BufferBuilder buf, Matrix4f mat,
                                     TornadoSample s, float masterSpin) {

        float intensity     = Mth.clamp(s.intensity(), 0f, 1f);
        float currentHeight = FUNNEL_HEIGHT * intensity;
        float bottomY       = TOP_Y - currentHeight;
        float topRadius     = TOP_RADIUS_BASE    + intensity * TOP_RADIUS_SCALE;
        float bottomRadius  = BOTTOM_RADIUS_BASE + intensity * BOTTOM_RADIUS_SCALE;
        float cx = s.worldX();
        float cz = s.worldZ();

        for (int ring = 0; ring < RING_LAYERS; ring++) {

            float t0 = (float) ring       / RING_LAYERS;
            float t1 = (float) (ring + 1) / RING_LAYERS;

            float y0 = TOP_Y - currentHeight * t0;
            float y1 = TOP_Y - currentHeight * t1;
            float r0 = Mth.lerp(t0, topRadius, bottomRadius);
            float r1 = Mth.lerp(t1, topRadius, bottomRadius);

            float rot0 = masterSpin + ring       * TWIST_PER_RING;
            float rot1 = masterSpin + (ring + 1) * TWIST_PER_RING;

            float alpha = Mth.clamp(0.55f + intensity * 0.30f, 0f, 0.85f);

            // Colour grades grey at top → dark brown at bottom
            float fr0 = Mth.lerp(t0, 160f, 100f) / 255f;
            float fg0 = Mth.lerp(t0, 170f,  80f) / 255f;
            float fb0 = Mth.lerp(t0, 175f,  72f) / 255f;
            float fr1 = Mth.lerp(t1, 160f, 100f) / 255f;
            float fg1 = Mth.lerp(t1, 170f,  80f) / 255f;
            float fb1 = Mth.lerp(t1, 175f,  72f) / 255f;

            for (int seg = 0; seg < ANGLE_SEGS; seg++) {

                float a0 = (float) (2 * Math.PI * seg       / ANGLE_SEGS);
                float a1 = (float) (2 * Math.PI * (seg + 1) / ANGLE_SEGS);

                float ax = cx + Mth.cos(a0 + rot0) * r0;
                float az = cz + Mth.sin(a0 + rot0) * r0;
                float bx = cx + Mth.cos(a1 + rot0) * r0;
                float bz = cz + Mth.sin(a1 + rot0) * r0;
                float cx2 = cx + Mth.cos(a1 + rot1) * r1;
                float cz2 = cz + Mth.sin(a1 + rot1) * r1;
                float dx2 = cx + Mth.cos(a0 + rot1) * r1;
                float dz2 = cz + Mth.sin(a0 + rot1) * r1;

                float u0 = (float) seg       / ANGLE_SEGS;
                float u1 = (float) (seg + 1) / ANGLE_SEGS;

                buf.addVertex(mat, ax,  y0, az ).setUv(u0, t0).setColor(fr0, fg0, fb0, alpha);
                buf.addVertex(mat, bx,  y0, bz ).setUv(u1, t0).setColor(fr0, fg0, fb0, alpha);
                buf.addVertex(mat, cx2, y1, cz2).setUv(u1, t1).setColor(fr1, fg1, fb1, alpha);
                buf.addVertex(mat, dx2, y1, dz2).setUv(u0, t1).setColor(fr1, fg1, fb1, alpha);
            }
        }

        // Debris disk at funnel tip
        if (intensity > 0.15f) {
            float debrisR = bottomRadius + DEBRIS_EXTRA_RADIUS;
            float innerR  = debrisR * 0.3f;
            float alpha   = Mth.clamp(0.35f + intensity * 0.25f, 0f, 0.65f);
            float spin    = masterSpin * 0.6f;
            int   segs    = 20;

            for (int i = 0; i < segs; i++) {
                float a0 = (float) (2 * Math.PI * i       / segs) + spin;
                float a1 = (float) (2 * Math.PI * (i + 1) / segs) + spin;

                buf.addVertex(mat, cx + Mth.cos(a0) * innerR, bottomY, cz + Mth.sin(a0) * innerR).setUv(0, 0).setColor(90/255f, 70/255f, 60/255f, alpha);
                buf.addVertex(mat, cx + Mth.cos(a0) * debrisR, bottomY, cz + Mth.sin(a0) * debrisR).setUv(1, 0).setColor(90/255f, 70/255f, 60/255f, alpha);
                buf.addVertex(mat, cx + Mth.cos(a1) * debrisR, bottomY, cz + Mth.sin(a1) * debrisR).setUv(1, 1).setColor(90/255f, 70/255f, 60/255f, alpha);
                buf.addVertex(mat, cx + Mth.cos(a1) * innerR, bottomY, cz + Mth.sin(a1) * innerR).setUv(0, 1).setColor(90/255f, 70/255f, 60/255f, alpha);
            }
        }
    }
}