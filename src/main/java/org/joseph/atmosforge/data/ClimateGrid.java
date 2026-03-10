package org.joseph.atmosforge.data;


import net.minecraft.server.level.ServerLevel;
import org.joseph.atmosforge.atmosphere.BiomeTemperatureResolver;
import org.joseph.atmosforge.atmosphere.PressureCell;
import org.joseph.atmosforge.core.AtmoConfig;
import org.joseph.atmosforge.core.AtmosSavedData;


import java.util.HashMap;
import java.util.Map;


public class ClimateGrid {


    private final Map<RegionPos, PressureCell> grid = new HashMap<>();


    public Map<RegionPos, PressureCell> getGrid() {
        return grid;
    }


    public PressureCell getOrCreateRegion(ServerLevel level,
                                          RegionPos pos,
                                          double thermalShift,
                                          AtmosSavedData savedData) {


        return grid.computeIfAbsent(pos, p -> {


            double temperature =
                    BiomeTemperatureResolver.resolveTemperature(level, p.x(), p.z())
                            + thermalShift;


            double moisture = 0.5;


            double pressure = savedData.getPressure(p.x(), p.z());
            if (pressure <= 0.0) {
                pressure = AtmoConfig.STANDARD_PRESSURE;
            }


            return new PressureCell(temperature, moisture, pressure);
        });
    }
}

