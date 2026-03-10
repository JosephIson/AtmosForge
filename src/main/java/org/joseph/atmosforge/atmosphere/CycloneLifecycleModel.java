package org.joseph.atmosforge.atmosphere;

import org.joseph.atmosforge.core.AtmoConfig;
import org.joseph.atmosforge.data.RegionPos;

import java.util.Map;

public final class CycloneLifecycleModel {

    private CycloneLifecycleModel() {}

    /**
     * Uses cyclone tilt (upper vort max offset from surface low) to estimate lifecycle stage:
     * - Developing: moderate tilt (sweet spot)
     * - Mature/stacked: low tilt
     * - Overly displaced: weak coupling
     *
     * Applies occlusion/decay tendencies once mature:
     * - reduces front strength
     * - fills surface pressure slightly
     * - damps upper divergence slightly
     */
    public static void apply(Map<RegionPos, PressureCell> regions) {

        for (PressureCell cell : regions.values()) {

            double tilt = cell.getCycloneTilt();
            double growth = clamp01(cell.getCycloneGrowth());

            // Maturity: high when stacked (tilt small), low when strongly tilted (developing), low when very displaced
            double maturity;
            if (tilt <= AtmoConfig.MATURITY_STACK_TILT) {
                maturity = 1.0;
            } else if (tilt <= AtmoConfig.MATURITY_DEVELOP_MAX_TILT) {
                // Developing zone: maturity ramps down from 1 -> ~0.15
                double t = (tilt - AtmoConfig.MATURITY_STACK_TILT) /
                        (AtmoConfig.MATURITY_DEVELOP_MAX_TILT - AtmoConfig.MATURITY_STACK_TILT);
                maturity = 1.0 - (0.85 * t);
            } else if (tilt <= AtmoConfig.MATURITY_DECOUPLE_TILT) {
                // Decoupling zone: maturity stays low
                maturity = 0.15;
            } else {
                maturity = 0.10;
            }

            // If system is not actually developing (growth low), keep maturity from maxing out too hard
            // This avoids randomly stacked weak systems being treated like fully mature cyclones.
            maturity *= (0.45 + 0.55 * growth);

            maturity = clamp01(maturity);
            cell.setCycloneMaturity(maturity);

            // Occlusion/decay only matters when maturity is significant
            if (maturity <= AtmoConfig.OCCLUSION_START_MATURITY) continue;

            // Scale occlusion intensity from start threshold to 1.0
            double occlT = (maturity - AtmoConfig.OCCLUSION_START_MATURITY) /
                    (1.0 - AtmoConfig.OCCLUSION_START_MATURITY);
            occlT = clamp01(occlT);

            // 1) Front weakening (occlusion wraps warm/cold fronts and reduces sharp gradients)
            double newFront = cell.getFrontStrength() * (1.0 - AtmoConfig.OCCLUSION_FRONT_DECAY * occlT);
            if (newFront < 0.0) newFront = 0.0;
            cell.setFrontStrength(newFront);

            // 2) Surface pressure filling (system begins to weaken)
            cell.addSurfacePressure(AtmoConfig.OCCLUSION_PRESSURE_FILL * occlT);

            // 3) Upper divergence damping (less sustained deepening support)
            double div = cell.getUpperDivergence();
            div *= (1.0 - AtmoConfig.OCCLUSION_DIVERGENCE_DAMP * occlT);
            cell.setUpperDivergence(div);
        }
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }
}
