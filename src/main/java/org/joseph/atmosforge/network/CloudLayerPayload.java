package org.joseph.atmosforge.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.joseph.atmosforge.Atmosforge;

import java.util.ArrayList;
import java.util.List;

public record CloudLayerPayload(List<Entry> entries, long gameTime) implements CustomPacketPayload {

    public static final Type<CloudLayerPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Atmosforge.MODID, "cloud_layer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CloudLayerPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public CloudLayerPayload decode(RegistryFriendlyByteBuf buf) {
                    long time = buf.readLong();
                    int count = buf.readVarInt();
                    List<Entry> list = new ArrayList<>(count);

                    for (int i = 0; i < count; i++) {
                        int rx = buf.readVarInt();
                        int rz = buf.readVarInt();
                        float cloudiness = buf.readFloat(); // 0..1
                        float base = buf.readFloat();       // 0..1
                        list.add(new Entry(rx, rz, cloudiness, base));
                    }
                    return new CloudLayerPayload(list, time);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, CloudLayerPayload payload) {
                    buf.writeLong(payload.gameTime());
                    buf.writeVarInt(payload.entries().size());
                    for (Entry e : payload.entries()) {
                        buf.writeVarInt(e.regionX());
                        buf.writeVarInt(e.regionZ());
                        buf.writeFloat(e.cloudiness());
                        buf.writeFloat(e.baseDensity());
                    }
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(int regionX, int regionZ, float cloudiness, float baseDensity) {}
}

