package org.joseph.atmosforge.storm;


import org.joseph.atmosforge.atmosphere.PressureCell;
import org.joseph.atmosforge.core.AtmoConfig;
import org.joseph.atmosforge.data.RegionPos;


import java.util.Iterator;
import java.util.Map;


public final class StormCoreSystem {


    private StormCoreSystem() {}


    public static void apply(Map<RegionPos, PressureCell> regions,
                             StormRegistry registry) {


        for (Map.Entry<RegionPos, PressureCell> entry : regions.entrySet()) {


            RegionPos pos = entry.getKey();
            PressureCell cell = entry.getValue();


            StormCell storm = registry.getOrCreate(pos);


            StormEval eval = evaluate(cell);


            // --- Gentle smoothing (prevents intensity collapse) ---
            double smooth = AtmoConfig.STORM_INTENSITY_SMOOTH;


            double newIntensity =
                    lerp(storm.getIntensity(), eval.intensity, smooth);


            storm.setIntensity(newIntensity);
            storm.setType(eval.type);
            storm.setConvectiveScore(eval.convectiveScore);
            storm.setSynopticScore(eval.synopticScore);


            // --- Hysteresis logic ---
            double alive = AtmoConfig.STORM_ALIVE_THRESHOLD;
            double linger = alive * 0.5; // weakening buffer zone


            if (storm.getIntensity() >= alive) {


                storm.incrementAge();
                storm.resetBelowKill();


            } else if (storm.getIntensity() >= linger) {


                // Weak but still alive
                storm.incrementAge();
                storm.resetBelowKill();


            } else {


                storm.incrementBelowKill();
            }
        }


        // --- Extended cleanup (4x grace period) ---
        Iterator<Map.Entry<RegionPos, StormCell>> it =
                registry.getAll().entrySet().iterator();


        while (it.hasNext()) {


            Map.Entry<RegionPos, StormCell> e = it.next();
            StormCell storm = e.getValue();


            if (storm.getBelowKillTicks()
                    >= AtmoConfig.STORM_KILL_AFTER_TICKS * 4) {


                it.remove();
            }
        }
    }


    private static StormEval evaluate(PressureCell cell) {


        double precip = clamp01(cell.getPrecipRate());
        double updraft = clamp01(cell.getUpdraftIndex());


        double instability =
                clamp01(cell.getInstabilityIndex() / AtmoConfig.INSTABILITY_NORM);


        double front =
                clamp01(cell.getFrontStrength() / AtmoConfig.FRONT_NORM);


        double shear =
                clamp01(cell.getShearIndex() / AtmoConfig.SHEAR_NORM);


        double cycloneGrowth =
                clamp01(cell.getCycloneGrowth());


        double div = cell.getUpperDivergence();
        double divLift = div * AtmoConfig.STORM_LIFT_FROM_DIVERGENCE;
        if (divLift < 0.0) divLift = 0.0;
        divLift = clamp01(divLift);


        double vort =
                clamp01(cell.getUpperVorticity() / AtmoConfig.STORM_VORT_NORM);


        double synopticScore =
                (front * 0.30) +
                        (cycloneGrowth * 0.25) +
                        (divLift * 0.20) +
                        (vort * 0.15) +
                        (precip * 0.10);


        synopticScore = clamp01(synopticScore);


        double convectiveScore =
                (instability * 0.35) +
                        (updraft * 0.30) +
                        (shear * 0.20) +
                        (precip * 0.15);


        convectiveScore = clamp01(convectiveScore);


        double mode = convectiveScore - synopticScore;
        double convWeight = clamp01(0.50 + mode * 0.50);
        double synWeight = 1.0 - convWeight;


        double intensity =
                (convectiveScore * convWeight) +
                        (synopticScore * synWeight);


        intensity = clamp01(intensity);


        StormType type = classify(
                intensity,
                precip,
                updraft,
                instability,
                shear,
                synopticScore,
                convectiveScore
        );


        StormEval out = new StormEval();
        out.type = type;
        out.intensity = intensity;
        out.convectiveScore = convectiveScore;
        out.synopticScore = synopticScore;
        return out;
    }


    private static StormType classify(double intensity,
                                      double precip,
                                      double updraft,
                                      double instability,
                                      double shear,
                                      double synopticScore,
                                      double convectiveScore) {


        if (intensity < AtmoConfig.STORM_ALIVE_THRESHOLD)
            return StormType.NONE;


        if (synopticScore >= AtmoConfig.SYNOPTIC_STORM_THRESHOLD &&
                precip >= 0.20)
            return StormType.EXTRATROPICAL_CYCLONE;


        if (convectiveScore >= AtmoConfig.MCS_THRESHOLD &&
                precip >= AtmoConfig.MCS_MIN_PRECIP &&
                updraft >= AtmoConfig.MCS_MIN_UPDRAFT)
            return StormType.MCS;


        if (shear >= AtmoConfig.SUPERCELL_MIN_SHEAR &&
                instability >= AtmoConfig.SUPERCELL_MIN_INSTABILITY &&
                updraft >= AtmoConfig.SUPERCELL_MIN_UPDRAFT)
            return StormType.SUPERCELL;


        if (convectiveScore >= AtmoConfig.THUNDERSTORM_THRESHOLD &&
                updraft >= AtmoConfig.THUNDERSTORM_MIN_UPDRAFT)
            return StormType.THUNDERSTORMS;


        if (precip >= AtmoConfig.SHOWERS_MIN_PRECIP)
            return StormType.SHOWERS;


        return StormType.NONE;
    }


    private static double lerp(double a, double b, double t) {
        if (t <= 0.0) return a;
        if (t >= 1.0) return b;
        return a + (b - a) * t;
    }


    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }


    private static final class StormEval {
        StormType type;
        double intensity;
        double convectiveScore;
        double synopticScore;
    }
}


