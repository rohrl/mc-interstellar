package io.github.rohrl.interstellar.source;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Server-authoritative complete source, or count=0 to clear the previous selection. */
public record SourcePayload(Identifier dimension, int count, double x, double y, double z,
                            double enclosingRadius, double schwarzschildRadius) implements CustomPayload {
    public static final Id<SourcePayload> ID = new Id<>(Identifier.of("interstellar", "source_selection"));
    public static final PacketCodec<RegistryByteBuf, SourcePayload> CODEC = new PacketCodec<>() {
        @Override public SourcePayload decode(RegistryByteBuf buf) {
            return new SourcePayload(buf.readIdentifier(), buf.readVarInt(), buf.readDouble(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble());
        }
        @Override public void encode(RegistryByteBuf buf, SourcePayload value) {
            buf.writeIdentifier(value.dimension); buf.writeVarInt(value.count);
            buf.writeDouble(value.x); buf.writeDouble(value.y); buf.writeDouble(value.z);
            buf.writeDouble(value.enclosingRadius); buf.writeDouble(value.schwarzschildRadius);
        }
    };
    @Override public Id<? extends CustomPayload> getId() { return ID; }
    public boolean blackHoleProxy() {
        return count > 0 && enclosingRadius > 0 && schwarzschildRadius >= enclosingRadius*(1-1e-12);
    }
}
