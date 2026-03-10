package org.joseph.atmosforge.atmosphere;

import org.joseph.atmosforge.core.AtmoConfig;
import org.joseph.atmosforge.data.RegionPos;

public final class LatitudeModel {

    private LatitudeModel() {}

    /**
     * Returns a latitude temperature offset in °C.
     * Uses Z as pseudo-latitude and includes seasonal axial tilt by shifting the thermal equator.
     */
    public static double getLatitudeTemperatureOffset(RegionPos pos, double thermalEquatorShiftRegions) {

        // Shift the effective latitude based on season
        double shiftedZ = pos.z() - thermalEquatorShiftRegions;

        // Convert to a gentle lat scale
        double latitude = shiftedZ * 0.01;

        // Warm near the (shifted) equator, colder toward poles
        double gradient = Math.cos(latitude);

        return gradient * AtmoConfig.LATITUDE_TEMP_AMPLITUDE_C;
    }
}