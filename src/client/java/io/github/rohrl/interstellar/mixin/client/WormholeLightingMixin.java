package io.github.rohrl.interstellar.mixin.client;

import io.github.rohrl.interstellar.wormhole.WormholePair;
import io.github.rohrl.interstellar.client.WormholeClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
abstract class WormholeLightingMixin {
    @Shadow private ClientWorld world;
    // Called after the native main-thread gate; cancelling onUnloadChunk at HEAD
    // would run on the network thread. Retain the lighting as well as chunk data.
    @Inject(method="unloadChunk",at=@At("HEAD"),cancellable=true)
    private void interstellar$keep(UnloadChunkS2CPacket packet,CallbackInfo ci) {
        if(WormholePair.active(world)&&WormholePair.contains(packet.pos().x,packet.pos().z))ci.cancel();
    }
    @Inject(method="onPlayerPositionLook",at=@At("TAIL"))
    private void interstellar$transit(net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket packet,CallbackInfo ci) {WormholeClient.afterTeleport();}
}
