package org.joseph.atmosforge.atmosphere;

public enum AirMassType {

    COLD_DRY,
    COLD_MOIST,
    WARM_DRY,
    WARM_MOIST;

    public static AirMassType classify(double temperature, double moisture) {

        boolean warm = temperature > 15.0;
        boolean moist = moisture > 0.5;

        if (warm && moist) return WARM_MOIST;
        if (warm) return WARM_DRY;
        if (moist) return COLD_MOIST;
        return COLD_DRY;
    }
}