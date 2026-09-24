package io.github.rohrl.interstellar.gravity;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.networking.v1.*;

/** Small immutable configuration snapshot; client rope visuals never read server globals. */
public record GravityVisualPayload(boolean enabled,double strength) implements CustomPayload {
    public static final Id<GravityVisualPayload> ID=new Id<>(Identifier.of("interstellar","gravity_visuals"));
    public static final PacketCodec<RegistryByteBuf,GravityVisualPayload> CODEC=new PacketCodec<>() {
        public GravityVisualPayload decode(RegistryByteBuf b) {return new GravityVisualPayload(b.readBoolean(),b.readDouble());}
        public void encode(RegistryByteBuf b,GravityVisualPayload p) {b.writeBoolean(p.enabled);b.writeDouble(p.strength);}
    };
    public Id<? extends CustomPayload> getId() {return ID;}
    public static void register() {
        PayloadTypeRegistry.playS2C().register(ID,CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->send(handler.player));
    }
    public static void send(ServerPlayerEntity player) {
        if(ServerPlayNetworking.canSend(player,ID))ServerPlayNetworking.send(player,new GravityVisualPayload(GravitySources.config.enabled,GravitySources.config.strengthPerBlock));
    }
}
