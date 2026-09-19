package io.github.rohrl.interstellar.client;

import com.google.gson.GsonBuilder;
import io.github.rohrl.interstellar.Interstellar;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.util.LinkedHashMap;

/** Opt-in same-frame appearance pairs. Never changes simulation, camera or saved settings. */
final class AppearanceCapture {
    private static TerrainScreen requested;
    private static int comparison;
    private static BufferedImage reference;
    private static LinkedHashMap<String,Object> metadata;
    static void register() {
        WorldFog.register();
        EntityMesh.register();
        WorldRenderEvents.END.register(context -> {
            WorldProjection.capture(context.projectionMatrix());
            if(requested==null)return;
            var client=MinecraftClient.getInstance();
            if(client.currentScreen!=requested) {clear();return;}
            try {
                if(!client.isInSingleplayer() || !client.isPaused())throw new IllegalStateException("Use a paused singleplayer F9 view");
                requested.checkAppearancePose();
                reference=readFramebuffer();
                var camera=client.gameRenderer.getCamera();
                metadata=new LinkedHashMap<>();
                metadata.put("schema",1);
                metadata.put("reference",comparison==7?"same-scene-general-program":comparison==6?"same-scene-cache-reach-16":comparison==5?"same-scene-no-empty-cell-cache":comparison==4?"same-scene-original-addressing-same-bounds-path-AA-and-scale":comparison==3?"same-scene-original-bounds-same-path-AA-and-scale":comparison==2?"same-scene-original-path-same-AA-and-scale":comparison==1?"same-scene-four-rays-full-resolution":"vanilla-world-END-before-hand-and-HUD");
                metadata.put("candidate",comparison!=0?"same-scene-selected-AA-scale-and-path":"terrain-backend-zero-bending-full-resolution");
                metadata.put("qualitySettings",requested.qualitySettings());
                metadata.put("sameFrame",true);
                metadata.put("width",reference.getWidth());metadata.put("height",reference.getHeight());
                metadata.put("camera",new double[]{camera.getPos().x,camera.getPos().y,camera.getPos().z});
                metadata.put("yaw",camera.getYaw());metadata.put("pitch",camera.getPitch());
                metadata.put("projection",context.projectionMatrix().get(new float[16]));
                metadata.put("configuredFov",client.options.getFov().getValue());
                metadata.put("candidateVerticalFov",WorldProjection.current().verticalFov());
                metadata.put("candidateRaySlopes",WorldProjection.current());
                metadata.put("terrainFog",WorldFog.current());
                metadata.put("gamma",client.options.getGamma().getValue());
                metadata.put("perspective",client.options.getPerspective().name());
                metadata.put("dimension",client.world.getRegistryKey().getValue().toString());
                metadata.put("worldTime",client.world.getTime());metadata.put("timeOfDay",client.world.getTimeOfDay());
                metadata.put("rain",client.world.getRainGradient(1));metadata.put("thunder",client.world.getThunderGradient(1));
                metadata.put("modVersion",FabricLoader.getInstance().getModContainer("interstellar").orElseThrow().getMetadata().getVersion().getFriendlyString());
            } catch(Exception failure) {fail(failure);}
        });
    }
    static void request(TerrainScreen screen,int mode) {clear();requested=screen;comparison=mode;}
    static void finish(TerrainScreen screen) {
        if(requested!=screen || reference==null)return;
        try {
            screen.checkAppearancePose();
            metadata.put("candidateScene",screen.appearanceScene());
            if(comparison==7) {
                screen.renderShaderComparison(true);reference=readFramebuffer();screen.renderShaderComparison(false);
            } else if(comparison==6) {
                screen.renderReachComparison(true);reference=readFramebuffer();screen.renderReachComparison(false);
            } else if(comparison==5) {
                screen.renderCellComparison(true);reference=readFramebuffer();screen.renderCellComparison(false);
            } else if(comparison==4) {
                screen.renderFetchComparison(true);reference=readFramebuffer();screen.renderFetchComparison(false);
            } else if(comparison==3) {
                screen.renderBoundsComparison(true);reference=readFramebuffer();screen.renderBoundsComparison(false);
            } else if(comparison==2) {
                screen.renderPathComparison(true);reference=readFramebuffer();screen.renderPathComparison(false);
            } else if(comparison==1) {
                screen.renderQuality(true);reference=readFramebuffer();screen.renderQuality(false);
            } else screen.renderAppearanceCandidate();
            var candidate=readFramebuffer();
            if(candidate.getWidth()!=reference.getWidth() || candidate.getHeight()!=reference.getHeight())throw new IllegalStateException("Framebuffer resized during capture");
            var root=FabricLoader.getInstance().getGameDir().resolve("interstellar-captures");
            Files.createDirectories(root);
            var dir=Files.createTempDirectory(root,"pair-");
            ImageIO.write(reference,"PNG",dir.resolve("reference.png").toFile());
            ImageIO.write(candidate,"PNG",dir.resolve("candidate.png").toFile());
            Files.writeString(dir.resolve("metadata.json"),new GsonBuilder().setPrettyPrinting().create().toJson(metadata));
            screen.appearanceStatus("Pair saved: "+dir.getFileName());
            Interstellar.LOGGER.info("Appearance pair saved: {}",dir.toAbsolutePath());
        } catch(Exception failure) {fail(failure);}
        finally {clear();}
    }
    private static void fail(Exception failure) {
        if(requested!=null)requested.appearanceStatus("Pair rejected: "+failure.getMessage());
        Interstellar.LOGGER.warn("Appearance capture failed",failure);clear();
    }
    private static void clear() {requested=null;reference=null;metadata=null;comparison=0;}
    private static BufferedImage readFramebuffer() {
        var framebuffer=MinecraftClient.getInstance().getFramebuffer();
        int w=framebuffer.textureWidth,h=framebuffer.textureHeight;
        var pixels=MemoryUtil.memAlloc(Math.multiplyExact(Math.multiplyExact(w,h),4));
        int read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),readBuffer=GL11.glGetInteger(GL11.GL_READ_BUFFER);
        int pack=GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
        int[] names={GL11.GL_PACK_ALIGNMENT,GL11.GL_PACK_ROW_LENGTH,GL11.GL_PACK_SKIP_ROWS,GL11.GL_PACK_SKIP_PIXELS};
        int[] saved=new int[4];for(int i=0;i<4;i++)saved[i]=GL11.glGetInteger(names[i]);
        try {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,framebuffer.fbo);GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,0);
            for(int i=0;i<4;i++)GL11.glPixelStorei(names[i],i==0?1:0);
            GL11.glReadPixels(0,0,w,h,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
            var image=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
            for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                int p=4*(x+(h-1-y)*w);
                image.setRGB(x,y,((pixels.get(p)&255)<<16)|((pixels.get(p+1)&255)<<8)|(pixels.get(p+2)&255));
            }
            return image;
        } finally {
            for(int i=0;i<4;i++)GL11.glPixelStorei(names[i],saved[i]);
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,pack);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);GL11.glReadBuffer(readBuffer);
            MemoryUtil.memFree(pixels);
        }
    }
}
