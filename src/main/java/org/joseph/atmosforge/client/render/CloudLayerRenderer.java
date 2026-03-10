package org.joseph.atmosforge.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
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
import org.joseph.atmosforge.client.ClientCloudData;
import org.joseph.atmosforge.client.ClientStormData;
import org.joseph.atmosforge.core.AtmoConfig;

/**
 * Volumetric cloud deck renderer.
 *
 * Instead of a single flat slab, CLOUD_NUM_LAYERS horizontal quads are stacked
 * from CLOUD_BASE_Y to CLOUD_BASE_Y + CLOUD_DEPTH. Each layer is given an alpha
 * that follows a raised-cosine bell curve (peaking at the middle of the stack),
 * so the combined result looks like a three-dimensional cloud mass rather than a
 * painted ceiling. Each layer also uses a slightly shifted UV to break repetition
 * and create the illusion of depth when the camera moves vertically.
 *
 * Storm regions (SUPERCELL, MCS, THUNDERSTORMS) lower the cloud base and darken
 * the colour using data from ClientStormData.
 */
@EventBusSubscriber(modid = Atmosforge.MODID, value = Dist.CLIENT)
public final class CloudLayerRenderer {

    // Bottom of the volumetric cloud deck (blocks)
    private static final float CLOUD_BASE_Y = AtmoConfig.CLOUD_BASE_Y;
    private static final float CLOUD_DEPTH   = AtmoConfig.CLOUD_DEPTH;
    private static final int   NUM_LAYERS    = AtmoConfig.CLOUD_NUM_LAYERS;

    // How much the cloud base is pushed down for severe storm regions (blocks)
    private static final float STORM_BASE_DROP = 24f;
    // How much the cloud depth expands for severe storm regions (blocks)
    private static final float STORM_DEPTH_EXTRA = 20f;

    // Tile sizing (world blocks)
    private static final int TILE = 64;

    // How far to render clouds from player
    private static final int RADIUS_BLOCKS = 1536;

    // Region = 256 blocks (16 chunks * 16 blocks)
    private static final int REGION_SHIFT_BLOCKS = 8;

    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(Atmosforge.MODID, "textures/misc/cloud_noise.png");

    private CloudLayerRenderer() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // AtmosForge replaces vanilla cloud rendering entirely.
        // Vanilla renderClouds() is called after AFTER_SKY fires, so without
        // this it would draw on top of and hide our custom clouds.
        if (mc.options.cloudStatus().get() != CloudStatus.OFF) {
            mc.options.cloudStatus().set(CloudStatus.OFF);
        }

        PoseStack ps = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();

        double camX = cam.x;
        double camY = cam.y;
        double camZ = cam.z;

        int px = mc.player.blockPosition().getX();
        int pz = mc.player.blockPosition().getZ();

        int minX = px - RADIUS_BLOCKS;
        int maxX = px + RADIUS_BLOCKS;
        int minZ = pz - RADIUS_BLOCKS;
        int maxZ = pz + RADIUS_BLOCKS;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(TEX));

        ps.pushPose();
        ps.translate(-camX, -camY, -camZ);

        // Slight world-space drift with time
        float t = (ClientCloudData.getLastGameTime() % 24000L) / 24000f;
        float driftU = t * 0.15f;
        float driftV = t * 0.08f;

        int overlay = OverlayTexture.NO_OVERLAY;
        int light = LightTexture.FULL_BRIGHT;
        float nx = 0f, ny = -1f, nz = 0f;

        for (int x = (minX / TILE) * TILE; x <= maxX; x += TILE) {
            for (int z = (minZ / TILE) * TILE; z <= maxZ; z += TILE) {

                int rx = x >> REGION_SHIFT_BLOCKS;
                int rz = z >> REGION_SHIFT_BLOCKS;

                ClientCloudData.CloudSample s = ClientCloudData.get(rx, rz);
                if (s == null) continue;

                float cloudiness = Mth.clamp(s.cloudiness(), 0f, 1f);
                float base = Mth.clamp(s.baseDensity(), 0f, 1f);

                float density = base * (0.35f + 0.65f * cloudiness);
                if (density < 0.06f) continue;

                // Distance fade at render radius edge
                float tdx = (x + TILE * 0.5f) - px;
                float tdz = (z + TILE * 0.5f) - pz;
                float dist = (float) Math.sqrt(tdx * tdx + tdz * tdz);
                float edgeFade = 1.0f - Mth.clamp(
                        (dist - RADIUS_BLOCKS * 0.75f) / (RADIUS_BLOCKS * 0.25f), 0f, 1f);
                if (edgeFade < 0.01f) continue;

                // --- Storm region adjustments ---
                ClientStormData.StormSample storm = ClientStormData.get(rx, rz);
                boolean severe = storm != null && storm.intensity() > 0.2f
                        && (storm.type() >= 2); // THUNDERSTORMS(2), SUPERCELL(3), MCS(4), EXTRATROPICAL(5)

                float baseY  = CLOUD_BASE_Y  - (severe ? STORM_BASE_DROP * storm.intensity() : 0f);
                float depth  = CLOUD_DEPTH   + (severe ? STORM_DEPTH_EXTRA * storm.intensity() : 0f);

                // Darken colour for storm regions
                float stormDarken = severe ? storm.intensity() * 0.45f : 0f;
                int r = (int) ((210 - cloudiness * 60f) * (1f - stormDarken));
                int g = (int) ((215 - cloudiness * 65f) * (1f - stormDarken));
                int b = (int) ((220 - cloudiness * 70f) * (1f - stormDarken));

                // --- Volumetric layer loop ---
                for (int layer = 0; layer < NUM_LAYERS; layer++) {

                    // layerFrac: 0 = bottom slab, 1 = top slab
                    float layerFrac = (float) layer / (NUM_LAYERS - 1);

                    // Bell curve: alpha peaks in the middle of the stack
                    float bell = (float) Math.pow(
                            Math.sin(Math.PI * layerFrac), 1.5);

                    float alpha = Mth.clamp(density * edgeFade * bell * 0.50f, 0f, 0.60f);
                    if (alpha < 0.025f) continue;

                    int a = (int) (alpha * 255f);

                    float y = baseY + layerFrac * depth;

                    // Slight per-layer UV offset breaks up the flat-texture look
                    float uvBias = layerFrac * 0.10f;
                    float u0 = (x / 512f) + driftU + uvBias;
                    float v0 = (z / 512f) + driftV + uvBias;
                    float u1 = ((x + TILE) / 512f) + driftU + uvBias;
                    float v1 = ((z + TILE) / 512f) + driftV + uvBias;

                    var pose = ps.last();

                    vc.addVertex(pose, x,        y, z       ).setUv(u0, v0).setColor(r, g, b, a).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
                    vc.addVertex(pose, x + TILE, y, z       ).setUv(u1, v0).setColor(r, g, b, a).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
                    vc.addVertex(pose, x + TILE, y, z + TILE).setUv(u1, v1).setColor(r, g, b, a).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
                    vc.addVertex(pose, x,        y, z + TILE).setUv(u0, v1).setColor(r, g, b, a).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
                }
            }
        }

        ps.popPose();
        buffers.endBatch();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}

