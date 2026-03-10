package org.joseph.atmosforge.atmosphere;

import org.joseph.atmosforge.core.AtmoConfig;
import org.joseph.atmosforge.data.RegionPos;

import java.util.HashMap;
import java.util.Map;

public final class UpperLevelDynamics {

    private UpperLevelDynamics() {}

    /**
     * Two-layer coupling model:
     * - Thermal wind (perpendicular to surface temperature gradient) creates an upper-wind target
     * - Existing upper wind (seeded by jet stream earlier in the tick) is blended toward that target
     * - Upper divergence is computed from the blended upper wind field
     * - Upper divergence forces surface pressure tendency (divergence lowers surface pressure)
     */
    public static void apply(Map<RegionPos, PressureCell> regions) {

        Map<RegionPos, double[]> blendedUpperWind = new HashMap<>();

        // 1) Blend jet-seeded upper wind toward thermal-wind target
        for (Map.Entry<RegionPos, PressureCell> entry : regions.entrySet()) {

            RegionPos pos = entry.getKey();
            PressureCell cell = entry.getValue();

            PressureCell east = regions.get(new RegionPos(pos.x() + 1, pos.z()));
            PressureCell west = regions.get(new RegionPos(pos.x() - 1, pos.z()));
            PressureCell north = regions.get(new RegionPos(pos.x(), pos.z() - 1));
            PressureCell south = regions.get(new RegionPos(pos.x(), pos.z() + 1));

            double thermalX = 0.0;
            double thermalZ = 0.0;

            if (east != null && west != null && north != null && south != null) {

                double dTdx = (east.getSurfaceTemperature() - west.getSurfaceTemperature()) * 0.5;
                double dTdz = (south.getSurfaceTemperature() - north.getSurfaceTemperature()) * 0.5;

                // Perpendicular to temperature gradient (thermal wind proxy)
                thermalX = -dTdz * AtmoConfig.THERMAL_WIND_STRENGTH;
                thermalZ = dTdx * AtmoConfig.THERMAL_WIND_STRENGTH;
            }

            // Existing (jet-seeded) upper wind from earlier in the tick
            double jetX = cell.getUpperWind().getX();
            double jetZ = cell.getUpperWind().getZ();

            // Weighted blend: keep jet steering while allowing thermal gradients to shape flow
            double blend = AtmoConfig.UPPER_THERMAL_BLEND; // 0..1
            double outX = jetX * (1.0 - blend) + thermalX * blend;
            double outZ = jetZ * (1.0 - blend) + thermalZ * blend;

            // Cap for stability
            double mag = Math.sqrt(outX * outX + outZ * outZ);
            if (mag > AtmoConfig.UPPER_WIND_MAX && mag > 0.0) {
                double s = AtmoConfig.UPPER_WIND_MAX / mag;
                outX *= s;
                outZ *= s;
            }

            blendedUpperWind.put(pos, new double[]{outX, outZ});
        }

        // 2) Apply blended upper wind
        for (Map.Entry<RegionPos, double[]> entry : blendedUpperWind.entrySet()) {
            PressureCell cell = regions.get(entry.getKey());
            if (cell == null) continue;

            double[] w = entry.getValue();
            cell.setUpperWind(w[0], w[1]);
        }

        // 3) Compute upper divergence and force surface pressure
        for (Map.Entry<RegionPos, PressureCell> entry : regions.entrySet()) {

            RegionPos pos = entry.getKey();
            PressureCell cell = entry.getValue();

            PressureCell east = regions.get(new RegionPos(pos.x() + 1, pos.z()));
            PressureCell west = regions.get(new RegionPos(pos.x() - 1, pos.z()));
            PressureCell north = regions.get(new RegionPos(pos.x(), pos.z() - 1));
            PressureCell south = regions.get(new RegionPos(pos.x(), pos.z() + 1));

            if (east == null || west == null || north == null || south == null) {
                cell.setUpperDivergence(0.0);
                continue;
            }

            double dUdx = (east.getUpperWind().getX() - west.getUpperWind().getX()) * 0.5;
            double dVdz = (south.getUpperWind().getZ() - north.getUpperWind().getZ()) * 0.5;

            double divergence = (dUdx + dVdz) * AtmoConfig.UPPER_DIVERGENCE_STRENGTH;

            if (divergence > AtmoConfig.UPPER_DIVERGENCE_MAX) divergence = AtmoConfig.UPPER_DIVERGENCE_MAX;
            if (divergence < -AtmoConfig.UPPER_DIVERGENCE_MAX) divergence = -AtmoConfig.UPPER_DIVERGENCE_MAX;

            cell.setUpperDivergence(divergence);

            // Divergence aloft -> surface pressure falls; convergence -> rises
            cell.addSurfacePressure(-divergence);
        }
    }
}
