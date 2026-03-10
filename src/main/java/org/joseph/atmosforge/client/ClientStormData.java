package org.joseph.atmosforge.client;

import org.joseph.atmosforge.network.StormDataPayload;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientStormData {

    private ClientStormData() {}

    public record StormSample(
            int regionX,
            int regionZ,
            int type,
            float intensity,
            float cloudiness,
            float shear,
            float upperWindX,
            float upperWindZ
    ) {}

    private static final Map<Long, StormSample> STORMS =
            new ConcurrentHashMap<>();

    public static void accept(StormDataPayload payload) {

        STORMS.clear();

        for (StormDataPayload.Entry e : payload.entries()) {

            long key = key(e.regionX(), e.regionZ());

            STORMS.put(key,
                    new StormSample(
                            e.regionX(),
                            e.regionZ(),
                            e.type(),
                            e.intensity(),
                            e.cloudiness(),
                            e.shear(),
                            e.upperWindX(),
                            e.upperWindZ()
                    ));
        }
    }

    public static Map<Long, StormSample> getAll() {
        return STORMS;
    }

    private static long key(int x, int z) {
        return (((long) x) << 32) | (z & 0xffffffffL);
    }
}

