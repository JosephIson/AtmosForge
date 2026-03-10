package org.joseph.atmosforge.client;

import org.joseph.atmosforge.network.TornadoDataPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side cache of active tornadoes synced from the server.
 * Replaced atomically on each received TornadoDataPayload to keep
 * the renderer and the network handler on separate threads.
 */
public final class ClientTornadoData {

    private ClientTornadoData() {}

    private static volatile List<TornadoSample> TORNADOS = List.of();

    public static void accept(TornadoDataPayload payload) {
        List<TornadoSample> list = new ArrayList<>(payload.entries().size());
        for (TornadoDataPayload.Entry e : payload.entries()) {
            list.add(new TornadoSample(e.worldX(), e.worldZ(), e.intensity(), e.stage()));
        }
        TORNADOS = List.copyOf(list);
    }

    public static List<TornadoSample> getAll() {
        return TORNADOS;
    }

    /**
     * @param worldX    block-space X of funnel centre
     * @param worldZ    block-space Z of funnel centre
     * @param intensity 0..1
     * @param stage     0=FORMING  1=MATURE  2=DISSIPATING
     */
    public record TornadoSample(float worldX, float worldZ, float intensity, byte stage) {}
}