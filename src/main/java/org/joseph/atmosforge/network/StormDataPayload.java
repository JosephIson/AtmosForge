package org.joseph.atmosforge.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.joseph.atmosforge.Atmosforge;

import java.util.ArrayList;
import java.util.List;

public record StormDataPayload(List<Entry> entries)
        implements CustomPacketPayload {

    public static final Type<StormDataPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    Atmosforge.MODID, "storm_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StormDataPayload> STREAM_CODEC =
            new StreamCodec<>() {

                @Override
                public StormDataPayload decode(RegistryFriendlyByteBuf buf) {

                    int size = buf.readVarInt();
                    List<Entry> list = new ArrayList<>(size);

                    for (int i = 0; i < size; i++) {
                        int rx = buf.readVarInt();
                        int rz = buf.readVarInt();
                        int type = buf.readVarInt();
                        float intensity = buf.readFloat();
                        float cloudiness = buf.readFloat();
                        float shear = buf.readFloat();
                        float upperWindX = buf.readFloat();
                        float upperWindZ = buf.readFloat();

                        list.add(new Entry(
                                rx, rz, type,
                                intensity,
                                cloudiness,
                                shear,
                                upperWindX,
                                upperWindZ));
                    }

                    return new StormDataPayload(list);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf,
                                   StormDataPayload payload) {

                    buf.writeVarInt(payload.entries.size());

                    for (Entry e : payload.entries) {

                        buf.writeVarInt(e.regionX());
                        buf.writeVarInt(e.regionZ());
                        buf.writeVarInt(e.type());

                        buf.writeFloat(e.intensity());
                        buf.writeFloat(e.cloudiness());
                        buf.writeFloat(e.shear());
                        buf.writeFloat(e.upperWindX());
                        buf.writeFloat(e.upperWindZ());
                    }
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(
            int regionX,
            int regionZ,
            int type,
            float intensity,
            float cloudiness,
            float shear,
            float upperWindX,
            float upperWindZ
    ) {}
}

