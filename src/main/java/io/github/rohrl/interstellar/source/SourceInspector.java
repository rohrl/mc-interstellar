package io.github.rohrl.interstellar.source;

import io.github.rohrl.interstellar.Interstellar;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import java.util.*;
import static net.minecraft.server.command.CommandManager.*;
import static io.github.rohrl.interstellar.source.ClusterProbe.*;

/** Event-driven discovery shared by all viewers; never force-loads chunks. */
public final class SourceInspector {
    private SourceInspector() { }
    private static final Map<ServerWorld,WorldSources> worlds=new IdentityHashMap<>();
    private static final Map<ServerPlayerEntity,Selection> selections=new IdentityHashMap<>();
    private static long ticks;
    private static int worldCursor;
    private static int discoveryCursor;
    private static final class Selection {
        final ServerWorld world;
        Cell requested;
        long id;
        boolean pinned,announce;
        SourcePayload payload;
        SourceState state;
        Selection(ServerWorld world) {this.world=world;}
    }
    private static final class ChunkScan {
        final WorldChunk chunk;
        final int firstSection;
        int section,cell;
        ChunkScan(WorldChunk chunk,int firstSection) {this.chunk=chunk;this.firstSection=firstSection;}
    }
    private static final class WorldSources {
        final ServerWorld world;
        final ClusterTracker tracker;
        final LinkedHashMap<Long,ChunkScan> scans=new LinkedHashMap<>();
        WorldSources(ServerWorld world) {
            this.world=world;
            tracker=new ClusterTracker(cell->state(world,cell),legacy(world)?LEGACY_RADIUS_PER_BLOCK:RADIUS_PER_BLOCK);
        }
        void discover(int budget) {
            int checks=0,reads=0,examined=0;
            while(!scans.isEmpty()&&checks<8&&reads<budget&&examined++<budget+8) {
                var it=scans.entrySet().iterator();var scanEntry=it.next();var scan=scanEntry.getValue();
                if(!world.getChunkManager().isChunkLoaded(scan.chunk.getPos().x,scan.chunk.getPos().z)) {
                    // Load notification can precede full accessibility. Keep the hint until ready;
                    // CHUNK_UNLOAD explicitly removes it. Never force a chunk or lose discovery.
                    long key=scanEntry.getKey();it.remove();scans.put(key,scan);checks++;continue;
                }
                var sections=scan.chunk.getSectionArray();
                if(scan.section==sections.length) {it.remove();continue;}
                // Interleave chunks, starting near player height, so a nearby source is not
                // queued behind every empty underground section of the whole render distance.
                int offset=(scan.section+1)/2*(scan.section%2==0?-1:1);
                int sectionIndex=Math.floorMod(scan.firstSection+offset,sections.length);
                var section=sections[sectionIndex];
                if(scan.cell==0) {
                    checks++;
                    if(!section.hasAny(block->block.isOf(SourceBlocks.MASS_BLOCK))) {
                        scan.section++;long key=scanEntry.getKey();it.remove();scans.put(key,scan);continue;
                    }
                }
                int x=scan.cell&15,z=(scan.cell>>4)&15,y=scan.cell>>8;
                if(section.getBlockState(x,y,z).isOf(SourceBlocks.MASS_BLOCK))
                    tracker.discover(new Cell(scan.chunk.getPos().getStartX()+x,world.getBottomY()+sectionIndex*16+y,scan.chunk.getPos().getStartZ()+z));
                reads++;
                if(++scan.cell==4096) {scan.cell=0;scan.section++;long key=scanEntry.getKey();it.remove();scans.put(key,scan);}
            }
        }
    }
    /** Preserve the existing exhibit's calibration and passive actors. */
    public static boolean legacy(ServerWorld world) {return world.getRegistryKey().getValue().toString().equals("interstellar:demo");}
    private static WorldSources sources(ServerWorld world) {return worlds.computeIfAbsent(world,WorldSources::new);}
    public static Collection<ClusterTracker.Entry> entries(ServerWorld world) {
        var data=worlds.get(world);return data==null?List.of():data.tracker.entries();
    }
    public static long revision(ServerWorld world) {
        var data=worlds.get(world);return data==null?0:data.tracker.revision();
    }
    public static void changed(ServerWorld world,BlockPos pos) {
        sources(world).tracker.changed(new Cell(pos.getX(),pos.getY(),pos.getZ()),ticks);
    }
    public static void register() {
        PayloadTypeRegistry.playS2C().register(SourcePayload.ID,SourcePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SourceStatePayload.ID,SourceStatePayload.CODEC);
        ServerChunkEvents.CHUNK_LOAD.register((world,chunk)-> {
            var data=sources(world);var pos=chunk.getPos();data.tracker.chunkChanged(pos.x,pos.z,ticks);
            int y=world.getPlayers().isEmpty()?world.getSeaLevel():world.getPlayers().getFirst().getBlockY();
            int section=Math.clamp((y-world.getBottomY())>>4,0,chunk.getSectionArray().length-1);
            data.scans.put(ClusterTracker.chunk(pos.x,pos.z),new ChunkScan(chunk,section));
        });
        ServerChunkEvents.CHUNK_UNLOAD.register((world,chunk)-> {
            var data=sources(world);var pos=chunk.getPos();
            data.scans.remove(ClusterTracker.chunk(pos.x,pos.z));data.tracker.chunkChanged(pos.x,pos.z,ticks);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{worlds.clear();selections.clear();ticks=0;worldCursor=0;discoveryCursor=0;});
        ServerTickEvents.END_SERVER_TICK.register(SourceInspector::tick);
        CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->dispatcher.register(literal("interstellar")
                .then(literal("inspect").requires(source->source.hasPermissionLevel(2))
                        .then(argument("pos",BlockPosArgumentType.blockPos()).executes(context->{
                            request(context.getSource().getWorld(),context.getSource().getPlayerOrThrow(),BlockPosArgumentType.getBlockPos(context,"pos"));return 1;
                        })))
                .then(literal("source")
                        .then(literal("status").executes(context->{
                            var data=sources(context.getSource().getWorld());
                            String status="Sources="+data.tracker.entries().size()+", ready="+data.tracker.entries().stream().filter(ClusterTracker.Entry::ready).count()
                                    +", pending="+data.tracker.pending()+", discovery chunks="+data.scans.size()+", revision="+data.tracker.revision();
                            context.getSource().sendFeedback(()->Text.literal(status),false);
                            for(var entry:data.tracker.entries())context.getSource().sendFeedback(()->Text.literal("Source "+entry.id+": "+entry.result+", dirty="+entry.dirty()),false);
                            return 1;
                        }))
                        .then(literal("auto").executes(context->{selections.remove(context.getSource().getPlayerOrThrow());return 1;})))));
    }
    public static void request(ServerWorld world,ServerPlayerEntity player,BlockPos pos) {
        if(player.squaredDistanceTo(Vec3d.ofCenter(pos))>128*128) {
            player.sendMessage(Text.literal("Interstellar: inspection target must be within 128 blocks."));return;
        }
        var cell=new Cell(pos.getX(),pos.getY(),pos.getZ());
        if(state(world,cell)!=CellState.MASS) {
            player.sendMessage(Text.literal("Interstellar: target must be a mass block in a loaded chunk."));return;
        }
        var selection=new Selection(world);selection.requested=cell;selection.pinned=true;selection.announce=true;
        selections.put(player,selection);sources(world).tracker.discover(cell);
        sendState(player,selection,SourceState.REFRESHING);
    }
    private static CellState state(ServerWorld world,Cell cell) {
        var pos=new BlockPos(cell.x(),cell.y(),cell.z());
        if(!world.isInBuildLimit(pos))return CellState.EMPTY;
        if(!world.getChunkManager().isChunkLoaded(cell.x()>>4,cell.z()>>4))return CellState.UNKNOWN;
        return world.getBlockState(pos).isOf(SourceBlocks.MASS_BLOCK)?CellState.MASS:CellState.EMPTY;
    }
    private static void sendState(ServerPlayerEntity player,Selection selection,SourceState state) {
        if(selection.state==state)return;
        selection.state=state;if(state!=SourceState.REFRESHING)selection.payload=null;
        if(ServerPlayNetworking.canSend(player,SourceStatePayload.ID))
            ServerPlayNetworking.send(player,new SourceStatePayload(selection.world.getRegistryKey().getValue(),state));
    }
    private static double distanceSquared(ServerPlayerEntity player,ClusterTracker.Entry entry) {
        var r=entry.result;return player.squaredDistanceTo(r.x(),r.y(),r.z());
    }
    private static double score(ServerPlayerEntity player,ClusterTracker.Entry entry) {
        var r=entry.result;return r.schwarzschildRadius()/(distanceSquared(player,entry)+r.enclosingRadius()*r.enclosingRadius());
    }
    private static void track(ServerPlayerEntity player) {
        var world=player.getServerWorld();var data=sources(world);var selection=selections.get(player);
        if(selection==null||selection.world!=world) {selection=new Selection(world);selections.put(player,selection);}
        if(selection.requested!=null) {
            var found=data.tracker.at(selection.requested);
            if(found!=null) {selection.id=found.id;selection.requested=null;}
        }
        var entry=data.tracker.byId(selection.id);
        if(entry!=null&&distanceSquared(player,entry)>288*288)entry=null;
        if(selection.requested==null&&(!selection.pinned||entry==null)) {
            double best=entry==null?0:score(player,entry)*1.6;
            for(var candidate:data.tracker.entries()) {
                if(!candidate.ready()||distanceSquared(player,candidate)>256*256)continue;
                double score=score(player,candidate);
                if(score>best) {entry=candidate;best=score;}
            }
        }
        if(entry==null) {sendState(player,selection,selection.requested!=null?SourceState.REFRESHING:SourceState.NONE);return;}
        selection.id=entry.id;
        if(!entry.ready()) {
            sendState(player,selection,entry.dirty()&&ticks-entry.dirtySince()<20?SourceState.REFRESHING:
                    entry.result.status()==Status.LIMIT?SourceState.LIMIT:SourceState.UNLOADED);return;
        }
        var r=entry.result;
        var payload=new SourcePayload(world.getRegistryKey().getValue(),r.count(),r.x(),r.y(),r.z(),r.enclosingRadius(),r.schwarzschildRadius());
        if(!payload.equals(selection.payload)||selection.state!=SourceState.READY) {
            if(ServerPlayNetworking.canSend(player,SourcePayload.ID))ServerPlayNetworking.send(player,payload);
            selection.payload=payload;selection.state=SourceState.READY;
            String description=String.format(Locale.ROOT,"Interstellar: N=%d | centre=(%.2f, %.2f, %.2f) | R=%.3f | r_s=%.3f | C=%.3f | %s%s",
                    r.count(),r.x(),r.y(),r.z(),r.enclosingRadius(),r.schwarzschildRadius(),r.compactness(),
                    r.blackHoleProxy()?"black-hole proxy":"extended source",legacy(world)?" | legacy demo calibration":"");
            if(selection.announce) {player.sendMessage(Text.literal(description));selection.announce=false;}
            Interstellar.LOGGER.info(description);
        }
    }
    private static void tick(MinecraftServer server) {
        long started=io.github.rohrl.interstellar.gravity.GravityControl.begin();
        try {update(server);}finally {io.github.rohrl.interstellar.gravity.GravityControl.end(0,started);}
    }
    private static void update(MinecraftServer server) {
        ticks++;
        selections.keySet().removeIf(player->server.getPlayerManager().getPlayer(player.getUuid())!=player);
        var loaded=new ArrayList<>(worlds.values());
        if(!loaded.isEmpty())for(int slice=0;slice<4;slice++) {
            // Fixed aggregate budgets across worlds, including palette checks and cell reads.
            var data=loaded.get(Math.floorMod(worldCursor++,loaded.size()));
            data.tracker.advance(ticks,64);
        }
        var visible=loaded.stream().filter(data->!data.world.getPlayers().isEmpty()).toList();
        if(!visible.isEmpty())for(int slice=0;slice<4;slice++)visible.get(Math.floorMod(discoveryCursor++,visible.size())).discover(256);
        if(ticks%4==0)for(var player:server.getPlayerManager().getPlayerList())track(player);
    }
}
