package org.joseph.atmosforge.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.CloudStatus;
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
import org.joseph.atmosforge.client.ClientCloudData;
import org.joseph.atmosforge.client.ClientStormData;
import org.joseph.atmosforge.core.AtmoConfig;

/**
 * Volumetric cloud deck renderer.
 *
 * Renders CLOUD_NUM_LAYERS horizontal slabs stacked from CLOUD_BASE_Y to
 * CLOUD_BASE_Y + CLOUD_DEPTH. Each layer alpha follows a bell curve so the
 * combined result looks like a 3D cloud mass rather than a painted ceiling.
 * Each layer uses a slightly shifted UV to break texture repetition with depth.
 *
 * Uses Tesselator + POSITION_COLOR_TEX directly — bypassing the entity render
 * type pipeline which requires entity-specific samplers (overlay, lightmap)
 * that cloud geometry does not provide.
 *
 * Storm regions lower the cloud base and darken the deck proportional to
 * storm intensity.
 */
@EventBusSubscriber(modid = Atmosforge.MODID, value = Dist.CLIENT)
public final class CloudLayerRenderer {

    private static final float CLOUD_BASE_Y    = AtmoConfig.CLOUD_BASE_Y;
    private static final float CLOUD_DEPTH     = AtmoConfig.CLOUD_DEPTH;
    private static final int   NUM_LAYERS      = AtmoConfig.CLOUD_NUM_LAYERS;

    private static final float STORM_BASE_DROP  = 24f;
    private static final float STORM_DEPTH_EXTRA = 20f;

    private static final int TILE          = 64;
    private static final int RADIUS_BLOCKS = 1536;
    private static final int REGION_SHIFT  = 8;   // region = 256 blocks

    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(Atmosforge.MODID, "textures/misc/cloud_noise.png");

    private CloudLayerRenderer() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // AtmosForge owns cloud rendering — suppress vanilla renderClouds() which
        // fires after AFTER_SKY and would otherwise draw on top of our clouds.
        if (mc.options.cloudStatus().get() != CloudStatus.OFF) {
            mc.options.cloudStatus().set(CloudStatus.OFF);
        }

        Vec3 cam = event.getCamera().getPosition();
        int px = mc.player.blockPosition().getX();
        int pz = mc.player.blockPosition().getZ();

        int minX = px - RADIUS_BLOCKS;
        int maxX = px + RADIUS_BLOCKS;
        int minZ = pz - RADIUS_BLOCKS;
        int maxZ = pz + RADIUS_BLOCKS;

        // Drift UV slowly over time for a moving-cloud feel
        float t = (ClientCloudData.getLastGameTime() % 24000L) / 24000f;
        float driftU = t * 0.15f;
        float driftV = t * 0.08f;

        // Set up simple position-colour-tex shader — no entity samplers needed
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
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
        BufferBuilder buf = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

        for (int x = (minX / TILE) * TILE; x <= maxX; x += TILE) {
            for (int z = (minZ / TILE) * TILE; z <= maxZ; z += TILE) {

                int rx = x >> REGION_SHIFT;
                int rz = z >> REGION_SHIFT;

                ClientCloudData.CloudSample s = ClientCloudData.get(rx, rz);
                if (s == null) continue;

                float cloudiness = Mth.clamp(s.cloudiness(), 0f, 1f);
                float base       = Mth.clamp(s.baseDensity(), 0f, 1f);
                float density    = base * (0.35f + 0.65f * cloudiness);
                if (density < 0.06f) continue;

                // Fade at the edge of the render radius
                float tdx = (x + TILE * 0.5f) - px;
                float tdz = (z + TILE * 0.5f) - pz;
                float dist = (float) Math.sqrt(tdx * tdx + tdz * tdz);
                float edgeFade = 1f - Mth.clamp(
                        (dist - RADIUS_BLOCKS * 0.75f) / (RADIUS_BLOCKS * 0.25f), 0f, 1f);
                if (edgeFade < 0.01f) continue;

                // Storm region adjustments
                ClientStormData.StormSample storm = ClientStormData.get(rx, rz);
                boolean severe = storm != null && storm.intensity() > 0.2f && storm.type() >= 2;

                float baseY = CLOUD_BASE_Y - (severe ? STORM_BASE_DROP  * storm.intensity() : 0f);
                float depth = CLOUD_DEPTH  + (severe ? STORM_DEPTH_EXTRA * storm.intensity() : 0f);

                float stormDarken = severe ? storm.intensity() * 0.45f : 0f;
                float fr = (210 - cloudiness * 60f) / 255f * (1f - stormDarken);
                float fg = (215 - cloudiness * 65f) / 255f * (1f - stormDarken);
                float fb = (220 - cloudiness * 70f) / 255f * (1f - stormDarken);

                // Volumetric layers — bell-curve alpha peaks in the middle of the stack
                for (int layer = 0; layer < NUM_LAYERS; layer++) {

                    float layerFrac = (float) layer / (NUM_LAYERS - 1);
                    float bell  = (float) Math.pow(Math.sin(Math.PI * layerFrac), 1.5);
                    float alpha = Mth.clamp(density * edgeFade * bell * 0.55f, 0f, 0.65f);
                    if (alpha < 0.025f) continue;

                    float y      = baseY + layerFrac * depth;
                    float uvBias = layerFrac * 0.10f;
                    float u0 = x            / 512f + driftU + uvBias;
                    float v0 = z            / 512f + driftV + uvBias;
                    float u1 = (x + TILE)   / 512f + driftU + uvBias;
                    float v1 = (z + TILE)   / 512f + driftV + uvBias;

                    buf.addVertex(mat, x,        y, z       ).setColor(fr, fg, fb, alpha).setUv(u0, v0);
                    buf.addVertex(mat, x + TILE, y, z       ).setColor(fr, fg, fb, alpha).setUv(u1, v0);
                    buf.addVertex(mat, x + TILE, y, z + TILE).setColor(fr, fg, fb, alpha).setUv(u1, v1);
                    buf.addVertex(mat, x,        y, z + TILE).setColor(fr, fg, fb, alpha).setUv(u0, v1);
                }
            }
        }

        ps.popPose();

        MeshData mesh = buf.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
