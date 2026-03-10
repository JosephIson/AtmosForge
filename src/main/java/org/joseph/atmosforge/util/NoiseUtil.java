package org.joseph.atmosforge.util;

import java.util.Random;

public final class NoiseUtil {

    private static final long GLOBAL_SEED = 987654321L;

    private NoiseUtil() {}

    // Smooth 2D deterministic noise
    public static double smoothNoise(double x, double z) {

        long seed = hash(x, z);
        Random random = new Random(seed);

        return random.nextDouble() * 2.0 - 1.0;
    }

    public static double fractalNoise(double x, double z, int octaves) {

        double total = 0.0;
        double frequency = 1.0;
        double amplitude = 1.0;
        double maxValue = 0.0;

        for (int i = 0; i < octaves; i++) {

            total += smoothNoise(x * frequency, z * frequency) * amplitude;

            maxValue += amplitude;
            amplitude *= 0.5;
            frequency *= 2.0;
        }

        return total / maxValue;
    }

    private static long hash(double x, double z) {

        long hx = Double.doubleToLongBits(x);
        long hz = Double.doubleToLongBits(z);

        long h = hx * 31 + hz;
        h ^= GLOBAL_SEED;

        return h;
    }
}