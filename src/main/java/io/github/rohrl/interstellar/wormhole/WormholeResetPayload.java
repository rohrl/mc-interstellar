package io.github.rohrl.interstellar.wormhole;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Request an upright camera without moving the player or changing their aim. */
public record WormholeResetPayload() implements CustomPayload {
    public static final WormholeResetPayload INSTANCE=new WormholeResetPayload();
    public static final Id<WormholeResetPayload> ID=new Id<>(Identifier.of("interstellar","wormhole_reset"));
    public static final PacketCodec<RegistryByteBuf,WormholeResetPayload> CODEC=PacketCodec.unit(INSTANCE);
    public Id<? extends CustomPayload> getId() {return ID;}
}
