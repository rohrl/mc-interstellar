package io.github.rohrl.interstellar.wormhole;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/** Sent immediately before the native authoritative teleport, never instead of it. */
public record WormholeTransitPayload(int from,Vec3d eye,Vec3d target,double roll) implements CustomPayload {
    public static final Id<WormholeTransitPayload> ID=new Id<>(Identifier.of("interstellar","wormhole_transit"));
    public static final PacketCodec<RegistryByteBuf,WormholeTransitPayload> CODEC=new PacketCodec<>() {
        public WormholeTransitPayload decode(RegistryByteBuf b) {
            int from=b.readVarInt();if(from<0||from>1)throw new IllegalArgumentException("Invalid wormhole end");
            return new WormholeTransitPayload(from,read(b),read(b),b.readDouble());
        }
        public void encode(RegistryByteBuf b,WormholeTransitPayload p) {
            b.writeVarInt(p.from);write(b,p.eye);write(b,p.target);b.writeDouble(p.roll);
        }
        private Vec3d read(RegistryByteBuf b) {return new Vec3d(b.readDouble(),b.readDouble(),b.readDouble());}
        private void write(RegistryByteBuf b,Vec3d v) {b.writeDouble(v.x);b.writeDouble(v.y);b.writeDouble(v.z);}
    };
    public Id<? extends CustomPayload> getId() {return ID;}
}
