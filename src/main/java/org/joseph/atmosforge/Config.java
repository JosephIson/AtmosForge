package org.joseph.atmosforge;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // --- Simulation Settings ---

    public static final ModConfigSpec.IntValue SIMULATION_INTERVAL =
            BUILDER.comment("How often (in ticks) the atmosphere updates.")
                    .defineInRange("simulationInterval", 20, 1, 200);

    public static final ModConfigSpec.IntValue SIMULATION_RADIUS =
            BUILDER.comment("How many regions around each player are simulated.")
                    .defineInRange("simulationRadius", 2, 1, 8);

    // --- Atmospheric Behavior ---

    public static final ModConfigSpec.DoubleValue PRESSURE_VARIANCE =
            BUILDER.comment("Random pressure fluctuation strength.")
                    .defineInRange("pressureVariance", 0.1D, 0.0D, 5.0D);

    public static final ModConfigSpec.DoubleValue TEMPERATURE_VARIANCE =
            BUILDER.comment("Random temperature fluctuation strength.")
                    .defineInRange("temperatureVariance", 0.05D, 0.0D, 5.0D);

    public static final ModConfigSpec SPEC = BUILDER.build();
}