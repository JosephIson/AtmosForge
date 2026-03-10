package org.joseph.atmosforge.atmosphere;

import org.joseph.atmosforge.core.AtmoConfig;
import org.joseph.atmosforge.data.RegionPos;

public final class SeasonalMoistureModel {

    private SeasonalMoistureModel() {}

    /**
     * Returns a multiplier applied to a region's baseline moisture due to seasons.
     * Summer hemisphere tends to be more humid, winter hemisphere tends to be drier.
     */
    public static double moistureMultiplier(RegionPos pos, double seasonSignal) {

        // Pseudo-latitude: Z axis is north/south
        double lat = pos.z() * 0.01;

        // 0 at equator, 1 toward poles (clamped)
        double latFactor = Math.max(0.0, Math.min(1.0, Math.abs(lat)));

        // Hemisphere sign: +1 for +Z, -1 for -Z, 0 for equator band
        double hemi = (pos.z() > 0) ? 1.0 : (pos.z() < 0 ? -1.0 : 0.0);

        // Local season: + = local summer, - = local winter
        double localSeason = seasonSignal * hemi;

        double mult = 1.0 + (localSeason * latFactor * AtmoConfig.SEASONAL_MOISTURE_AMPLITUDE);

        if (mult < AtmoConfig.SEASONAL_MOISTURE_MULT_MIN) return AtmoConfig.SEASONAL_MOISTURE_MULT_MIN;
        if (mult > AtmoConfig.SEASONAL_MOISTURE_MULT_MAX) return AtmoConfig.SEASONAL_MOISTURE_MULT_MAX;
        return mult;
    }

    public static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }
}