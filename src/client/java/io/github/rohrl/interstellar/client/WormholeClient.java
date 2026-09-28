package io.github.rohrl.interstellar.client;

import io.github.rohrl.interstellar.Interstellar;
import io.github.rohrl.interstellar.wormhole.WormholePair;
import io.github.rohrl.interstellar.wormhole.WormholeReadyPayload;
import io.github.rohrl.interstellar.wormhole.WormholeTransitPayload;
import io.github.rohrl.interstellar.wormhole.WormholeResetPayload;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import net.minecraft.util.math.Vec3d;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

public final class WormholeClient {
    private static ClientWorld readyWorld;
    private static ClientWorld cameraWorld;
    private static double roll;
    private static Transit transit;
    private static KeyBinding resetOrientation;
    private record Transit(WormholeTransitPayload payload,Vec3d velocity,Vec3d previousEye,long tick) {}
    private WormholeClient() {}
    public static void register() {
        resetOrientation=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.interstellar.reset_orientation",
            InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_R,"key.categories.interstellar"));
        ClientPlayNetworking.registerGlobalReceiver(WormholeTransitPayload.ID,(payload,context)-> {
            var client=context.client();if(!WormholePair.active(client.world)||client.player==null)return;
            cameraWorld=client.world;
            if(payload.eye().equals(payload.target())) {
                Interstellar.LOGGER.info("Wormhole camera upright: roll={} -> 0, eye={}, yaw={}, pitch={}",
                    Math.toDegrees(roll),client.player.getEyePos(),client.player.getYaw(),client.player.getPitch());
                roll=0;transit=null;return;
            }
            var p=client.player;
            transit=new Transit(payload,WormholePair.transferVector(payload.from(),payload.eye(),p.getVelocity()),
                new Vec3d(p.prevX,p.prevY+p.getStandingEyeHeight(),p.prevZ),client.world.getTime());
        });
        ClientPlayNetworking.registerGlobalReceiver(WormholeReadyPayload.ID,(payload,context)-> {
            var world=context.client().world;if(!WormholePair.active(world))return;
            // Chunk data is applied immediately, but native light packets are queued.
            // Insert the readiness barrier in that same ordered queue.
            world.enqueueChunkUpdate(()-> {
                if(context.client().world!=world)return;
                int count=((WormholeChunkCache)world.getChunkManager()).interstellar$remoteChunkCount();
                if(count!=WormholePair.CHUNKS.size()) {
                    Interstellar.LOGGER.error("Wormhole readiness rejected: {}/{} chunks",count,WormholePair.CHUNKS.size());return;
                }
                readyWorld=world;ClientPlayNetworking.send(payload);
                Interstellar.LOGGER.info("Wormhole native regions ready: {} chunks with light data applied",count);
                for(int end=0;end<2;end++) {
                    var pos=net.minecraft.util.math.BlockPos.ofFloored(WormholePair.centre(end)).add(20,-12,20);
                    Interstellar.LOGGER.info("Wormhole client region {}: sample={}, block={}, sky={}, blocklight={}",end,pos,
                        net.minecraft.registry.Registries.BLOCK.getId(world.getBlockState(pos).getBlock()),
                        world.getLightLevel(net.minecraft.world.LightType.SKY,pos.up()),world.getLightLevel(net.minecraft.world.LightType.BLOCK,pos.up()));
                }
            });
        });
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client-> {
            if(readyWorld!=client.world)readyWorld=null;
            if(cameraWorld!=client.world) {cameraWorld=null;transit=null;roll=0;}
            if(transit!=null && client.world.getTime()-transit.tick>10)transit=null;
            while(resetOrientation.wasPressed())if(client.currentScreen==null && WormholePair.active(client.world)
                    && ClientPlayNetworking.canSend(WormholeResetPayload.ID))
                ClientPlayNetworking.send(WormholeResetPayload.INSTANCE);
        });
    }
    static String resetHint() {return resetOrientation.getBoundKeyLocalizedText().getString()+": upright";}
    public static boolean ready() {return readyWorld!=null && readyWorld==MinecraftClient.getInstance().world;}
    public static double roll() {return cameraWorld==MinecraftClient.getInstance().world?roll:0;}
    /** Called after vanilla has accepted the matching position packet and sent its acknowledgement. */
    public static void afterTeleport() {
        var client=MinecraftClient.getInstance();var p=client.player;var t=transit;
        if(t==null||p==null||cameraWorld!=client.world)return;
        if(p.getEyePos().squaredDistanceTo(t.payload.target())>1e-6) {transit=null;return;}
        roll=t.payload.roll();p.setVelocity(t.velocity);
        // Keep adjacent frame positions in the same chart. The old chart's last
        // tick can lie inside the new chart's throat, which the ray solver supports.
        if(t.previousEye.squaredDistanceTo(t.payload.eye())<16 && t.previousEye.squaredDistanceTo(WormholePair.centre(t.payload.from()))>1e-8) {
            var previous=WormholePair.transfer(t.payload.from(),t.previousEye);
            p.prevX=previous.x;p.prevY=previous.y-p.getStandingEyeHeight();p.prevZ=previous.z;
            p.lastRenderX=p.prevX;p.lastRenderY=p.prevY;p.lastRenderZ=p.prevZ;
        }
        Interstellar.LOGGER.info("Wormhole client crossing applied: eye={}, velocity={}, roll={}",p.getEyePos(),p.getVelocity(),Math.toDegrees(roll));
        transit=null;
    }
}
