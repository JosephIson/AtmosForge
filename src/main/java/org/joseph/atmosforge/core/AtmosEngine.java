package org.joseph.atmosforge.core;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import org.joseph.atmosforge.Config;
import org.joseph.atmosforge.atmosphere.JetStream;
import org.joseph.atmosforge.atmosphere.PressureCell;
import org.joseph.atmosforge.atmosphere.WindVector;
import org.joseph.atmosforge.data.ClimateGrid;
import org.joseph.atmosforge.data.RegionPos;
import org.joseph.atmosforge.network.AtmoNetwork;
import org.joseph.atmosforge.network.CloudLayerPayload;
import org.joseph.atmosforge.network.StormDataPayload;
import org.joseph.atmosforge.network.TornadoDataPayload;
import org.joseph.atmosforge.storm.StormCell;
import org.joseph.atmosforge.storm.StormCoreSystem;
import org.joseph.atmosforge.storm.StormRegistry;
import org.joseph.atmosforge.storm.TornadoCell;
import org.joseph.atmosforge.storm.TornadoLifecycleModel;
import org.joseph.atmosforge.storm.TornadoRegistry;

import java.util.*;

public class AtmosEngine {

    private static final int REGION_SHIFT = 4;
    private static final int STORM_SYNC_INTERVAL = 10;

    private int stormSyncCounter = 0;

    public void tick(ServerLevel level) {

        WorldDataManager data = WorldDataManager.get(level);

        ClimateGrid grid = data.getClimateGrid();
        JetStream jetStream = data.getJetStream();
        StormRegistry stormRegistry = data.getStormRegistry();
        TornadoRegistry tornadoRegistry = data.getTornadoRegistry();
        AtmosSavedData savedData = data.getSavedData(level);

        int simulationRadius = Config.SIMULATION_RADIUS.get();
        double thermalShift = 0.0;

        Map<RegionPos, PressureCell> activeRegions = new HashMap<>();

        for (ServerPlayer player : level.players()) {

            int regionX = player.chunkPosition().x >> REGION_SHIFT;
            int regionZ = player.chunkPosition().z >> REGION_SHIFT;

            for (int dx = -simulationRadius; dx <= simulationRadius; dx++) {
                for (int dz = -simulationRadius; dz <= simulationRadius; dz++) {

                    RegionPos pos = new RegionPos(regionX + dx, regionZ + dz);
                    PressureCell cell =
                            grid.getOrCreateRegion(level, pos, thermalShift, savedData);

                    activeRegions.put(pos, cell);
                }
            }
        }

        applyUpperJet(activeRegions, jetStream);
        computeShearIndex(activeRegions);

        StormCoreSystem.apply(activeRegions, stormRegistry);
        TornadoLifecycleModel.apply(activeRegions, stormRegistry, tornadoRegistry);

        stormSyncCounter++;
        if (stormSyncCounter >= STORM_SYNC_INTERVAL) {
            stormSyncCounter = 0;
            sendCloudData(level, activeRegions);
            sendStormData(level, stormRegistry, activeRegions);
            sendTornadoData(level, tornadoRegistry);
        }
    }

    private void applyUpperJet(Map<RegionPos, PressureCell> regions,
                               JetStream jetStream) {

        jetStream.tick();

        for (Map.Entry<RegionPos, PressureCell> entry : regions.entrySet()) {

            WindVector jet = jetStream.getJetVector(entry.getKey());
            PressureCell cell = entry.getValue();

            if (jet != null)
                cell.setUpperWind(jet.getX(), jet.getZ());
        }
    }

    private void computeShearIndex(Map<RegionPos, PressureCell> regions) {

        for (PressureCell cell : regions.values()) {

            double dx = cell.getUpperWind().getX()
                    - cell.getSurfaceWind().getX();

            double dz = cell.getUpperWind().getZ()
                    - cell.getSurfaceWind().getZ();

            cell.setShearIndex(Math.sqrt(dx * dx + dz * dz));
        }
    }

    private void sendCloudData(ServerLevel level,
                               Map<RegionPos, PressureCell> regions) {

        ArrayList<CloudLayerPayload.Entry> list = new ArrayList<>();

        for (Map.Entry<RegionPos, PressureCell> e : regions.entrySet()) {
            PressureCell cell = e.getValue();

            // PrecipitationModel sets cloudiness, but even without the full
            // physics pipeline running we can derive a display value from
            // surface moisture so clouds are always visible.
            float cloudiness = (float) Math.max(
                    cell.getCloudiness(),
                    cell.getSurfaceMoisture() * 0.6);

            // baseDensity gives the cloud layer its base opacity.
            // Moisture of 0.5 (the initialised default) maps to 0.42,
            // giving a modest visible cloud cover right away.
            float baseDensity = (float) Math.max(0.15, cell.getSurfaceMoisture() * 0.85);

            list.add(new CloudLayerPayload.Entry(
                    e.getKey().x(),
                    e.getKey().z(),
                    Mth.clamp(cloudiness, 0f, 1f),
                    Mth.clamp(baseDensity, 0f, 1f)
            ));
        }

        CloudLayerPayload payload = new CloudLayerPayload(list, level.getGameTime());
        for (ServerPlayer player : level.players()) {
            AtmoNetwork.sendTo(player, payload);
        }
    }

    private void sendTornadoData(ServerLevel level, TornadoRegistry registry) {

        ArrayList<TornadoDataPayload.Entry> list = new ArrayList<>();

        for (TornadoCell t : registry.getAll()) {
            list.add(new TornadoDataPayload.Entry(
                    (float) t.getWorldX(),
                    (float) t.getWorldZ(),
                    (float) t.getIntensity(),
                    (byte) t.getStage().ordinal()
            ));
        }

        TornadoDataPayload payload = new TornadoDataPayload(list);
        for (ServerPlayer player : level.players()) {
            AtmoNetwork.sendTo(player, payload);
        }
    }

    private void sendStormData(ServerLevel level,
                               StormRegistry registry,
                               Map<RegionPos, PressureCell> regions) {

        ArrayList<StormDataPayload.Entry> list = new ArrayList<>();

        for (Map.Entry<RegionPos, PressureCell> e : regions.entrySet()) {

            StormCell storm = registry.get(e.getKey());
            if (storm == null) continue;

            float intensity = (float) storm.getIntensity();

            list.add(new StormDataPayload.Entry(
                    e.getKey().x(),
                    e.getKey().z(),
                    storm.getType().ordinal(),
                    intensity,
                    (float) e.getValue().getCloudiness(),
                    (float) e.getValue().getShearIndex(),
                    (float) e.getValue().getUpperWind().getX(),
                    (float) e.getValue().getUpperWind().getZ()
            ));
        }

        StormDataPayload payload = new StormDataPayload(list);

        for (ServerPlayer player : level.players()) {
            AtmoNetwork.sendTo(player, payload);
        }
    }
}

