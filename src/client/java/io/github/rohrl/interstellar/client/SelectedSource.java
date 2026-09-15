package io.github.rohrl.interstellar.client;

import io.github.rohrl.interstellar.source.SourcePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

/** Main-client-thread state. Never share the integrated server's mutable world objects. */
final class SelectedSource {
    private static SourcePayload source;
    private static ClientWorld world;
    static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->clear());
        ClientPlayNetworking.registerGlobalReceiver(SourcePayload.ID,(payload,context)-> {
            var client=context.client();
            if (payload.count()==0) { clear(); return; }
            if (client.world==null || !client.world.getRegistryKey().getValue().equals(payload.dimension())) return;
            source=payload; world=client.world;
        });
    }
    private static void clear() { source=null; world=null; }
    static SourcePayload current() {
        if (MinecraftClient.getInstance().world!=world) clear();
        return source;
    }
}