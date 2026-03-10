package org.joseph.atmosforge.util;

public final class MathUtil {

    private MathUtil() {}

    // ----------------------------
    // Clamp Utilities
    // ----------------------------

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clamp01(double value) {
        return clamp(value, 0.0, 1.0);
    }

    // ----------------------------
    // Interpolation
    // ----------------------------

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static double smoothstep(double edge0, double edge1, double x) {
        x = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
        return x * x * (3 - 2 * x);
    }

    // ----------------------------
    // Vector Helpers
    // ----------------------------

    public static double magnitude(double x, double z) {
        return Math.sqrt(x * x + z * z);
    }

    // ----------------------------
    // Gradient Helper
    // ----------------------------

    public static double centralDifference(double forward, double backward) {
        return (forward - backward) * 0.5;
    }
}