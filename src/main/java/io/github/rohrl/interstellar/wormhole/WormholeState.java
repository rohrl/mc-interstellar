package io.github.rohrl.interstellar.wormhole;

import io.github.rohrl.interstellar.Interstellar;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.*;
import net.minecraft.registry.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentState;
import java.util.*;

/** The one pair belongs to the save, not the item stack or player. */
public final class WormholeState extends PersistentState {
    WormholePair.Layout layout=WormholePair.EMPTY;
    private record Sent(ServerWorld world,long revision) {}
    private static final Map<UUID,Sent> sent=new HashMap<>();
    public static WormholeState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(new Type<>(WormholeState::new,WormholeState::read,null),"interstellar_wormhole");
    }
    public static void register() {
        PayloadTypeRegistry.playS2C().register(WormholeLayoutPayload.ID,WormholeLayoutPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->resetViewer(handler.player));
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer,newPlayer,alive)->resetViewer(newPlayer));
        ServerLifecycleEvents.SERVER_STOPPED.register(server->sent.clear());
        ServerTickEvents.END_SERVER_TICK.register(server->{
            var state=get(server);var players=server.getPlayerManager().getPlayerList();
            // Migrate existing demo saves, without creating a second pair beside an item pair.
            if(state.layout.revision()==0 && players.stream().anyMatch(p->p.getWorld().getRegistryKey().equals(WormholePair.WORLD)))demo(server);
            sent.keySet().removeIf(id->server.getPlayerManager().getPlayer(id)==null);
            for(var player:players) {
                var now=new Sent(player.getServerWorld(),state.layout.revision());
                if(!now.equals(sent.get(player.getUuid()))) {
                    ServerPlayNetworking.send(player,new WormholeLayoutPayload(state.layout));sent.put(player.getUuid(),now);
                    WormholeTravel.reset(player);
                }
            }
        });
    }
    private static void resetViewer(ServerPlayerEntity player) {
        sent.remove(player.getUuid());WormholeChunks.forget(player.getUuid());
    }
    public static void demo(MinecraftServer server) {
        var state=get(server);if(state.layout.demo())return;
        state.set(new WormholePair.Layout(WormholePair.WORLD,List.of(WormholePair.A,WormholePair.B),state.layout.revision()+1,true));
    }
    public static void clear(ServerPlayerEntity player) {
        var state=get(player.getServer());state.set(new WormholePair.Layout(player.getWorld().getRegistryKey(),List.of(),state.layout.revision()+1,false));
        player.sendMessage(Text.literal("Wormhole closed. Your next throw places the first mouth."),false);
    }
    public static boolean place(ServerPlayerEntity player,Vec3d centre) {
        var world=player.getServerWorld();var state=get(player.getServer());var old=state.layout;
        // A successful throw in a different dimension starts a new local pair.
        // Validate before replacing anything, so a miss never closes the old one.
        boolean movedDimension=!old.mouths().isEmpty()&&!old.dimension().equals(world.getRegistryKey());
        var mouths=movedDimension?List.<Vec3d>of():old.mouths();double r=WormholePair.METRIC.mouthRadius();
        String error=null;
        if(centre.y-r<world.getBottomY() || centre.y+r>=world.getTopY()
            || !world.getWorldBorder().contains(centre.x-r,centre.z-r) || !world.getWorldBorder().contains(centre.x+r,centre.z+r))error="The mouth would cross the world boundary.";
        else if(world.getPlayers().stream().anyMatch(p->p.getEyePos().distanceTo(centre)<r+4))error="Too close to a player. Aim farther away.";
        else if(!mouths.isEmpty() && mouths.getLast().distanceTo(centre)<r*2+4)error="Too close to the remaining mouth. Leave at least 20 blocks between centres.";
        if(error==null)error=clearance(world,centre,r);
        if(error!=null){player.sendMessage(Text.literal("Wormhole: "+error+(old.mouths().isEmpty()?"":" Existing mouths kept.")),true);
            Interstellar.LOGGER.info("Wormhole placement rejected: {}; centre={}",error,centre);return false;}
        var next=mouths.isEmpty()?List.of(centre):List.of(mouths.getLast(),centre);
        state.set(new WormholePair.Layout(world.getRegistryKey(),next,old.revision()+1,false));
        player.sendMessage(Text.literal(next.size()==1?(movedDimension?"Previous pair closed. ":"")+"First wormhole end placed (closed). Throw another Rift Pearl elsewhere to connect it.":
            mouths.size()==2?"Oldest mouth relocated. Preparing the new destination...":"Wormhole connected. Preparing both destinations..."),false);
        world.playSound(null,centre.x,centre.y,centre.z,net.minecraft.sound.SoundEvents.BLOCK_BEACON_ACTIVATE,net.minecraft.sound.SoundCategory.BLOCKS,1,.8f);
        return true;
    }
    private static String clearance(ServerWorld world,Vec3d centre,double radius) {
        var lo=net.minecraft.util.math.BlockPos.ofFloored(centre.add(-radius,-radius,-radius));
        var hi=net.minecraft.util.math.BlockPos.ofFloored(centre.add(radius,radius,radius));
        for(int x=lo.getX()>>4;x<=hi.getX()>>4;x++)for(int z=lo.getZ()>>4;z<=hi.getZ()>>4;z++)
            if(!world.getChunkManager().isChunkLoaded(x,z))return "The landing area is still loading. Try again shortly.";
        // At most 17^3 cells for the fixed radius; no chunk generation or terrain edits.
        for(var pos:net.minecraft.util.math.BlockPos.iterate(lo,hi))if(Vec3d.ofCenter(pos).squaredDistanceTo(centre)<radius*radius
                && !world.getBlockState(pos).getCollisionShape(world,pos).isEmpty())return "The mouth needs more open space around the landing surface.";
        return null;
    }
    private void set(WormholePair.Layout value) {
        layout=value;markDirty();Interstellar.LOGGER.info("Wormhole layout changed: dimension={}, revision={}, demo={}, mouths={}",value.dimension().getValue(),value.revision(),value.demo(),value.mouths());
    }
    private static WormholeState read(NbtCompound nbt,RegistryWrapper.WrapperLookup registries) {
        var state=new WormholeState();var mouths=new ArrayList<Vec3d>();var entries=nbt.getList("mouths",NbtElement.COMPOUND_TYPE);
        for(int i=0;i<Math.min(entries.size(),2);i++){var p=entries.getCompound(i);mouths.add(new Vec3d(p.getDouble("x"),p.getDouble("y"),p.getDouble("z")));}
        state.layout=new WormholePair.Layout(RegistryKey.of(RegistryKeys.WORLD,Identifier.of(nbt.getString("dimension"))),mouths,nbt.getLong("revision"),nbt.getBoolean("demo"));return state;
    }
    @Override public NbtCompound writeNbt(NbtCompound nbt,RegistryWrapper.WrapperLookup registries) {
        nbt.putString("dimension",layout.dimension().getValue().toString());nbt.putLong("revision",layout.revision());nbt.putBoolean("demo",layout.demo());
        var mouths=new NbtList();for(var c:layout.mouths()){var p=new NbtCompound();p.putDouble("x",c.x);p.putDouble("y",c.y);p.putDouble("z",c.z);mouths.add(p);}nbt.put("mouths",mouths);return nbt;
    }
}
