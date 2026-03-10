package org.joseph.atmosforge.atmosphere;

public final class InstabilityIndex {

    private InstabilityIndex() {}

    /**
     * Returns an instability score (0+). Higher means more supportive of deep convection.
     * This is a gameplay-friendly proxy, not a full CAPE solver.
     */
    public static double compute(double temperatureC,
                                 double moisture01,
                                 double frontStrength,
                                 double windMagnitude) {

        // Warmth contribution (kicks in above ~18C)
        double heat = clamp01((temperatureC - 18.0) / 20.0);

        // Moisture contribution (already 0..1)
        double moisture = clamp01(moisture01);

        // Lift contribution from fronts (scaled)
        double lift = clamp01(frontStrength / 6.0);

        // Shear-ish contribution from wind magnitude (scaled)
        double shear = clamp01(windMagnitude / 8.0);

        // Weighted blend: heat+moisture drive instability, fronts add lift, wind adds organization
        double score01 = (0.45 * heat) + (0.35 * moisture) + (0.15 * lift) + (0.05 * shear);

        // Return a more readable 0..100-ish scale
        return score01 * 100.0;
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }
}