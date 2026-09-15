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

/** Bounded on-demand server metadata work; no force loading, GR solver, or per-block ticking. */
public final class SourceInspector {
    private SourceInspector() { }
    private record Job(ServerWorld world, ServerPlayerEntity player, long epoch, ClusterProbe probe) { }
    private static final class Selection {
        final ServerWorld world;
        final SourceFootprint footprint;
        boolean dirty;
        Selection(ServerWorld world, SourceFootprint footprint) {this.world=world;this.footprint=footprint;}
    }
    private static final Map<ServerPlayerEntity, Selection> selections = new IdentityHashMap<>();
    private static final ArrayDeque<Job> jobs = new ArrayDeque<>();
    private static final Map<ServerWorld,Long> epochs = new IdentityHashMap<>();
    public static void changed(ServerWorld world, BlockPos pos) { changedChunk(world,pos.getX()>>4,pos.getZ()>>4); }
    private static void changedChunk(ServerWorld world, int x, int z) {
        // In-flight probes retain conservative world epochs; completed sources have local dependencies.
        epochs.put(world,epoch(world)+1);
        for (var selection:selections.values())
            if(selection.world==world && selection.footprint.contains(x,z)) selection.dirty=true;
    }
    private static long epoch(ServerWorld world) { return epochs.getOrDefault(world,0L); }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(SourcePayload.ID, SourcePayload.CODEC);
        ServerChunkEvents.CHUNK_LOAD.register((world,chunk)->changedChunk(world,chunk.getPos().x,chunk.getPos().z));
        ServerChunkEvents.CHUNK_UNLOAD.register((world,chunk)->changedChunk(world,chunk.getPos().x,chunk.getPos().z));
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{jobs.clear();epochs.clear();selections.clear();});
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
        jobs.removeIf(job->job.player==player);
        if (selections.remove(player) != null) sendClear(player, world);
        if (jobs.size() >= 8) { player.sendMessage(Text.literal("Interstellar: inspection queue busy; retry shortly.")); return; }
        if (state(world,new ClusterProbe.Cell(pos.getX(),pos.getY(),pos.getZ())) != ClusterProbe.CellState.MASS) {
            player.sendMessage(Text.literal("Interstellar: target must be a mass block in a loaded chunk.")); return;
        }
        jobs.add(new Job(world,player,epoch(world),new ClusterProbe(
                new ClusterProbe.Cell(pos.getX(),pos.getY(),pos.getZ()),cell->state(world,cell),4096)));
        player.sendMessage(Text.literal("Interstellar: inspecting connected mass blocks..."));
    }
    private static ClusterProbe.CellState state(ServerWorld world, ClusterProbe.Cell cell) {
        BlockPos pos=new BlockPos(cell.x(),cell.y(),cell.z());
        if (!world.isInBuildLimit(pos)) return ClusterProbe.CellState.EMPTY;
        if (!world.getChunkManager().isChunkLoaded(cell.x()>>4,cell.z()>>4)) return ClusterProbe.CellState.UNKNOWN;
        return world.getBlockState(pos).isOf(SourceBlocks.MASS_BLOCK) ? ClusterProbe.CellState.MASS : ClusterProbe.CellState.EMPTY;
    }
    private static void sendClear(ServerPlayerEntity player, ServerWorld world) {
        if (ServerPlayNetworking.canSend(player, SourcePayload.ID)) {
            ServerPlayNetworking.send(player, new SourcePayload(world.getRegistryKey().getValue(),0,0,0,0,0,0));
        }
    }
    private static void tick(MinecraftServer server) {
        // Coalesce arbitrarily many block/chunk revisions into one invalidation per selected player.
        selections.entrySet().removeIf(entry -> {
            var player=entry.getKey(); var selection=entry.getValue();
            if (server.getPlayerManager().getPlayer(player.getUuid()) != player) return true;
            if (player.getServerWorld()!=selection.world || selection.dirty) {
                sendClear(player, player.getServerWorld());
                return true;
            }
            return false;
        });
        // Four 64-cell slices, rotating jobs fairly. Even very large clusters cannot monopolize a tick.
        for (int slice=0;slice<4 && !jobs.isEmpty();slice++) {
            Job job=jobs.remove();
            if (server.getPlayerManager().getPlayer(job.player.getUuid()) != job.player || job.player.getServerWorld()!=job.world) continue;
            if (job.epoch!=epoch(job.world)) {
                job.player.sendMessage(Text.literal("Interstellar: mass blocks or loaded chunks changed; inspect again.")); continue;
            }
            job.probe.advance(64);
            if (!job.probe.finished()) {jobs.add(job);continue;}
            var result=job.probe.result();
            String message;
            if (result.status()!=ClusterProbe.Status.COMPLETE) {
                message="Interstellar: incomplete cluster ("+result.status()+"), "+result.count()+" known mass blocks. No compactness classification.";
            } else {
                message=String.format(Locale.ROOT,
                        "Interstellar: N=%d | centre=(%.2f, %.2f, %.2f) | enclosing R=%.3f | r_s=%.3f | C=%.3f | %s (spherical proxy; F9 frozen terrain preview)",
                        result.count(),result.x(),result.y(),result.z(),result.enclosingRadius(),result.schwarzschildRadius(),result.compactness(),
                        result.blackHoleProxy()?"black-hole proxy":"extended source");
            }
            if (result.status()==ClusterProbe.Status.COMPLETE && ServerPlayNetworking.canSend(job.player, SourcePayload.ID)) {
                ServerPlayNetworking.send(job.player, new SourcePayload(job.world.getRegistryKey().getValue(),
                        result.count(),result.x(),result.y(),result.z(),result.enclosingRadius(),result.schwarzschildRadius()));
                selections.put(job.player, new Selection(job.world,SourceFootprint.enclosing(result.x(),result.z(),result.enclosingRadius())));
            }
            job.player.sendMessage(Text.literal(message));
            Interstellar.LOGGER.info(message);
        }
    }
}
