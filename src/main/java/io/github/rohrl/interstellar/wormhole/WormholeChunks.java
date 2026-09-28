package io.github.rohrl.interstellar.wormhole;

import io.github.rohrl.interstellar.Interstellar;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Bounded native remote-world delivery. No joins, forced synchronous loads or full-world scans. */
public final class WormholeChunks {
    private static final ChunkTicketType<ChunkPos> TICKET=ChunkTicketType.create("interstellar_wormhole",Comparator.comparingLong(ChunkPos::toLong));
    private static volatile Session session;
    private static long generation;
    private WormholeChunks() {}
    private static final class Viewer {
        final long generation=++WormholeChunks.generation;
        final LinkedHashSet<Long> pending=new LinkedHashSet<>();
        boolean announced,ready;
        Viewer() {for(var p:WormholePair.CHUNKS)pending.add(p.toLong());}
    }
    private static final class Session {
        final ServerWorld world;
        final Set<Long> dirty=ConcurrentHashMap.newKeySet();
        final Map<UUID,Viewer> viewers=new LinkedHashMap<>();
        int tickets,ticks,viewerCursor;
        Session(ServerWorld world) {this.world=world;}
        void close() {
            for(int i=0;i<tickets;i++) {var p=WormholePair.CHUNKS.get(i);world.getChunkManager().removeTicket(TICKET,p,1,p);}
            Interstellar.LOGGER.info("Wormhole remote regions released: {} tickets",tickets);
        }
    }
    public static void register() {
        PayloadTypeRegistry.playS2C().register(WormholeReadyPayload.ID,WormholeReadyPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(WormholeReadyPayload.ID,WormholeReadyPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(WormholeReadyPayload.ID,(payload,context)-> {
            var s=session;var player=context.player();
            if(s==null||player.getServerWorld()!=s.world)return;
            var viewer=s.viewers.get(player.getUuid());
            if(viewer!=null && viewer.announced && viewer.generation==payload.generation()) {
                viewer.ready=true;
                Interstellar.LOGGER.info("Wormhole destination ready on client: {}, generation={}, chunks={}",player.getName().getString(),viewer.generation,WormholePair.CHUNKS.size());
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(WormholeChunks::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server->session=null);
    }
    public static boolean ready(ServerPlayerEntity player) {
        var s=session;if(s==null||player.getServerWorld()!=s.world)return false;
        var viewer=s.viewers.get(player.getUuid());return viewer!=null&&viewer.ready;
    }
    public static String status(ServerPlayerEntity player) {
        var s=session;if(s==null||s.world!=player.getServerWorld())return "Remote regions inactive";
        var v=s.viewers.get(player.getUuid());if(v==null)return "Preparing remote viewer";
        if(v.ready&&v.pending.isEmpty())ServerPlayNetworking.send(player,new WormholeReadyPayload(v.generation));
        return "Remote regions: "+s.tickets+"/"+WormholePair.CHUNKS.size()+" tickets, "+v.pending.size()+" pending chunks; client "+(v.ready?"ready":"loading");
    }
    /** Also called from the lighting worker; only bounded keys enter the concurrent set. */
    public static void dirty(ServerWorld world,int x,int z) {
        var s=session;if(s!=null && s.world==world && WormholePair.contains(x,z))s.dirty.add(ChunkPos.toLong(x,z));
    }
    private static void tick(MinecraftServer server) {
        var world=server.getWorld(WormholePair.WORLD);
        if(world==null || world.getPlayers().isEmpty()) {
            var old=session;session=null;if(old!=null)old.close();return;
        }
        if(session==null)session=new Session(world);
        var s=session;s.ticks++;
        var players=world.getPlayers();
        s.viewers.keySet().removeIf(id->players.stream().noneMatch(p->p.getUuid().equals(id)));
        for(var p:players)s.viewers.computeIfAbsent(p.getUuid(),id->new Viewer());
        // Ticket propagation does the asynchronous work. getWorldChunk(x,z) below
        // is Minecraft's nonblocking lookup; its similarly named future method waits.
        for(int n=0;n<2 && s.tickets<WormholePair.CHUNKS.size();n++) {
            var p=WormholePair.CHUNKS.get(s.tickets++);world.getChunkManager().addTicket(TICKET,p,1,p);
        }
        if(s.ticks%4==0)for(var it=s.dirty.iterator();it.hasNext();) {
            long key=it.next();it.remove();for(var viewer:s.viewers.values())viewer.pending.add(key);
        }
        int sent=0;long deadline=System.nanoTime()+2_000_000;
        int start=s.viewerCursor++%players.size();
        for(int i=0;i<players.size() && sent<2 && System.nanoTime()<deadline;i++) {
            var player=players.get((start+i)%players.size());var viewer=s.viewers.get(player.getUuid());
            int examined=0;
            for(var it=viewer.pending.iterator();it.hasNext() && sent<2 && examined++<32 && System.nanoTime()<deadline;) {
                long key=it.next();var chunk=world.getChunkManager().getWorldChunk(ChunkPos.getPackedX(key),ChunkPos.getPackedZ(key));
                if(chunk==null || !chunk.isLightOn())continue;
                player.networkHandler.sendPacket(new ChunkDataS2CPacket(chunk,world.getChunkManager().getLightingProvider(),null,null));
                it.remove();sent++;
            }
            if(viewer.pending.isEmpty() && !viewer.announced) {
                viewer.announced=true;ServerPlayNetworking.send(player,new WormholeReadyPayload(viewer.generation));
            }
        }
    }
}
