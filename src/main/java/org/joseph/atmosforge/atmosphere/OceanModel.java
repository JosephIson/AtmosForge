package org.joseph.atmosforge.atmosphere;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.joseph.atmosforge.data.RegionPos;
import org.joseph.atmosforge.core.AtmoConfig;

public final class OceanModel {

    private OceanModel() {}

    public static boolean isOceanRegion(ServerLevel level, RegionPos pos) {

        int blockX = (pos.x() << AtmoConfig.REGION_SHIFT) << 4;
        int blockZ = (pos.z() << AtmoConfig.REGION_SHIFT) << 4;

        BlockPos sample = new BlockPos(blockX, level.getSeaLevel(), blockZ);

        FluidState fluid = level.getFluidState(sample);

        return fluid.getType() == Fluids.WATER;
    }

    public static double applyOceanThermalModeration(boolean isOcean,
                                                     double baseTemperature,
                                                     double latitudeOffset) {

        if (!isOcean) return baseTemperature + latitudeOffset;

        // Oceans reduce seasonal swing by half
        return baseTemperature + (latitudeOffset * 0.5);
    }

    public static double oceanMoistureBoost(boolean isOcean) {
        return isOcean ? 0.15 : 0.0;
    }
}