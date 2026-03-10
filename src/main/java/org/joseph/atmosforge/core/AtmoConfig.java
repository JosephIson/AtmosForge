package org.joseph.atmosforge.core;

public final class AtmoConfig {

    public static final int REGION_SHIFT = 4;

    public static final double PRESSURE_DIFFUSION_RATE = 0.05;
    public static final double TEMP_PRESSURE_FACTOR = 0.0005;
    public static final double STANDARD_PRESSURE = 1013.0;

    public static final double CORIOLIS_STRENGTH = 0.02;

    public static final long SEASON_YEAR_LENGTH_TICKS = 24000L * 96L;
    public static final double AXIAL_TILT_REGION_SHIFT = 120.0;
    public static final double LATITUDE_TEMP_AMPLITUDE_C = 20.0;

    public static final double SEASONAL_MOISTURE_AMPLITUDE = 0.35;
    public static final double SEASONAL_MOISTURE_MULT_MIN = 0.35;
    public static final double SEASONAL_MOISTURE_MULT_MAX = 1.65;
    public static final double SEASONAL_MOISTURE_ADJUST_RATE = 0.02;

    public static final double VORTICITY_SCALE = 25.0;

    public static final double PLANETARY_WAVE_STRENGTH = 0.6;

    public static final double BAROCLINIC_STRENGTH = 0.12;
    public static final double BAROCLINIC_MAX_TENDENCY = 1.25;

    public static final double THERMAL_WIND_STRENGTH = 0.6;
    public static final double UPPER_DIVERGENCE_STRENGTH = 0.12;

    public static final double UPPER_THERMAL_BLEND = 0.55;

    public static final double UPPER_WIND_MAX = 12.0;
    public static final double UPPER_DIVERGENCE_MAX = 2.0;

    public static final double MATURITY_STACK_TILT = 1.0;
    public static final double MATURITY_DEVELOP_MAX_TILT = 3.0;
    public static final double MATURITY_DECOUPLE_TILT = 4.0;

    public static final double OCCLUSION_START_MATURITY = 0.55;
    public static final double OCCLUSION_FRONT_DECAY = 0.18;
    public static final double OCCLUSION_PRESSURE_FILL = 0.22;
    public static final double OCCLUSION_DIVERGENCE_DAMP = 0.25;

    // Diagnostic precipitation
    public static final double INSTABILITY_NORM = 100.0;
    public static final double FRONT_NORM = 10.0;

    public static final double LIFT_FROM_DIVERGENCE = 0.75;
    public static final double LIFT_WEIGHT_DIVERGENCE = 0.65;
    public static final double LIFT_WEIGHT_FRONTS = 0.55;

    public static final double PRECIP_STRENGTH = 1.10;
    public static final double CONVECTIVE_BOOST = 0.85;
    public static final double DRY_AIR_PENALTY = 0.65;

    public static final double UPDRAFT_SMOOTH_RATE = 0.20;
    public static final double PRECIP_SMOOTH_RATE = 0.18;
    public static final double CLOUD_SMOOTH_RATE = 0.12;

    public static final double SNOW_TEMP_CUTOFF = 0.0;
    public static final double RAIN_TEMP_CUTOFF = 2.0;

    public static final double CLOUD_MOISTURE_WEIGHT = 0.62;
    public static final double CLOUD_LIFT_WEIGHT = 0.55;

    // ------------------------
    // Storm core (classification + persistence)
    // ------------------------

    // Normalizers
    public static final double SHEAR_NORM = 18.0;
    public static final double STORM_VORT_NORM = 35.0;

    // Upper divergence -> lift for storm diagnosis
    public static final double STORM_LIFT_FROM_DIVERGENCE = 0.85;

    // Persistence
    public static final double STORM_ALIVE_THRESHOLD = 0.12;
    public static final int STORM_KILL_AFTER_TICKS = 80;

    // Smoothing (0..1)
    public static final double STORM_INTENSITY_SMOOTH = 0.18;

    // Type thresholds
    public static final double SHOWERS_MIN_PRECIP = 0.10;

    public static final double THUNDERSTORM_THRESHOLD = 0.30;
    public static final double THUNDERSTORM_MIN_UPDRAFT = 0.22;

    public static final double SUPERCELL_MIN_SHEAR = 0.55;
    public static final double SUPERCELL_MIN_INSTABILITY = 0.55;
    public static final double SUPERCELL_MIN_UPDRAFT = 0.40;

    public static final double MCS_THRESHOLD = 0.45;
    public static final double MCS_MIN_PRECIP = 0.35;
    public static final double MCS_MIN_UPDRAFT = 0.28;

    public static final double SYNOPTIC_STORM_THRESHOLD = 0.45;

    private AtmoConfig() {}
}
