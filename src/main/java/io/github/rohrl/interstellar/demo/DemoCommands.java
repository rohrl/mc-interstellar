package io.github.rohrl.interstellar.demo;

import io.github.rohrl.interstellar.Interstellar;
import io.github.rohrl.interstellar.source.SourceInspector;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import java.util.*;
import static net.minecraft.server.command.CommandManager.literal;

/** Opt-in exhibit with bounded construction and persistent return locations. Never edits another dimension. */
public final class DemoCommands {
    private static final RegistryKey<World> WORLD=RegistryKey.of(RegistryKeys.WORLD,Identifier.of("interstellar","demo"));
    private static Job job;
    private DemoCommands() { }
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->dispatcher.register(literal("interstellar")
                .then(literal("demo").requires(s->s.hasPermissionLevel(2))
                        .executes(c->{message(c.getSource().getPlayerOrThrow(),"Use /interstellar demo enter or /interstellar demo leave. Enter builds a separate exhibit once; F10 enables lensing.");return 1;})
                        .then(literal("enter").executes(c->enter(c.getSource().getPlayerOrThrow())))
                        .then(literal("leave").executes(c->leave(c.getSource().getPlayerOrThrow()))))));
        ServerTickEvents.END_SERVER_TICK.register(DemoCommands::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server->job=null);
    }
    private static int enter(ServerPlayerEntity player) {
        var server=player.getServer();var world=server.getWorld(WORLD);
        if(world==null) {message(player,"Demo dimension unavailable. Restart Minecraft after installing this build.");return 0;}
        if(state(server).data.getBoolean("built")) {arrive(player,world);return 1;}
        if(job==null)job=new Job(world);
        job.waiting.add(player.getUuid());
        message(player,"Preparing the separate demo exhibit. You can keep playing; you will enter when ready. /interstellar demo leave cancels.");
        return 1;
    }
    private static void tick(MinecraftServer server) {
        if(job==null)return;
        var work=job;
        try {
            // At most256 target cells per tick, including the non-destructive preflight.
            for(int n=0;n<256 && work.cursor<work.blocks.size();n++,work.cursor++) {
                var entry=work.blocks.get(work.cursor);var current=work.world.getBlockState(entry.getKey());
                if(!current.isAir() && !current.equals(entry.getValue())) {
                    for(var id:work.waiting) {var p=server.getPlayerManager().getPlayer(id);if(p!=null)message(p,"Demo build stopped: occupied cell "+entry.getKey()+". Existing blocks were preserved.");}
                    job=null;return;
                }
                if(work.build && !current.equals(entry.getValue()))work.world.setBlockState(entry.getKey(),entry.getValue(),3);
            }
            if(work.cursor<work.blocks.size())return;
            if(!work.build) {work.build=true;work.cursor=0;return;}
            for(int i=0;i<3;i++) {
                var sheep=EntityType.SHEEP.create(work.world);
                if(sheep!=null) {sheep.refreshPositionAndAngles(-8+i*7,65,-24,0,0);sheep.setPersistent();sheep.setCustomName(Text.literal("Demo sheep "+(i+1)));work.world.spawnEntity(sheep);}
            }
            var state=state(server);state.data.putBoolean("built",true);state.markDirty();job=null;
            Interstellar.LOGGER.info("Demo exhibit ready: {} blocks, 3 sheep, separate dimension {}",work.blocks.size(),WORLD.getValue());
            for(var id:work.waiting) {var p=server.getPlayerManager().getPlayer(id);if(p!=null)arrive(p,work.world);}
        } catch(RuntimeException failure) {job=null;Interstellar.LOGGER.error("Demo setup failed",failure);for(var id:work.waiting){var p=server.getPlayerManager().getPlayer(id);if(p!=null)message(p,"Demo setup failed; see game log. Your original world was not edited.");}}
    }
    private static void arrive(ServerPlayerEntity player,ServerWorld world) {
        var state=state(player.getServer());String key=player.getUuidAsString();
        if(!player.getWorld().getRegistryKey().equals(WORLD)) {
            var saved=new NbtCompound();saved.putString("world",player.getWorld().getRegistryKey().getValue().toString());
            saved.putDouble("x",player.getX());saved.putDouble("y",player.getY());saved.putDouble("z",player.getZ());
            saved.putFloat("yaw",player.getYaw());saved.putFloat("pitch",player.getPitch());
            saved.putString("mode",player.interactionManager.getGameMode().getName());saved.putBoolean("flying",player.getAbilities().flying);
            state.data.put(key,saved);state.markDirty();
        }
        player.changeGameMode(GameMode.CREATIVE);player.teleport(world,2,80.38,-54,0,0);
        player.getAbilities().flying=true;player.sendAbilitiesUpdate();
        SourceInspector.request(world,player,DemoScene.ANCHOR);
        message(player,"Demo ready: F10 lensing, WASD/mouse to explore, Space/Shift to fly. Source updates automatically. /interstellar demo leave returns you. F10 reloads terrain when re-enabled.");
    }
    private static int leave(ServerPlayerEntity player) {
        if(job!=null)job.waiting.remove(player.getUuid());
        if(!player.getWorld().getRegistryKey().equals(WORLD)) {message(player,"Demo entry cancelled; already outside the exhibit.");return 1;}
        var state=state(player.getServer());String key=player.getUuidAsString();
        if(!state.data.contains(key)) {message(player,"No saved return location. Use /execute in minecraft:overworld run tp @s <x> <y> <z>.");return 0;}
        var saved=state.data.getCompound(key);
        var world=player.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD,Identifier.of(saved.getString("world"))));
        if(world==null) {message(player,"Return dimension is unavailable; saved return location retained.");return 0;}
        player.teleport(world,saved.getDouble("x"),saved.getDouble("y"),saved.getDouble("z"),saved.getFloat("yaw"),saved.getFloat("pitch"));
        player.changeGameMode(GameMode.byName(saved.getString("mode"),GameMode.SURVIVAL));
        player.getAbilities().flying=saved.getBoolean("flying")&&player.getAbilities().allowFlying;player.sendAbilitiesUpdate();
        state.data.remove(key);state.markDirty();message(player,"Returned to your saved location and game mode. Inspect your original source before enabling F10.");return 1;
    }
    private static void message(ServerPlayerEntity player,String text) {player.sendMessage(Text.literal("Interstellar: "+text));}
    private static State state(MinecraftServer server) {return server.getOverworld().getPersistentStateManager().getOrCreate(new PersistentState.Type<>(State::new,(nbt,registries)->new State(nbt),null),"interstellar_demo");}
    private static final class State extends PersistentState {
        final NbtCompound data;
        State() {data=new NbtCompound();} State(NbtCompound data) {this.data=data;}
        @Override public NbtCompound writeNbt(NbtCompound nbt,RegistryWrapper.WrapperLookup registries) {nbt.copyFrom(data);return nbt;}
    }
    private static final class Job {
        final ServerWorld world;final List<Map.Entry<BlockPos,BlockState>> blocks=new ArrayList<>(DemoScene.blocks().entrySet());
        final Set<UUID> waiting=new HashSet<>();int cursor;boolean build;
        Job(ServerWorld world) {this.world=world;}
    }
}
