package org.joseph.atmosforge.atmosphere;

import org.joseph.atmosforge.data.RegionPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CycloneStructureModel {

    private static final int SEARCH_RADIUS = 4;

    private CycloneStructureModel() {}

    /**
     * Detects surface lows and matches them with nearby upper-level vorticity maxima.
     * Stores:
     * - cycloneTilt (region-distance between surface low and upper vort max)
     * - cycloneGrowth (0..1 factor used to modulate baroclinic amplification)
     */
    public static void apply(Map<RegionPos, PressureCell> regions) {

        // Reset structure fields each tick
        for (PressureCell cell : regions.values()) {
            cell.setCycloneTilt(0.0);
            cell.setCycloneGrowth(0.0);
        }

        List<RegionPos> lows = new ArrayList<>();

        // 1) Find local minima of surface pressure (simple 4-neighbor low detection)
        for (Map.Entry<RegionPos, PressureCell> entry : regions.entrySet()) {

            RegionPos pos = entry.getKey();
            PressureCell cell = entry.getValue();

            PressureCell east = regions.get(new RegionPos(pos.x() + 1, pos.z()));
            PressureCell west = regions.get(new RegionPos(pos.x() - 1, pos.z()));
            PressureCell north = regions.get(new RegionPos(pos.x(), pos.z() - 1));
            PressureCell south = regions.get(new RegionPos(pos.x(), pos.z() + 1));

            if (east == null || west == null || north == null || south == null) continue;

            double p = cell.getSurfacePressure();

            if (p < east.getSurfacePressure()
                    && p < west.getSurfacePressure()
                    && p < north.getSurfacePressure()
                    && p < south.getSurfacePressure()) {
                lows.add(pos);
            }
        }

        // 2) For each low, find the nearby upper vorticity maximum and compute tilt
        for (RegionPos lowPos : lows) {

            PressureCell lowCell = regions.get(lowPos);
            if (lowCell == null) continue;

            RegionPos bestVortPos = null;
            double bestVort = -1.0;

            for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
                for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {

                    RegionPos p = new RegionPos(lowPos.x() + dx, lowPos.z() + dz);
                    PressureCell c = regions.get(p);
                    if (c == null) continue;

                    double vort = c.getUpperVorticity();
                    if (vort > bestVort) {
                        bestVort = vort;
                        bestVortPos = p;
                    }
                }
            }

            if (bestVortPos == null) continue;

            int offX = bestVortPos.x() - lowPos.x();
            int offZ = bestVortPos.z() - lowPos.z();

            double tilt = Math.sqrt(offX * (double) offX + offZ * (double) offZ);

            // Growth factor: strongest when tilted but not too far displaced
            // - too stacked (tilt < 1): baroclinic growth begins to shut down
            // - sweet spot (1..3): strongest development stage
            // - too separated (> 4): coupling weakens
            double growth;
            if (tilt < 1.0) {
                growth = 0.35;
            } else if (tilt <= 3.0) {
                growth = 1.0;
            } else if (tilt <= 4.0) {
                growth = 0.65;
            } else {
                growth = 0.25;
            }

            lowCell.setCycloneTilt(tilt);
            lowCell.setCycloneGrowth(growth);
        }
    }
}
