package org.joseph.atmosforge.atmosphere;

import org.joseph.atmosforge.data.RegionPos;

public class JetStream {

    private double globalShift = 0.0;

    public void tick() {
        // Slowly oscillate over time
        globalShift += 0.001;
    }

    public WindVector getJetVector(RegionPos pos) {

        // Use Z as pseudo-latitude
        double latitudeFactor = Math.sin(pos.z() * 0.02 + globalShift);

        // Strongest at mid-latitudes
        double strength = latitudeFactor * 5.0;

        return new WindVector(strength, 0);
    }
}