package org.joseph.atmosforge.atmosphere;

public class WindVector {

    private double x;
    private double z;

    public WindVector(double x, double z) {
        this.x = x;
        this.z = z;
    }

    public void set(double x, double z) {
        this.x = x;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public double getZ() {
        return z;
    }

    public double magnitude() {
        return Math.sqrt(x * x + z * z);
    }
}