package org.joseph.atmosforge.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.joseph.atmosforge.Atmosforge;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → client packet carrying the position, intensity, and stage of
 * every active tornado in the loaded region, sent at the same cadence as
 * StormDataPayload (every STORM_SYNC_INTERVAL simulation ticks).
 */
public record TornadoDataPayload(List<Entry> entries)
        implements CustomPacketPayload {

    public static final Type<TornadoDataPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    Atmosforge.MODID, "tornado_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TornadoDataPayload> STREAM_CODEC =
            new StreamCodec<>() {

                @Override
                public TornadoDataPayload decode(RegistryFriendlyByteBuf buf) {
                    int size = buf.readVarInt();
                    List<Entry> list = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        float wx = buf.readFloat();
                        float wz = buf.readFloat();
                        float intensity = buf.readFloat();
                        byte stage = buf.readByte();
                        list.add(new Entry(wx, wz, intensity, stage));
                    }
                    return new TornadoDataPayload(list);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf,
                                   TornadoDataPayload payload) {
                    buf.writeVarInt(payload.entries.size());
                    for (Entry e : payload.entries) {
                        buf.writeFloat(e.worldX());
                        buf.writeFloat(e.worldZ());
                        buf.writeFloat(e.intensity());
                        buf.writeByte(e.stage());
                    }
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * @param worldX    block-space X of the funnel centre
     * @param worldZ    block-space Z of the funnel centre
     * @param intensity 0..1 (used to scale funnel radius and height client-side)
     * @param stage     0=FORMING  1=MATURE  2=DISSIPATING
     */
    public record Entry(float worldX, float worldZ, float intensity, byte stage) {}
}