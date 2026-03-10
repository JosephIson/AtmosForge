package org.joseph.atmosforge.network;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.joseph.atmosforge.client.ClientCloudData;
import org.joseph.atmosforge.client.ClientStormData;
import org.joseph.atmosforge.client.ClientTornadoData;

public final class AtmoNetwork {

    private static final String PROTOCOL = "1";

    private AtmoNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {

        PayloadRegistrar registrar = event.registrar(PROTOCOL);

        registrar.playToClient(
                StormDataPayload.TYPE,
                StormDataPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (Minecraft.getInstance().level == null) return;
                    ClientStormData.accept(payload);
                }
        );

        registrar.playToClient(
                CloudLayerPayload.TYPE,
                CloudLayerPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (Minecraft.getInstance().level == null) return;
                    ClientCloudData.accept(payload);
                }
        );

        registrar.playToClient(
                TornadoDataPayload.TYPE,
                TornadoDataPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (Minecraft.getInstance().level == null) return;
                    ClientTornadoData.accept(payload);
                }
        );
    }

    public static void sendTo(ServerPlayer player, StormDataPayload payload) {
        player.connection.send(payload);
    }

    public static void sendTo(ServerPlayer player, CloudLayerPayload payload) {
        player.connection.send(payload);
    }

    public static void sendTo(ServerPlayer player, TornadoDataPayload payload) {
        player.connection.send(payload);
    }
}

