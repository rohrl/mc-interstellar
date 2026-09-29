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
    static final io.github.rohrl.interstellar.wormhole.OpeningProgress opening=new io.github.rohrl.interstellar.wormhole.OpeningProgress();
    private static WormholeReadyPayload preparedRegions;
    private static boolean opticalPrepared,acknowledged;
    private static long lastProgress;
    static int renderingOptics;
    private static ClientWorld readyWorld;
    private static ClientWorld cameraWorld;
    private static double roll;
    private static Transit transit;
    private static KeyBinding resetOrientation;
    private record Transit(WormholeTransitPayload payload,Vec3d velocity,Vec3d previousEye,long tick) {}
    private WormholeClient() {}
    public static void register() {
        WormholeAppearance.register();
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler,client)-> {
            WormholeAppearance.changed();
            resetOpening(false);
            WormholePair.clientLayout(null,WormholePair.EMPTY);readyWorld=null;cameraWorld=null;transit=null;roll=0;
        });
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(io.github.rohrl.interstellar.wormhole.WormholeSeed.ENTITY,
            net.minecraft.client.render.entity.FlyingItemEntityRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(io.github.rohrl.interstellar.wormhole.WormholeLayoutPayload.ID,(payload,context)-> {
            WormholeAppearance.changed();
            var world=context.client().world;if(world==null)return;
            WormholePair.clientLayout(world,payload.layout());readyWorld=null;transit=null;roll=0;cameraWorld=world;
            resetOpening(payload.layout().active(world));
            if(context.client().currentScreen instanceof TerrainScreen screen && screen.isWormhole())context.client().setScreen(null);
            ((WormholeChunkCache)world.getChunkManager()).interstellar$refreshRegions();
            LiveTerrain.wormholeChanged();
            Interstellar.LOGGER.info("Wormhole client layout: revision={}, mouths={}",payload.layout().revision(),payload.layout().mouths());
        });
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
            transit=new Transit(payload,WormholePair.transferVector(client.world,payload.from(),payload.eye(),p.getVelocity()),
                new Vec3d(p.prevX,p.prevY+p.getStandingEyeHeight(),p.prevZ),client.world.getTime());
        });
        ClientPlayNetworking.registerGlobalReceiver(WormholeReadyPayload.ID,(payload,context)-> {
            var world=context.client().world;if(!WormholePair.active(world)||payload.revision()!=WormholePair.layout(world).revision())return;
            // Chunk data is applied immediately, but native light packets are queued.
            // Insert the readiness barrier in that same ordered queue.
            world.enqueueChunkUpdate(()-> {
                if(context.client().world!=world||payload.revision()!=WormholePair.layout(world).revision())return;
                int count=((WormholeChunkCache)world.getChunkManager()).interstellar$remoteChunkCount();
                if(count!=WormholePair.chunks(world).size()) {
                    Interstellar.LOGGER.error("Wormhole readiness rejected: {}/{} chunks",count,WormholePair.chunks(world).size());return;
                }
                readyWorld=world;preparedRegions=payload;
                Interstellar.LOGGER.info("Wormhole native regions ready: {} chunks with light data applied",count);
                for(int end=0;end<2;end++) {
                    var pos=net.minecraft.util.math.BlockPos.ofFloored(WormholePair.centre(world,end)).add(20,-12,20);
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
            while(resetOrientation.wasPressed())if(client.currentScreen==null)resetOrientation();
        });
    }
    static String resetHint() {return resetOrientation.getBoundKeyLocalizedText().getString()+": upright";}
    static boolean canResetOrientation() {return WormholePair.active(MinecraftClient.getInstance().world)
        && ClientPlayNetworking.canSend(WormholeResetPayload.ID);}
    static void resetOrientation() {if(canResetOrientation())ClientPlayNetworking.send(WormholeResetPayload.INSTANCE);}
    public static boolean ready() {return readyWorld!=null && readyWorld==MinecraftClient.getInstance().world;}
    static boolean nearby() {
        var client=MinecraftClient.getInstance();if(!WormholePair.present(client.world)||client.player==null)return false;
        var eye=client.player.getEyePos();double reach=client.options.getViewDistance().getValue()*16+32;
        return eye.squaredDistanceTo(WormholePair.centre(client.world,WormholePair.nearest(client.world,eye)))<=reach*reach;
    }
    private static void resetOpening(boolean paired) {
        opening.reset(paired);preparedRegions=null;opticalPrepared=acknowledged=false;renderingOptics=0;lastProgress=System.nanoTime();
    }
    static void progress(double geometry) {
        var client=MinecraftClient.getInstance();long now=System.nanoTime();
        double dt=lastProgress==0?0:(now-lastProgress)/1e9;lastProgress=now;
        int total=WormholePair.chunks(client.world).size();
        double received=total==0?0:Math.min(1,((WormholeChunkCache)client.world.getChunkManager()).interstellar$remoteChunkCount()/(double)total);
        opening.advance(.2*received+.8*geometry,opticalPrepared,client.isPaused()?0:dt);
    }
    static void opticalFrame(boolean passage) {
        opticalPrepared=true;
        if(passage)opening.presented();
        if(passage && opening.open() && !acknowledged && preparedRegions!=null && ready()) {
            ClientPlayNetworking.send(preparedRegions);acknowledged=true;
            Interstellar.LOGGER.info("Wormhole passage visually open: revision={}, renderer frame presented; travel acknowledged",preparedRegions.revision());
        }
    }
    static boolean passageOpen() {return opening.open() && WormholePair.active(MinecraftClient.getInstance().world);}
    static double closedRadius() {return opening.radius(WormholePair.METRIC.mouthRadius());}
    public static double roll() {return cameraWorld==MinecraftClient.getInstance().world?roll:0;}
    /** Called after vanilla has accepted the matching position packet and sent its acknowledgement. */
    public static void afterTeleport() {
        var client=MinecraftClient.getInstance();var p=client.player;var t=transit;
        if(t==null||p==null||cameraWorld!=client.world)return;
        if(p.getEyePos().squaredDistanceTo(t.payload.target())>1e-6) {transit=null;return;}
        roll=t.payload.roll();p.setVelocity(t.velocity);
        // Keep adjacent frame positions in the same chart. The old chart's last
        // tick can lie inside the new chart's throat, which the ray solver supports.
        if(t.previousEye.squaredDistanceTo(t.payload.eye())<16 && t.previousEye.squaredDistanceTo(WormholePair.centre(client.world,t.payload.from()))>1e-8) {
            var previous=WormholePair.transfer(client.world,t.payload.from(),t.previousEye);
            p.prevX=previous.x;p.prevY=previous.y-p.getStandingEyeHeight();p.prevZ=previous.z;
            p.lastRenderX=p.prevX;p.lastRenderY=p.prevY;p.lastRenderZ=p.prevZ;
        }
        Interstellar.LOGGER.info("Wormhole client crossing applied: eye={}, velocity={}, roll={}",p.getEyePos(),p.getVelocity(),Math.toDegrees(roll));
        transit=null;
    }
}
