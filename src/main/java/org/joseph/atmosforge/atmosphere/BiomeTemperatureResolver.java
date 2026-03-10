package org.joseph.atmosforge.atmosphere;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

public class BiomeTemperatureResolver {

    private static final int REGION_SHIFT = 4; // 16 chunks per region

    public static double resolveTemperature(ServerLevel level, int regionX, int regionZ) {

        // Convert region -> chunk -> block center
        int chunkX = regionX << REGION_SHIFT;
        int chunkZ = regionZ << REGION_SHIFT;

        int blockX = (chunkX << 4) + 8;
        int blockZ = (chunkZ << 4) + 8;

        BlockPos pos = new BlockPos(blockX, level.getSeaLevel(), blockZ);

        Biome biome = level.getBiome(pos).value();

        // Vanilla biome base temperature (usually 0.0–2.0)
        double baseTemp = biome.getBaseTemperature();

        // Scale into broader range (-10°C to ~50°C)
        return (baseTemp * 30.0) - 10.0;
    }
}