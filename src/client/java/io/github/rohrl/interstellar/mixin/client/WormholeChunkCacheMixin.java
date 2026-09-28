package io.github.rohrl.interstellar.mixin.client;

import io.github.rohrl.interstellar.client.StreamingTerrain;
import io.github.rohrl.interstellar.client.WormholeChunkCache;
import io.github.rohrl.interstellar.wormhole.WormholePair;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Fixed-size, world-owned cache outside vanilla's moving ring buffer. */
@Mixin(ClientChunkManager.class)
abstract class WormholeChunkCacheMixin implements WormholeChunkCache {
    @Shadow @Final private ClientWorld world;
    @Unique private final Map<Long,WorldChunk> interstellar$chunks=new ConcurrentHashMap<>();
    @Unique private boolean interstellar$retained(int x,int z) {return WormholePair.active(world)&&WormholePair.contains(x,z);}
    @Override public int interstellar$remoteChunkCount() {return interstellar$chunks.size();}
    @Inject(method="getChunk(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/WorldChunk;",at=@At("HEAD"),cancellable=true)
    private void interstellar$get(int x,int z,ChunkStatus status,boolean create,CallbackInfoReturnable<WorldChunk> cir) {
        if(!WormholePair.active(world))return;
        var chunk=interstellar$chunks.get(ChunkPos.toLong(x,z));if(chunk!=null)cir.setReturnValue(chunk);
    }
    @Inject(method="loadChunkFromPacket",at=@At("HEAD"),cancellable=true)
    private void interstellar$load(int x,int z,PacketByteBuf buf,NbtCompound nbt,Consumer<ChunkData.BlockEntityVisitor> consumer,CallbackInfoReturnable<WorldChunk> cir) {
        if(!interstellar$retained(x,z))return;
        var pos=new ChunkPos(x,z);var chunk=interstellar$chunks.computeIfAbsent(pos.toLong(),key->new WorldChunk(world,pos));
        chunk.loadFromPacket(buf,nbt,consumer);world.resetChunkColor(pos);
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)StreamingTerrain.dirty(x+dx,z+dz);
        cir.setReturnValue(chunk);
    }
    @Inject(method="onChunkBiomeData",at=@At("HEAD"),cancellable=true)
    private void interstellar$biomes(int x,int z,PacketByteBuf buf,CallbackInfo ci) {
        if(!interstellar$retained(x,z))return;
        var chunk=interstellar$chunks.get(ChunkPos.toLong(x,z));if(chunk!=null)chunk.loadBiomeFromPacket(buf);ci.cancel();
    }
    @Inject(method="unload",at=@At("HEAD"),cancellable=true)
    private void interstellar$keep(ChunkPos pos,CallbackInfo ci) {if(interstellar$retained(pos.x,pos.z))ci.cancel();}
}
