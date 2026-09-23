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
    private static final RegistryKey<World> GAMEPLAY=RegistryKey.of(RegistryKeys.WORLD,Identifier.of("interstellar","gameplay"));
    private static boolean exhibit(ServerWorld world) {return world.getRegistryKey().equals(WORLD)||world.getRegistryKey().equals(GAMEPLAY);}
    private static String builtKey(ServerWorld world) {return world.getRegistryKey().equals(WORLD)?"built":"gameplayBuilt";}
    private static Job job;
    private static final Map<UUID,Integer> awaitingSource=new HashMap<>();
    private DemoCommands() { }
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->dispatcher.register(literal("interstellar")
                .then(literal("demo").requires(s->s.hasPermissionLevel(2))
                        .executes(c->{message(c.getSource().getPlayerOrThrow(),"Use /interstellar demo enter or /interstellar demo leave. Enter builds a separate exhibit once; F10 enables lensing.");return 1;})
                        .then(literal("enter").executes(c->enter(c.getSource().getPlayerOrThrow())))
                        .then(literal("gameplay").executes(c->enter(c.getSource().getPlayerOrThrow(),GAMEPLAY)))
                        .then(literal("leave").executes(c->leave(c.getSource().getPlayerOrThrow())))
                        .then(literal("view")
                                .then(literal("wall").executes(c->view(c.getSource().getPlayerOrThrow(),2,80.38,-54,0,0)))
                                .then(literal("side").executes(c->view(c.getSource().getPlayerOrThrow(),58,80.38,2,90,0)))
                                .then(literal("close").executes(c->view(c.getSource().getPlayerOrThrow(),2,80.38,-26,0,0)))
                                .then(literal("terrain").executes(c->view(c.getSource().getPlayerOrThrow(),2,80.38,-54,0,35)))))));
        ServerTickEvents.END_SERVER_TICK.register(DemoCommands::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{job=null;awaitingSource.clear();});
    }
    private static int enter(ServerPlayerEntity player) {
        return enter(player,WORLD);
    }
    private static int enter(ServerPlayerEntity player,RegistryKey<World> target) {
        var server=player.getServer();var world=server.getWorld(target);
        if(world==null) {message(player,"Demo dimension unavailable. Restart Minecraft after installing this build.");return 0;}
        if(state(server).data.getBoolean(builtKey(world))) {arrive(player,world);return 1;}
        if(job!=null&&job.world!=world) {message(player,"Another exhibit is being prepared; retry when it is ready.");return 0;}
        if(job==null)job=new Job(world);
        job.waiting.add(player.getUuid());
        message(player,"Preparing the separate demo exhibit. You can keep playing; you will enter when ready. /interstellar demo leave cancels.");
        return 1;
    }
    private static void tick(MinecraftServer server) {
        for(var iterator=awaitingSource.entrySet().iterator();iterator.hasNext();) {
            var pending=iterator.next();var player=server.getPlayerManager().getPlayer(pending.getKey());
            if(player==null || !player.getWorld().getRegistryKey().equals(WORLD)) {iterator.remove();continue;}
            var world=player.getServerWorld();
            if(world.getChunkManager().isChunkLoaded(0,0)) {
                if(world.getBlockState(DemoScene.ANCHOR).isOf(io.github.rohrl.interstellar.source.SourceBlocks.MASS_BLOCK))
                    SourceInspector.request(world,player,DemoScene.ANCHOR);
                else message(player,"The exhibit source was edited. Inspect one of its remaining mass blocks to select it.");
                iterator.remove();
            } else if(pending.getValue()<=0) {message(player,"Source chunk is still loading. Run /interstellar inspect 0 80 0 once it is visible.");iterator.remove();}
            else pending.setValue(pending.getValue()-1);
        }
        if(job==null)return;
        var work=job;
        try {
            // Bound cells and wall time; a cold chunk load can still overrun one slice.
            long deadline=System.nanoTime()+4_000_000;
            for(int n=0;n<256 && work.cursor<work.blocks.size() && System.nanoTime()<deadline;n++,work.cursor++) {
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
            var state=state(server);state.data.putBoolean(builtKey(work.world),true);state.markDirty();job=null;
            Interstellar.LOGGER.info("Demo exhibit ready: {} blocks, 3 sheep, separate dimension {}",work.blocks.size(),work.world.getRegistryKey().getValue());
            for(var id:work.waiting) {var p=server.getPlayerManager().getPlayer(id);if(p!=null)arrive(p,work.world);}
        } catch(RuntimeException failure) {job=null;Interstellar.LOGGER.error("Demo setup failed",failure);for(var id:work.waiting){var p=server.getPlayerManager().getPlayer(id);if(p!=null)message(p,"Demo setup failed; see game log. Your original world was not edited.");}}
    }
    private static void arrive(ServerPlayerEntity player,ServerWorld world) {
        var state=state(player.getServer());String key=player.getUuidAsString();
        if(!exhibit(player.getServerWorld())) {
            var saved=new NbtCompound();saved.putString("world",player.getWorld().getRegistryKey().getValue().toString());
            saved.putDouble("x",player.getX());saved.putDouble("y",player.getY());saved.putDouble("z",player.getZ());
            saved.putFloat("yaw",player.getYaw());saved.putFloat("pitch",player.getPitch());
            saved.putString("mode",player.interactionManager.getGameMode().getName());saved.putBoolean("flying",player.getAbilities().flying);
            state.data.put(key,saved);state.markDirty();
        }
        player.changeGameMode(GameMode.CREATIVE);player.teleport(world,2,80.38,-54,0,0);
        player.getAbilities().flying=true;player.sendAbilitiesUpdate();
        // Player tickets load the source after the dimension transition; never probe it prematurely.
        if(world.getRegistryKey().equals(WORLD))awaitingSource.put(player.getUuid(),200);
        message(player,"Demo ready: F10 lensing, WASD/mouse to explore, Space/Shift to fly. /interstellar demo view wall|side|close|terrain selects a viewpoint; /interstellar demo leave returns you. Initial capture takes a moment; source edits update automatically.");
    }
    private static int view(ServerPlayerEntity player,double x,double y,double z,float yaw,float pitch) {
        if(!exhibit(player.getServerWorld())) {message(player,"Enter an exhibit first: /interstellar demo enter or gameplay");return 0;}
        player.teleport(player.getServerWorld(),x,y,z,yaw,pitch);
        player.getAbilities().flying=player.getAbilities().allowFlying;player.sendAbilitiesUpdate();return 1;
    }
    private static int leave(ServerPlayerEntity player) {
        awaitingSource.remove(player.getUuid());
        if(job!=null)job.waiting.remove(player.getUuid());
        if(!exhibit(player.getServerWorld())) {message(player,"Demo entry cancelled; already outside the exhibit.");return 1;}
        var state=state(player.getServer());String key=player.getUuidAsString();
        if(!state.data.contains(key)) {message(player,"No saved return location. Use /execute in minecraft:overworld run tp @s <x> <y> <z>.");return 0;}
        var saved=state.data.getCompound(key);
        var world=player.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD,Identifier.of(saved.getString("world"))));
        if(world==null) {message(player,"Return dimension is unavailable; saved return location retained.");return 0;}
        player.teleport(world,saved.getDouble("x"),saved.getDouble("y"),saved.getDouble("z"),saved.getFloat("yaw"),saved.getFloat("pitch"));
        player.changeGameMode(GameMode.byName(saved.getString("mode"),GameMode.SURVIVAL));
        player.getAbilities().flying=saved.getBoolean("flying")&&player.getAbilities().allowFlying;player.sendAbilitiesUpdate();
        state.data.remove(key);state.markDirty();message(player,"Returned to your saved location and game mode. Nearby mass blocks are discovered automatically; F10 enables lensing.");return 1;
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
