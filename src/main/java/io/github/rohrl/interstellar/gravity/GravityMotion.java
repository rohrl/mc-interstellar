package io.github.rohrl.interstellar.gravity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import java.util.function.Consumer;

public final class GravityMotion {
    private GravityMotion() { }
    public static Vec3d centre(LivingEntity entity) {return entity.getPos().add(0,entity.getHeight()*.5,0);}
    public static void mob(MobEntity entity,Vec3d movement,Consumer<Vec3d> move) {
        if(!(entity.getWorld() instanceof ServerWorld world)||entity.hasVehicle()||entity.hasPassengers()) {move.accept(movement);return;}
        Vec3d start=centre(entity);var field=GravitySources.find(world,start,start.add(movement));
        if(field==null) {move.accept(movement);return;}
        if(captured(field,start,start)||touchingHorizon(field,entity)) {entity.discard();return;}
        var a=field.acceleration(start.x,start.y,start.z);
        entity.setVelocity(entity.getVelocity().add(a.x(),a.y(),a.z()));
        move.accept(movement.add(a.x(),a.y(),a.z()));
        if(captured(field,start,centre(entity))||touchingHorizon(field,entity))entity.discard();
        else if(a.length()>0)entity.velocityModified=true;
    }
    public static boolean captured(GravityField field,Vec3d start,Vec3d end) {
        return GravitySources.config.capture&&field.captureFraction(start.x,start.y,start.z,end.x,end.y,end.z)>=0;
    }
    private static boolean touchingHorizon(GravityField field,LivingEntity entity) {
        var box=entity.getBoundingBox();return GravitySources.config.capture&&field.intersectsBox(box.minX,box.minY,box.minZ,box.maxX,box.maxY,box.maxZ);
    }
}
