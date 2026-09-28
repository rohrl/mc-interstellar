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
    @Unique private boolean interstellar$retained(int x,int z) {return WormholePair.active(world)&&WormholePair.contains(world,x,z);}
    @Override public int interstellar$remoteChunkCount() {return (int)interstellar$chunks.keySet().stream()
        .filter(k->interstellar$retained(ChunkPos.getPackedX(k),ChunkPos.getPackedZ(k))).count();}
    @Override public void interstellar$refreshRegions() {
        var client=net.minecraft.client.MinecraftClient.getInstance();if(client.player==null)return;
        var player=client.player.getChunkPos();int radius=client.options.getViewDistance().getValue()+3;
        for(var it=interstellar$chunks.entrySet().iterator();it.hasNext();) {
            var entry=it.next();var pos=entry.getValue().getPos();
            // Near retired chunks remain usable until vanilla refreshes/unloads them.
            // Their original packet bypassed vanilla's ring, so dropping all would leave holes.
            if(interstellar$retained(pos.x,pos.z) || Math.abs(pos.x-player.x)<=radius && Math.abs(pos.z-player.z)<=radius)continue;
            it.remove();world.unloadBlockEntities(entry.getValue());
            world.enqueueChunkUpdate(()-> {
                if(interstellar$retained(pos.x,pos.z)||world.getChunkManager().isChunkLoaded(pos.x,pos.z))return;
                var light=world.getLightingProvider();light.setColumnEnabled(pos,false);
                for(int y=light.getBottomY();y<light.getTopY();y++) {
                    var section=net.minecraft.util.math.ChunkSectionPos.from(pos,y);
                    light.enqueueSectionData(net.minecraft.world.LightType.BLOCK,section,null);
                    light.enqueueSectionData(net.minecraft.world.LightType.SKY,section,null);
                }
                for(int y=world.getBottomSectionCoord();y<world.getTopSectionCoord();y++)light.setSectionStatus(net.minecraft.util.math.ChunkSectionPos.from(pos,y),true);
            });
        }
    }
    @Inject(method="setChunkMapCenter",at=@At("TAIL"))
    private void interstellar$prune(int x,int z,CallbackInfo ci) {interstellar$refreshRegions();}
    @Inject(method="getChunk(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/WorldChunk;",at=@At("HEAD"),cancellable=true)
    private void interstellar$get(int x,int z,ChunkStatus status,boolean create,CallbackInfoReturnable<WorldChunk> cir) {
        var chunk=interstellar$chunks.get(ChunkPos.toLong(x,z));if(chunk!=null)cir.setReturnValue(chunk);
    }
    @Inject(method="loadChunkFromPacket",at=@At("HEAD"),cancellable=true)
    private void interstellar$load(int x,int z,PacketByteBuf buf,NbtCompound nbt,Consumer<ChunkData.BlockEntityVisitor> consumer,CallbackInfoReturnable<WorldChunk> cir) {
        if(!interstellar$retained(x,z)){
            var old=interstellar$chunks.remove(ChunkPos.toLong(x,z));if(old!=null)world.unloadBlockEntities(old);return;
        }
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
    private void interstellar$keep(ChunkPos pos,CallbackInfo ci) {
        if(interstellar$retained(pos.x,pos.z))ci.cancel();else {
            var old=interstellar$chunks.remove(pos.toLong());if(old!=null)world.unloadBlockEntities(old);
        }
    }
}
