package io.github.rohrl.interstellar.demo;

import io.github.rohrl.interstellar.Interstellar;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.Blocks;
import net.minecraft.block.DispenserBlock;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/** Demo-only calibrated dispensers. Native arrow gravity, drag and collisions stay active. */
final class DemoArrows {
    private static final String TAG="interstellar_demo_arrow";
    private static final Map<UUID,Flight> flights=new LinkedHashMap<>();
    private static final Vec3d CENTRE=new Vec3d(2,82,2);
    private static long lastTick=Long.MIN_VALUE;
    static boolean enabled=true;
    private static final class Flight {
        final ArrowEntity arrow;
        final String name;
        final boolean verify;
        final long launched;
        Vec3d last;
        double angle,minRadius=Double.POSITIVE_INFINITY,minX=Double.POSITIVE_INFINITY;
        Flight(ArrowEntity arrow,String name,boolean verify,long launched) {
            this.arrow=arrow;this.name=name;this.verify=verify;this.launched=launched;last=arrow.getPos().subtract(CENTRE);
        }
        void sample() {
            var p=arrow.getPos().subtract(CENTRE);
            angle+=Math.atan2(last.x*p.z-last.z*p.x,last.x*p.x+last.z*p.z);
            minRadius=Math.min(minRadius,p.length());minX=Math.min(minX,p.x);last=p;
        }
        void report(long now,String reason) {
            if(verify)Interstellar.LOGGER.info("Demo arrow {}: ticks={}, windingXZ={}deg, minR={}, minX={}, end={}, reason={}",
                    name,now-launched,Math.toDegrees(angle),minRadius,minX,arrow.getPos(),reason);
        }
    }
    private DemoArrows() { }
    static void register() {
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{flights.clear();lastTick=Long.MIN_VALUE;enabled=true;});
        // A previous visit's arrows must not accumulate in saved chunks. New launches are indexed first.
        ServerEntityEvents.ENTITY_LOAD.register((entity,world)-> {
            if(exhibit(world)&&entity.getCommandTags().contains(TAG)&&!flights.containsKey(entity.getUuid()))entity.discard();
        });
    }
    private static boolean exhibit(ServerWorld world) {return world.getRegistryKey().getValue().toString().equals("interstellar:arrows");}
    private static BlockPos pos(DemoArrowCourse.Shot shot) {return new BlockPos(shot.x(),shot.y(),shot.z());}
    private static Direction facing(DemoArrowCourse.Shot shot) {return shot.dx()>0?Direction.EAST:shot.dx()<0?Direction.WEST:Direction.SOUTH;}
    static boolean install(ServerWorld world) {
        for(var shot:DemoArrowCourse.SHOTS)if(!world.getChunkManager().isChunkLoaded(shot.x()>>4,shot.z()>>4))return false;
        var markers=List.of(Blocks.ORANGE_CONCRETE,Blocks.CYAN_CONCRETE,Blocks.MAGENTA_CONCRETE,Blocks.RED_CONCRETE);
        for(int i=0;i<DemoArrowCourse.SHOTS.size();i++) {
            var shot=DemoArrowCourse.SHOTS.get(i);var p=pos(shot);
            var dispenser=Blocks.DISPENSER.getDefaultState().with(DispenserBlock.FACING,facing(shot));
            var marker=markers.get(i).getDefaultState();var current=world.getBlockState(p);var below=world.getBlockState(p.down());
            if((!current.isAir()&&!current.equals(dispenser))||(!below.isAir()&&!below.equals(marker))) {
                Interstellar.LOGGER.warn("Demo arrow station {} skipped: occupied cells preserved at {}",shot.name(),p);continue;
            }
            world.setBlockState(p.down(),marker,3);world.setBlockState(p,dispenser,3);
        }
        return true;
    }
    static void tick(ServerWorld world) {
        long now=world.getTime();if(now==lastTick)return;lastTick=now;
        for(var iterator=flights.values().iterator();iterator.hasNext();) {
            var flight=iterator.next();var arrow=flight.arrow;
            if(flight.verify)flight.sample();
            if(arrow.isRemoved()||now-flight.launched>=160) {
                flight.report(now,arrow.isRemoved()?"removed":"demo lifetime");if(!arrow.isRemoved())arrow.discard();iterator.remove();
            }
        }
        if(!enabled||now%20!=0||world.getPlayers().stream().noneMatch(p->p.squaredDistanceTo(CENTRE)<96*96))return;
        fire(world,DemoArrowCourse.SHOTS.get((int)Math.floorMod(now/20,4)),false);
    }
    static void once(ServerWorld world) {
        enabled=false;
        for(var flight:flights.values())flight.arrow.discard();flights.clear();
        for(var shot:DemoArrowCourse.SHOTS)fire(world,shot,true);
    }
    private static void fire(ServerWorld world,DemoArrowCourse.Shot shot,boolean verify) {
        var p=pos(shot);if(flights.size()>=12||!world.getChunkManager().isChunkLoaded(p.getX()>>4,p.getZ()>>4))return;
        var state=world.getBlockState(p);
        if(!state.isOf(Blocks.DISPENSER)||state.get(DispenserBlock.FACING)!=facing(shot))return;
        var arrow=new ArrowEntity(world,shot.muzzleX(),shot.muzzleY(),shot.muzzleZ(),Items.ARROW.getDefaultStack(),null);
        arrow.pickupType=PersistentProjectileEntity.PickupPermission.DISALLOWED;
        arrow.addCommandTag(TAG);arrow.addCommandTag("interstellar_demo_"+shot.name());
        arrow.setVelocity(shot.vx(),shot.vy(),shot.vz());
        flights.put(arrow.getUuid(),new Flight(arrow,shot.name(),verify,world.getTime()));
        if(!world.spawnEntity(arrow)) {flights.remove(arrow.getUuid());return;}
        world.playSound(null,p,SoundEvents.BLOCK_DISPENSER_LAUNCH,SoundCategory.BLOCKS,.4f,1f);
    }
}
