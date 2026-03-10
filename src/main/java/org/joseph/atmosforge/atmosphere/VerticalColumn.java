package org.joseph.atmosforge.atmosphere;


public final class VerticalColumn {


    private double upperPressure;
    private double upperTemperature;


    public VerticalColumn(PressureCell surface) {
        this.upperPressure = surface.getSurfacePressure() * 0.60;
        this.upperTemperature = surface.getSurfaceTemperature() - 15.0;
    }


    public double getUpperPressure() {
        return upperPressure;
    }


    public void setUpperPressure(double upperPressure) {
        this.upperPressure = upperPressure;
    }


    public double getUpperTemperature() {
        return upperTemperature;
    }


    public void setUpperTemperature(double upperTemperature) {
        this.upperTemperature = upperTemperature;
    }
}

