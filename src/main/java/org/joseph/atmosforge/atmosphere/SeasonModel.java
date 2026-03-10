package org.joseph.atmosforge.atmosphere;

import org.joseph.atmosforge.core.AtmoConfig;

public class SeasonModel {

    private double phase = 0.0;

    public void tick(long worldGameTime) {
        long year = AtmoConfig.SEASON_YEAR_LENGTH_TICKS;
        long t = worldGameTime % year;
        phase = (t / (double) year) * (Math.PI * 2.0);
    }

    /**
     * Returns a north/south shift for the thermal equator in REGION units.
     * Positive means warmth shifts toward +Z, negative toward -Z.
     */
    public double getThermalEquatorShiftRegions() {
        return Math.sin(phase) * AtmoConfig.AXIAL_TILT_REGION_SHIFT;
    }

    /**
     * Returns a seasonal signal in [-1..1].
     * +1 means peak warmth shifted toward +Z hemisphere, -1 toward -Z hemisphere.
     */
    public double getSeasonSignal() {
        return Math.sin(phase);
    }

    /**
     * 0..1 where 0/1 = endpoints and ~0.5 = equinox-ish.
     */
    public double getSeasonStrength01() {
        return 0.5 + 0.5 * Math.sin(phase);
    }
}