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
import org.joseph.atmosforge.client.ClientCloudData;

@EventBusSubscriber(modid = Atmosforge.MODID, value = Dist.CLIENT)
public final class CloudLayerRenderer {

    // World-space cloud ceiling
    private static final int CLOUD_Y = 165;

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

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderTexture(0, TEX);

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(TEX));

        ps.pushPose();
        ps.translate(-camX, -camY, -camZ);

        // Slight drift with time, but world-space stable
        float t = (ClientCloudData.getLastGameTime() % 24000L) / 24000f;
        float driftU = t * 0.15f;
        float driftV = t * 0.08f;

        int overlay = OverlayTexture.NO_OVERLAY;
        int light = LightTexture.FULL_BRIGHT; // clouds in sky read better full bright; change later if wanted
        float nx = 0f, ny = -1f, nz = 0f;     // downward normal (ceiling plane)

        for (int x = (minX / TILE) * TILE; x <= maxX; x += TILE) {
            for (int z = (minZ / TILE) * TILE; z <= maxZ; z += TILE) {

                // sample region climate
                int rx = x >> REGION_SHIFT_BLOCKS;
                int rz = z >> REGION_SHIFT_BLOCKS;

                ClientCloudData.CloudSample s = ClientCloudData.get(rx, rz);
                if (s == null) continue;

                float cloudiness = Mth.clamp(s.cloudiness(), 0f, 1f);
                float base = Mth.clamp(s.baseDensity(), 0f, 1f);

                // If the atmosphere says clear, skip tiles
                float density = base * (0.35f + 0.65f * cloudiness);
                if (density < 0.08f) continue;

                // shape density with distance (fade edges of render disk)
                float dx = (x + TILE * 0.5f) - px;
                float dz = (z + TILE * 0.5f) - pz;
                float dist = (float)Math.sqrt(dx * dx + dz * dz);
                float edgeFade = 1.0f - Mth.clamp((dist - (RADIUS_BLOCKS * 0.75f)) / (RADIUS_BLOCKS * 0.25f), 0f, 1f);

                float alpha = Mth.clamp(density * edgeFade * 0.55f, 0f, 0.65f);
                if (alpha < 0.04f) continue;

                int a = (int)(alpha * 255f);

                // Darker for stormier background
                int r = 210 - (int)(cloudiness * 60f);
                int g = 215 - (int)(cloudiness * 65f);
                int b = 220 - (int)(cloudiness * 70f);

                float u0 = (x / 512f) + driftU;
                float v0 = (z / 512f) + driftV;
                float u1 = ((x + TILE) / 512f) + driftU;
                float v1 = ((z + TILE) / 512f) + driftV;

                float y = CLOUD_Y;

                var pose = ps.last();

                vc.addVertex(pose, x, y, z).setUv(u0, v0).setColor(r, g, b, a).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
                vc.addVertex(pose, x + TILE, y, z).setUv(u1, v0).setColor(r, g, b, a).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
                vc.addVertex(pose, x + TILE, y, z + TILE).setUv(u1, v1).setColor(r, g, b, a).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
                vc.addVertex(pose, x, y, z + TILE).setUv(u0, v1).setColor(r, g, b, a).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
            }
        }

        ps.popPose();
        buffers.endBatch();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}

