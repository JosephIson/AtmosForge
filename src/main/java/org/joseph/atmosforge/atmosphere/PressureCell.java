package org.joseph.atmosforge.atmosphere;

import org.joseph.atmosforge.Config;
import org.joseph.atmosforge.core.AtmoConfig;
import org.joseph.atmosforge.util.MathUtil;

import java.util.concurrent.ThreadLocalRandom;

public class PressureCell {

    // ------------------------
    // SURFACE LAYER
    // ------------------------

    private double surfaceTemperature;
    private final double baseMoisture;
    private double surfaceMoisture;
    private double surfacePressure;

    private final WindVector surfaceWind;

    // ------------------------
    // UPPER LAYER (500mb proxy)
    // ------------------------

    private final WindVector upperWind;
    private double upperVorticity;
    private double upperDivergence;

    // ------------------------
    // SHARED INDICES
    // ------------------------

    private double frontStrength;
    private double instabilityIndex;
    private double shearIndex;

    // Cyclone structure
    private double cycloneTilt;
    private double cycloneGrowth;

    // Cyclone lifecycle
    private double cycloneMaturity;

    // Diagnostic weather output
    private double updraftIndex;     // 0..1
    private double precipRate;       // 0..1 overall
    private double rainRate;         // 0..1
    private double snowRate;         // 0..1
    private double cloudiness;       // 0..1

    private AirMassType airMassType;

    public PressureCell(double temperature, double moisture, double pressure) {

        this.surfaceTemperature = temperature;
        this.baseMoisture = MathUtil.clamp01(moisture);
        this.surfaceMoisture = MathUtil.clamp01(moisture);
        this.surfacePressure = pressure;
        this.surfaceWind = new WindVector(0, 0);

        this.upperWind = new WindVector(0, 0);
        this.upperVorticity = 0.0;
        this.upperDivergence = 0.0;

        this.frontStrength = 0.0;
        this.instabilityIndex = 0.0;
        this.shearIndex = 0.0;

        this.cycloneTilt = 0.0;
        this.cycloneGrowth = 0.0;
        this.cycloneMaturity = 0.0;

        this.updraftIndex = 0.0;
        this.precipRate = 0.0;
        this.rainRate = 0.0;
        this.snowRate = 0.0;
        this.cloudiness = 0.0;

        this.airMassType = AirMassType.classify(surfaceTemperature, surfaceMoisture);
    }

    // ------------------------
    // SURFACE
    // ------------------------

    public double getSurfacePressure() {
        return surfacePressure;
    }

    public void setSurfacePressure(double value) {
        this.surfacePressure = value;
    }

    public void addSurfacePressure(double amount) {
        this.surfacePressure += amount;
    }

    public double getSurfaceTemperature() {
        return surfaceTemperature;
    }

    public void addSurfaceTemperature(double delta) {
        this.surfaceTemperature += delta;
        airMassType = AirMassType.classify(surfaceTemperature, surfaceMoisture);
    }

    public double getBaseMoisture() {
        return baseMoisture;
    }

    public double getSurfaceMoisture() {
        return surfaceMoisture;
    }

    public void addSurfaceMoisture(double amount) {
        this.surfaceMoisture = MathUtil.clamp01(surfaceMoisture + amount);
        airMassType = AirMassType.classify(surfaceTemperature, surfaceMoisture);
    }

    public void nudgeSurfaceMoistureToward(double target, double rate) {
        double delta = (MathUtil.clamp01(target) - surfaceMoisture) * rate;
        surfaceMoisture = MathUtil.clamp01(surfaceMoisture + delta);
        airMassType = AirMassType.classify(surfaceTemperature, surfaceMoisture);
    }

    public WindVector getSurfaceWind() {
        return surfaceWind;
    }

    public void setSurfaceWind(double x, double z) {
        surfaceWind.set(x, z);
    }

    // ------------------------
    // UPPER
    // ------------------------

    public WindVector getUpperWind() {
        return upperWind;
    }

    public void setUpperWind(double x, double z) {
        upperWind.set(x, z);
    }

    public double getUpperVorticity() {
        return upperVorticity;
    }

    public void setUpperVorticity(double value) {
        this.upperVorticity = value;
    }

    public double getUpperDivergence() {
        return upperDivergence;
    }

    public void setUpperDivergence(double value) {
        this.upperDivergence = value;
    }

    // ------------------------
    // SHARED INDICES
    // ------------------------

    public double getFrontStrength() {
        return frontStrength;
    }

    public void setFrontStrength(double strength) {
        this.frontStrength = strength;
    }

    public double getInstabilityIndex() {
        return instabilityIndex;
    }

    public void setInstabilityIndex(double value) {
        this.instabilityIndex = value;
    }

    public double getShearIndex() {
        return shearIndex;
    }

    public void setShearIndex(double value) {
        this.shearIndex = value;
    }

    // ------------------------
    // CYCLONE STRUCTURE
    // ------------------------

    public double getCycloneTilt() {
        return cycloneTilt;
    }

    public void setCycloneTilt(double value) {
        this.cycloneTilt = value;
    }

    public double getCycloneGrowth() {
        return cycloneGrowth;
    }

    public void setCycloneGrowth(double value) {
        this.cycloneGrowth = value;
    }

    // ------------------------
    // CYCLONE LIFECYCLE
    // ------------------------

    public double getCycloneMaturity() {
        return cycloneMaturity;
    }

    public void setCycloneMaturity(double value) {
        this.cycloneMaturity = value;
    }

    // ------------------------
    // DIAGNOSTIC WEATHER OUTPUT
    // ------------------------

    public double getUpdraftIndex() {
        return updraftIndex;
    }

    public void setUpdraftIndex(double value) {
        this.updraftIndex = clamp01(value);
    }

    public void nudgeUpdraftToward(double target, double rate) {
        double t = clamp01(target);
        double r = clamp01(rate);
        updraftIndex = clamp01(updraftIndex + (t - updraftIndex) * r);
    }

    public double getPrecipRate() {
        return precipRate;
    }

    public void setPrecipRate(double value) {
        this.precipRate = clamp01(value);
    }

    public void nudgePrecipToward(double target, double rate) {
        double t = clamp01(target);
        double r = clamp01(rate);
        precipRate = clamp01(precipRate + (t - precipRate) * r);
    }

    public double getRainRate() {
        return rainRate;
    }

    public void setRainRate(double value) {
        this.rainRate = clamp01(value);
    }

    public double getSnowRate() {
        return snowRate;
    }

    public void setSnowRate(double value) {
        this.snowRate = clamp01(value);
    }

    public double getCloudiness() {
        return cloudiness;
    }

    public void setCloudiness(double value) {
        this.cloudiness = clamp01(value);
    }

    public void nudgeCloudinessToward(double target, double rate) {
        double t = clamp01(target);
        double r = clamp01(rate);
        cloudiness = clamp01(cloudiness + (t - cloudiness) * r);
    }

    private double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    // ------------------------
    // AIR MASS
    // ------------------------

    public AirMassType getAirMassType() {
        return airMassType;
    }

    // ------------------------
    // SURFACE NOISE
    // ------------------------

    public void updateSurfaceNoise() {

        double pressureVariance = Config.PRESSURE_VARIANCE.get();
        double tempVariance = Config.TEMPERATURE_VARIANCE.get();

        surfaceTemperature += (ThreadLocalRandom.current().nextDouble() - 0.5) * tempVariance;

        surfacePressure -= surfaceTemperature * AtmoConfig.TEMP_PRESSURE_FACTOR;
        surfacePressure += (ThreadLocalRandom.current().nextDouble() - 0.5) * pressureVariance;

        airMassType = AirMassType.classify(surfaceTemperature, surfaceMoisture);
    }
}
