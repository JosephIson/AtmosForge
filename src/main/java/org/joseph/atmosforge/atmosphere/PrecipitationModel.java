package org.joseph.atmosforge.atmosphere;

import org.joseph.atmosforge.core.AtmoConfig;
import org.joseph.atmosforge.data.RegionPos;

import java.util.Map;

public final class PrecipitationModel {

    private PrecipitationModel() {}

    public static void apply(Map<RegionPos, PressureCell> regions) {

        for (Map.Entry<RegionPos, PressureCell> entry : regions.entrySet()) {

            PressureCell cell = entry.getValue();

            double moisture = clamp01(cell.getSurfaceMoisture());
            double instability = clamp01(cell.getInstabilityIndex() / AtmoConfig.INSTABILITY_NORM);
            double front = clamp01(cell.getFrontStrength() / AtmoConfig.FRONT_NORM);

            double divergence = cell.getUpperDivergence();
            double lift = divergence * AtmoConfig.LIFT_FROM_DIVERGENCE;
            if (lift < 0.0) lift = 0.0;
            lift = clamp01(lift);

            double synopticLift =
                    (lift * AtmoConfig.LIFT_WEIGHT_DIVERGENCE) +
                            (front * AtmoConfig.LIFT_WEIGHT_FRONTS);

            synopticLift = clamp01(synopticLift);

            double updraft =
                    synopticLift * (0.45 + 0.55 * instability);

            updraft = clamp01(updraft);

            double precip =
                    moisture *
                            updraft *
                            AtmoConfig.PRECIP_STRENGTH;

            double convBoost = instability * AtmoConfig.CONVECTIVE_BOOST;
            precip *= (1.0 + convBoost);

            double dryPenalty = 1.0 - (AtmoConfig.DRY_AIR_PENALTY * (1.0 - moisture));
            precip *= clamp01(dryPenalty);

            cell.nudgeUpdraftToward(updraft, AtmoConfig.UPDRAFT_SMOOTH_RATE);
            cell.nudgePrecipToward(precip, AtmoConfig.PRECIP_SMOOTH_RATE);

            double t = cell.getSurfaceTemperature();

            double rain;
            double snow;

            if (t <= AtmoConfig.SNOW_TEMP_CUTOFF) {
                snow = cell.getPrecipRate();
                rain = 0.0;
            } else if (t >= AtmoConfig.RAIN_TEMP_CUTOFF) {
                rain = cell.getPrecipRate();
                snow = 0.0;
            } else {
                double alpha = (t - AtmoConfig.SNOW_TEMP_CUTOFF) /
                        (AtmoConfig.RAIN_TEMP_CUTOFF - AtmoConfig.SNOW_TEMP_CUTOFF);
                alpha = clamp01(alpha);
                rain = cell.getPrecipRate() * alpha;
                snow = cell.getPrecipRate() * (1.0 - alpha);
            }

            cell.setRainRate(rain);
            cell.setSnowRate(snow);

            double cloudiness = clamp01(
                    (moisture * AtmoConfig.CLOUD_MOISTURE_WEIGHT) +
                            (synopticLift * AtmoConfig.CLOUD_LIFT_WEIGHT)
            );

            cell.nudgeCloudinessToward(cloudiness, AtmoConfig.CLOUD_SMOOTH_RATE);
        }
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }
}
