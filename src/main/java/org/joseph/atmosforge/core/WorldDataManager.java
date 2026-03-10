package org.joseph.atmosforge.core;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.joseph.atmosforge.atmosphere.JetStream;
import org.joseph.atmosforge.atmosphere.SeasonModel;
import org.joseph.atmosforge.data.ClimateGrid;
import org.joseph.atmosforge.storm.StormRegistry;

import java.util.HashMap;
import java.util.Map;

public final class WorldDataManager {

    private static final Map<ServerLevel, WorldDataManager> INSTANCES = new HashMap<>();

    private final ClimateGrid climateGrid;
    private final JetStream jetStream;
    private final SeasonModel seasonModel;
    private final StormRegistry stormRegistry;

    private WorldDataManager() {
        this.climateGrid = new ClimateGrid();
        this.jetStream = new JetStream();
        this.seasonModel = new SeasonModel();
        this.stormRegistry = new StormRegistry();
    }

    public static WorldDataManager get(ServerLevel level) {
        return INSTANCES.computeIfAbsent(level, l -> new WorldDataManager());
    }

    public ClimateGrid getClimateGrid() {
        return climateGrid;
    }

    public JetStream getJetStream() {
        return jetStream;
    }

    public SeasonModel getSeasonModel() {
        return seasonModel;
    }

    public StormRegistry getStormRegistry() {
        return stormRegistry;
    }

    public AtmosSavedData getSavedData(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();

        return storage.computeIfAbsent(
                new SavedData.Factory<>(
                        AtmosSavedData::new,
                        AtmosSavedData::load
                ),
                AtmosSavedData.DATA_NAME
        );
    }
}
