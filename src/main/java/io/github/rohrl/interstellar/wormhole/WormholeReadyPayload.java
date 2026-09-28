package io.github.rohrl.interstellar.wormhole;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Ordered after native chunk/light packets; echoed only after the client applies them. */
public record WormholeReadyPayload(long generation) implements CustomPayload {
    public static final Id<WormholeReadyPayload> ID=new Id<>(Identifier.of("interstellar","wormhole_ready"));
    public static final PacketCodec<RegistryByteBuf,WormholeReadyPayload> CODEC=new PacketCodec<>() {
        public WormholeReadyPayload decode(RegistryByteBuf buf) {return new WormholeReadyPayload(buf.readVarLong());}
        public void encode(RegistryByteBuf buf,WormholeReadyPayload p) {buf.writeVarLong(p.generation);}
    };
    public Id<? extends CustomPayload> getId() {return ID;}
}
