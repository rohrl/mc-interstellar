package io.github.rohrl.interstellar.client;

import io.github.rohrl.interstellar.source.SourcePayload;
import io.github.rohrl.interstellar.source.SourceState;
import io.github.rohrl.interstellar.source.SourceStatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

/** Main-client-thread state. Never share the integrated server's mutable world objects. */
final class SelectedSource {
    private static SourcePayload source;
    private static ClientWorld world;
    private static SourceState state=SourceState.NONE;
    private static long refreshingSince;
    static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->clear());
        ClientPlayNetworking.registerGlobalReceiver(SourcePayload.ID,(payload,context)-> {
            var client=context.client();
            if (payload.count()==0) { clear(); return; }
            if (client.world==null || !client.world.getRegistryKey().getValue().equals(payload.dimension())) return;
            source=payload; world=client.world;state=SourceState.READY;
        });
        ClientPlayNetworking.registerGlobalReceiver(SourceStatePayload.ID,(payload,context)-> {
            var client=context.client();
            if(client.world==null||!client.world.getRegistryKey().getValue().equals(payload.dimension()))return;
            if(payload.state()==SourceState.NONE) {clear();return;}
            if(payload.state()!=SourceState.REFRESHING)source=null;
            else if(state!=SourceState.REFRESHING)refreshingSince=client.world.getTime();
            world=client.world;state=payload.state();
        });
    }
    private static void clear() { source=null; world=null;state=SourceState.NONE; }
    static SourceState state() {current();return state;}
    static SourcePayload current() {
        if (MinecraftClient.getInstance().world!=world) clear();
        if(world!=null&&state==SourceState.REFRESHING&&world.getTime()-refreshingSince>=20)source=null;
        return source;
    }
}
