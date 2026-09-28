package io.github.rohrl.interstellar.wormhole;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;

public record WormholeLayoutPayload(WormholePair.Layout layout) implements CustomPayload {
    public static final Id<WormholeLayoutPayload> ID=new Id<>(Identifier.of("interstellar","wormhole_layout"));
    public static final PacketCodec<RegistryByteBuf,WormholeLayoutPayload> CODEC=new PacketCodec<>() {
        public WormholeLayoutPayload decode(RegistryByteBuf b) {
            var dimension=RegistryKey.of(RegistryKeys.WORLD,b.readIdentifier());long revision=b.readVarLong();boolean demo=b.readBoolean();
            int size=b.readVarInt();if(size<0||size>2)throw new IllegalArgumentException("Invalid mouth count");
            var mouths=new ArrayList<Vec3d>();for(int i=0;i<size;i++)mouths.add(new Vec3d(b.readDouble(),b.readDouble(),b.readDouble()));
            return new WormholeLayoutPayload(new WormholePair.Layout(dimension,mouths,revision,demo));
        }
        public void encode(RegistryByteBuf b,WormholeLayoutPayload p) {
            var l=p.layout;b.writeIdentifier(l.dimension().getValue());b.writeVarLong(l.revision());b.writeBoolean(l.demo());b.writeVarInt(l.mouths().size());
            for(var c:l.mouths()){b.writeDouble(c.x);b.writeDouble(c.y);b.writeDouble(c.z);}
        }
    };
    public Id<? extends CustomPayload> getId(){return ID;}
}
