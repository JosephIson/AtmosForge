package org.joseph.atmosforge.storm;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.joseph.atmosforge.core.WorldDataManager;
import org.joseph.atmosforge.data.RegionPos;

public final class StormWeatherController {

    private StormWeatherController() {}

    public static void apply(ServerLevel level) {

        WorldDataManager data = WorldDataManager.get(level);
        StormRegistry registry = data.getStormRegistry();

        double maxIntensity = 0.0;
        StormType dominantType = StormType.NONE;

        // Find strongest storm near players
        for (ServerPlayer player : level.players()) {

            int regionX = player.chunkPosition().x >> 4;
            int regionZ = player.chunkPosition().z >> 4;

            RegionPos pos = new RegionPos(regionX, regionZ);

            StormCell storm = registry.get(pos);
            if (storm == null) continue;

            if (storm.getIntensity() > maxIntensity) {
                maxIntensity = storm.getIntensity();
                dominantType = storm.getType();
            }
        }

        if (maxIntensity <= 0.05) {
            clearWeather(level);
            return;
        }

        applyWeather(level, dominantType, maxIntensity);
    }

    private static void applyWeather(ServerLevel level,
                                     StormType type,
                                     double intensity) {

        int rainTime = 6000;
        int thunderTime = 6000;

        level.setWeatherParameters(
                0,
                rainTime,
                true,
                type == StormType.THUNDERSTORMS ||
                        type == StormType.SUPERCELL
        );
    }

    private static void clearWeather(ServerLevel level) {
        level.setWeatherParameters(
                6000,
                0,
                false,
                false
        );
    }
}
