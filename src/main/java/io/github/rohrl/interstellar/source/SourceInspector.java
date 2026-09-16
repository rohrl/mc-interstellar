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
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import static net.minecraft.server.command.CommandManager.*;

/** Bounded event-driven metadata refresh; no force loading, GR solver, or per-block ticking. */
public final class SourceInspector {
    private SourceInspector() { }
    private record Job(ServerPlayerEntity player,Selection selection,long epoch,ClusterProbe probe) { }
    private static final class Selection {
        final ServerWorld world;
        final BlockPos anchor;
        final RefreshSchedule schedule=new RefreshSchedule();
        SourceFootprint footprint;
        SourceState state=SourceState.NONE;
        boolean announce=true;
        Selection(ServerWorld world,BlockPos anchor) {
            this.world=world;this.anchor=anchor.toImmutable();
            footprint=SourceFootprint.enclosing(anchor.getX()+.5,anchor.getZ()+.5,0);
        }
    }
    private static final Map<ServerPlayerEntity, Selection> selections = new IdentityHashMap<>();
    private static final ArrayDeque<Job> jobs = new ArrayDeque<>();
    private static final Map<ServerWorld,Long> epochs = new IdentityHashMap<>();
    private static long ticks;
    public static void changed(ServerWorld world, BlockPos pos) { changedChunk(world,pos.getX()>>4,pos.getZ()>>4); }
    private static void changedChunk(ServerWorld world, int x, int z) {
        // In-flight probes retain conservative world epochs; completed sources have local dependencies.
        epochs.put(world,epoch(world)+1);
        for (var selection:selections.values())
            if(selection.world==world && selection.footprint.contains(x,z)) selection.schedule.changed(ticks);
    }
    private static long epoch(ServerWorld world) { return epochs.getOrDefault(world,0L); }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(SourcePayload.ID, SourcePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SourceStatePayload.ID,SourceStatePayload.CODEC);
        ServerChunkEvents.CHUNK_LOAD.register((world,chunk)->changedChunk(world,chunk.getPos().x,chunk.getPos().z));
        ServerChunkEvents.CHUNK_UNLOAD.register((world,chunk)->changedChunk(world,chunk.getPos().x,chunk.getPos().z));
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{jobs.clear();epochs.clear();selections.clear();ticks=0;});
        ServerTickEvents.END_SERVER_TICK.register(SourceInspector::tick);
        CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->dispatcher.register(
                literal("interstellar").then(literal("inspect").requires(source->source.hasPermissionLevel(2))
                        .then(argument("pos", BlockPosArgumentType.blockPos()).executes(context->{
                            request(context.getSource().getWorld(),context.getSource().getPlayerOrThrow(),
                                    BlockPosArgumentType.getBlockPos(context,"pos"));
                            return 1;
                        })))));
    }
    public static void request(ServerWorld world, ServerPlayerEntity player, BlockPos pos) {
        if (player.squaredDistanceTo(Vec3d.ofCenter(pos)) > 128*128) {
            player.sendMessage(Text.literal("Interstellar: inspection target must be within 128 blocks.")); return;
        }
        if (state(world,new ClusterProbe.Cell(pos.getX(),pos.getY(),pos.getZ())) != ClusterProbe.CellState.MASS) {
            player.sendMessage(Text.literal("Interstellar: target must be a mass block in a loaded chunk.")); return;
        }
        jobs.removeIf(job->job.player==player);
        Selection selection=new Selection(world,pos);selections.put(player,selection);
        sendState(player,selection,SourceState.REFRESHING);
        player.sendMessage(Text.literal("Interstellar: inspecting connected mass blocks..."));
    }
    private static ClusterProbe.CellState state(ServerWorld world, ClusterProbe.Cell cell) {
        BlockPos pos=new BlockPos(cell.x(),cell.y(),cell.z());
        if (!world.isInBuildLimit(pos)) return ClusterProbe.CellState.EMPTY;
        if (!world.getChunkManager().isChunkLoaded(cell.x()>>4,cell.z()>>4)) return ClusterProbe.CellState.UNKNOWN;
        return world.getBlockState(pos).isOf(SourceBlocks.MASS_BLOCK) ? ClusterProbe.CellState.MASS : ClusterProbe.CellState.EMPTY;
    }
    private static void sendClear(ServerPlayerEntity player, ServerWorld world) {
        if(ServerPlayNetworking.canSend(player,SourceStatePayload.ID))
            ServerPlayNetworking.send(player,new SourceStatePayload(world.getRegistryKey().getValue(),SourceState.NONE));
        if (ServerPlayNetworking.canSend(player, SourcePayload.ID)) {
            ServerPlayNetworking.send(player, new SourcePayload(world.getRegistryKey().getValue(),0,0,0,0,0,0));
        }
    }
    private static void sendState(ServerPlayerEntity player,Selection selection,SourceState next) {
        if(selection.state==next)return;
        selection.state=next;
        if(ServerPlayNetworking.canSend(player,SourceStatePayload.ID))
            ServerPlayNetworking.send(player,new SourceStatePayload(selection.world.getRegistryKey().getValue(),next));
        else if(ServerPlayNetworking.canSend(player,SourcePayload.ID))
            ServerPlayNetworking.send(player,new SourcePayload(selection.world.getRegistryKey().getValue(),0,0,0,0,0,0));
        Interstellar.LOGGER.info("Source tracking: player={}, anchor={}, state={}",player.getName().getString(),selection.anchor,next);
    }
    private static void tick(MinecraftServer server) {
        ticks++;
        selections.entrySet().removeIf(entry -> {
            var player=entry.getKey(); var selection=entry.getValue();
            var currentPlayer=server.getPlayerManager().getPlayer(player.getUuid());
            if (currentPlayer!=player) {
                // Respawn can replace the server player without replacing the client world.
                if(currentPlayer!=null)sendClear(currentPlayer,currentPlayer.getServerWorld());
                return true;
            }
            if (player.getServerWorld()!=selection.world) {
                sendClear(player, player.getServerWorld());
                return true;
            }
            return false;
        });
        jobs.removeIf(job->selections.get(job.player)!=job.selection);
        // Clear stale metadata immediately, but debounce new probes and keep at most eight queued.
        for(var entry:selections.entrySet()) {
            var player=entry.getKey();var selection=entry.getValue();
            if(!selection.schedule.pending())continue;
            sendState(player,selection,SourceState.REFRESHING);
            if(jobs.size()>=8||jobs.stream().anyMatch(job->job.selection==selection)||!selection.schedule.claim(ticks))continue;
            BlockPos pos=selection.anchor;
            jobs.add(new Job(player,selection,epoch(selection.world),new ClusterProbe(
                    new ClusterProbe.Cell(pos.getX(),pos.getY(),pos.getZ()),cell->{
                        // A growing or partial component can depend on chunks outside its old bounds.
                        selection.footprint=selection.footprint.including(cell.x()>>4,cell.z()>>4);
                        return state(selection.world,cell);
                    },4096)));
        }
        // Four 64-cell slices, rotating jobs fairly. Even very large clusters cannot monopolize a tick.
        for (int slice=0;slice<4 && !jobs.isEmpty();slice++) {
            Job job=jobs.remove();
            Selection selection=job.selection;
            if(selections.get(job.player)!=selection)continue;
            if (job.epoch!=epoch(selection.world)) {
                selection.schedule.retry(ticks);continue;
            }
            job.probe.advance(64);
            if (!job.probe.finished()) {jobs.add(job);continue;}
            var result=job.probe.result();
            String message;
            if (result.status()!=ClusterProbe.Status.COMPLETE) {
                message="Interstellar: incomplete cluster ("+result.status()+"), "+result.count()+" known mass blocks. No compactness classification.";
                sendState(job.player,selection,switch(result.status()) {
                    case EMPTY -> SourceState.REMOVED;
                    case PARTIAL -> SourceState.UNLOADED;
                    case LIMIT -> SourceState.LIMIT;
                    default -> throw new IllegalStateException("Unexpected probe result");
                });
            } else {
                message=String.format(Locale.ROOT,
                        "Interstellar: N=%d | centre=(%.2f, %.2f, %.2f) | enclosing R=%.3f | r_s=%.3f | C=%.3f | %s (spherical proxy; F9 frozen terrain preview)",
                        result.count(),result.x(),result.y(),result.z(),result.enclosingRadius(),result.schwarzschildRadius(),result.compactness(),
                        result.blackHoleProxy()?"black-hole proxy":"extended source");
            }
            if (result.status()==ClusterProbe.Status.COMPLETE && ServerPlayNetworking.canSend(job.player, SourcePayload.ID)) {
                ServerPlayNetworking.send(job.player, new SourcePayload(selection.world.getRegistryKey().getValue(),
                        result.count(),result.x(),result.y(),result.z(),result.enclosingRadius(),result.schwarzschildRadius()));
                selection.state=SourceState.READY;
                selection.footprint=SourceFootprint.enclosing(result.x(),result.z(),result.enclosingRadius());
            }
            if(selection.announce) {job.player.sendMessage(Text.literal(message));selection.announce=false;}
            Interstellar.LOGGER.info(message);
        }
    }
}
