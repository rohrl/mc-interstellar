package io.github.rohrl.interstellar.source;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** State transitions clear stale metadata while preserving the client's armed live-view intent. */
public record SourceStatePayload(Identifier dimension,SourceState state) implements CustomPayload {
    public static final Id<SourceStatePayload> ID=new Id<>(Identifier.of("interstellar","source_state"));
    public static final PacketCodec<RegistryByteBuf,SourceStatePayload> CODEC=new PacketCodec<>() {
        public SourceStatePayload decode(RegistryByteBuf buf) {
            Identifier dimension=buf.readIdentifier();int ordinal=buf.readVarInt();
            if(ordinal<0||ordinal>=SourceState.values().length||ordinal==SourceState.READY.ordinal())
                throw new IllegalArgumentException("Invalid source state");
            return new SourceStatePayload(dimension,SourceState.values()[ordinal]);
        }
        public void encode(RegistryByteBuf buf,SourceStatePayload value) {
            buf.writeIdentifier(value.dimension);buf.writeVarInt(value.state.ordinal());
        }
    };
    public Id<? extends CustomPayload> getId() {return ID;}
}
