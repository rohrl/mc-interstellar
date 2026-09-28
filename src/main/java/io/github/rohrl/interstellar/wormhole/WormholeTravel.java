package io.github.rohrl.interstellar.wormhole;

import io.github.rohrl.interstellar.Interstellar;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/** Server-authoritative chart change at the throat. Creative controls prescribe the path. */
public final class WormholeTravel {
    private static final Map<UUID,Motion> motions=new HashMap<>();
    private record Motion(Vec3d eye,double roll) {}
    private WormholeTravel() {}
    public static void register() {
        PayloadTypeRegistry.playS2C().register(WormholeTransitPayload.ID,WormholeTransitPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(WormholeResetPayload.ID,WormholeResetPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(WormholeResetPayload.ID,(payload,context)-> {
            var player=context.player();if(!WormholePair.active(player.getServerWorld()))return;
            reset(player);
            player.sendMessage(net.minecraft.text.Text.translatable("message.interstellar.camera_upright"),true);
        });
        ServerTickEvents.END_SERVER_TICK.register(WormholeTravel::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server->motions.clear());
    }
    public static void reset(ServerPlayerEntity player) {
        motions.remove(player.getUuid());
        // Explicit comfort reset (also used by named viewpoints), not a crossing.
        var eye=player.getEyePos();ServerPlayNetworking.send(player,new WormholeTransitPayload(0,eye,eye,0));
    }
    public static Vec3d up(float yaw,float pitch,double roll) {
        var forward=Vec3d.fromPolar(pitch,yaw);var right=Vec3d.fromPolar(0,yaw+90);
        return right.crossProduct(forward).multiply(Math.cos(roll)).add(right.multiply(Math.sin(roll)));
    }
    private static void tick(MinecraftServer server) {
        var world=server.getWorld(WormholePair.WORLD);
        if(world==null || world.getPlayers().isEmpty()) {motions.clear();return;}
        var players=world.getPlayers();motions.keySet().removeIf(id->players.stream().noneMatch(p->p.getUuid().equals(id)));
        for(var player:players) {
            var eye=player.getEyePos();var previous=motions.get(player.getUuid());
            double roll=previous==null?0:previous.roll;int from=WormholePair.nearest(eye);
            double r=eye.distanceTo(WormholePair.centre(from)),mouth=WormholePair.METRIC.mouthRadius();
            // A small numerical deadband prevents the exactly-on-throat case bouncing.
            // Remote readiness is acknowledged only after applying chunk/light packets.
            if(WormholeChunks.ready(player) && !player.hasVehicle() && r<mouth-1e-5 && r>1e-4) {
                var target=WormholePair.transfer(from,eye);
                var forward=WormholePair.transferVector(from,eye,Vec3d.fromPolar(player.getPitch(),player.getYaw())).normalize();
                var up=WormholePair.transferVector(from,eye,up(player.getYaw(),player.getPitch(),roll)).normalize();
                float yaw=(float)Math.toDegrees(Math.atan2(-forward.x,forward.z));
                float pitch=(float)-Math.toDegrees(Math.atan2(forward.y,Math.hypot(forward.x,forward.z)));
                var right=Vec3d.fromPolar(0,yaw+90);var vertical=right.crossProduct(forward).normalize();
                double nextRoll=Math.atan2(up.dotProduct(right),up.dotProduct(vertical));
                // Position packets do not carry the client's creative-flight velocity.
                // The receiver transports its own actual velocity before vanilla resets it.
                ServerPlayNetworking.send(player,new WormholeTransitPayload(from,eye,target,nextRoll));
                var velocity=previous==null || eye.squaredDistanceTo(previous.eye)>16?player.getVelocity():eye.subtract(previous.eye);
                var mappedVelocity=WormholePair.transferVector(from,eye,velocity);
                player.teleport(world,target.x,target.y-player.getStandingEyeHeight(),target.z,yaw,pitch);
                player.setVelocity(mappedVelocity);player.fallDistance=0;
                motions.put(player.getUuid(),new Motion(target,nextRoll));
                Interstellar.LOGGER.info("Wormhole crossing: {} -> {}, eye={} -> {}, yaw={}, pitch={}, roll={}",from,1-from,eye,target,yaw,pitch,Math.toDegrees(nextRoll));
            } else motions.put(player.getUuid(),new Motion(eye,roll));
        }
    }
}
