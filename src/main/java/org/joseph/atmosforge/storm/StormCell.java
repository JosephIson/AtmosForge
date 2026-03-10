package org.joseph.atmosforge.storm;

public final class StormCell {

    private StormType type = StormType.NONE;

    // 0..1
    private double intensity = 0.0;

    // Persistence controls
    private int ageTicks = 0;
    private int belowKillTicks = 0;

    // Useful diagnostics for later rendering/effects decisions
    private double convectiveScore = 0.0;
    private double synopticScore = 0.0;

    public StormType getType() {
        return type;
    }

    public void setType(StormType type) {
        this.type = type;
    }

    public double getIntensity() {
        return intensity;
    }

    public void setIntensity(double intensity) {
        this.intensity = clamp01(intensity);
    }

    public int getAgeTicks() {
        return ageTicks;
    }

    public void setAgeTicks(int ageTicks) {
        this.ageTicks = Math.max(0, ageTicks);
    }

    public void incrementAge() {
        ageTicks++;
    }

    public int getBelowKillTicks() {
        return belowKillTicks;
    }

    public void setBelowKillTicks(int belowKillTicks) {
        this.belowKillTicks = Math.max(0, belowKillTicks);
    }

    public void incrementBelowKill() {
        belowKillTicks++;
    }

    public void resetBelowKill() {
        belowKillTicks = 0;
    }

    public double getConvectiveScore() {
        return convectiveScore;
    }

    public void setConvectiveScore(double convectiveScore) {
        this.convectiveScore = clamp01(convectiveScore);
    }

    public double getSynopticScore() {
        return synopticScore;
    }

    public void setSynopticScore(double synopticScore) {
        this.synopticScore = clamp01(synopticScore);
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }
}
