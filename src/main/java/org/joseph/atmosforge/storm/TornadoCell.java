package org.joseph.atmosforge.storm;

import org.joseph.atmosforge.data.RegionPos;

/**
 * Represents a single active tornado vortex.
 * Spawned by TornadoLifecycleModel from mature supercells with
 * sufficient shear, instability, and updraft.
 */
public final class TornadoCell {

    public enum Stage {
        FORMING,       // Funnel extending downward, ramping intensity
        MATURE,        // Full intensity, drifting with parent storm
        DISSIPATING    // Intensity decaying, funnel retracting
    }

    // World-space block position of the funnel center
    private double worldX;
    private double worldZ;

    // 0..1 intensity (0 = just born / just dying, 1 = violent EF5-equivalent)
    private double intensity = 0.0;

    private int ageTicks = 0;
    private Stage stage = Stage.FORMING;

    // Region that spawned this tornado
    private final RegionPos parentRegion;

    public TornadoCell(double worldX, double worldZ, RegionPos parentRegion) {
        this.worldX = worldX;
        this.worldZ = worldZ;
        this.parentRegion = parentRegion;
    }

    // -------------------------------------------------------------------------

    public double getWorldX() { return worldX; }
    public void setWorldX(double worldX) { this.worldX = worldX; }

    public double getWorldZ() { return worldZ; }
    public void setWorldZ(double worldZ) { this.worldZ = worldZ; }

    public double getIntensity() { return intensity; }
    public void setIntensity(double v) {
        this.intensity = Math.max(0.0, Math.min(1.0, v));
    }

    public int getAgeTicks() { return ageTicks; }
    public void incrementAge() { ageTicks++; }

    public Stage getStage() { return stage; }
    public void setStage(Stage stage) { this.stage = stage; }

    public RegionPos getParentRegion() { return parentRegion; }
}