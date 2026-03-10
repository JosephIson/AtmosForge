package org.joseph.atmosforge.client;

import org.joseph.atmosforge.network.CloudLayerPayload;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientCloudData {

    private ClientCloudData() {}

    public record CloudSample(int regionX, int regionZ, float cloudiness, float baseDensity) {}

    private static final Map<Long, CloudSample> CLOUDS = new ConcurrentHashMap<>();
    private static volatile long lastGameTime = 0L;

    public static void accept(CloudLayerPayload payload) {
        CLOUDS.clear();
        for (CloudLayerPayload.Entry e : payload.entries()) {
            CLOUDS.put(key(e.regionX(), e.regionZ()),
                    new CloudSample(e.regionX(), e.regionZ(), e.cloudiness(), e.baseDensity()));
        }
        lastGameTime = payload.gameTime();
    }

    public static CloudSample get(int rx, int rz) {
        return CLOUDS.get(key(rx, rz));
    }

    public static long getLastGameTime() {
        return lastGameTime;
    }

    public static long key(int rx, int rz) {
        return (((long) rx) << 32) | (rz & 0xffffffffL);
    }
}

