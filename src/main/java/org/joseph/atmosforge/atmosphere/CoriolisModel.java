package org.joseph.atmosforge.atmosphere;

import org.joseph.atmosforge.core.AtmoConfig;
import org.joseph.atmosforge.data.RegionPos;

public final class CoriolisModel {

    private CoriolisModel() {}

    public static void apply(RegionPos pos, WindVector vector) {

        double lat = pos.z() * 0.01;

        double latFactor = Math.max(0.0, Math.min(1.0, Math.abs(lat)));

        double hemisphere = (pos.z() < 0) ? 1.0 : -1.0;

        double magnitude = vector.magnitude();

        double angle =
                hemisphere *
                        latFactor *
                        AtmoConfig.CORIOLIS_STRENGTH *
                        (magnitude * 0.02);

        double x = vector.getX();
        double z = vector.getZ();

        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        double newX = (x * cos) - (z * sin);
        double newZ = (x * sin) + (z * cos);

        vector.set(newX, newZ);
    }
}