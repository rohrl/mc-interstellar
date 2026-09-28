package io.github.rohrl.interstellar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.rohrl.interstellar.Interstellar;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.*;
import net.minecraft.client.texture.SpriteAtlasTexture;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.function.Consumer;

/** Opt-in frozen export. Blocking readback/file I/O never participates in production timing. */
final class TerrainReplay {
    static final boolean ENABLED=Boolean.getBoolean("interstellar.rtxProbe");
    static final ShaderProgram[] PROGRAMS=new ShaderProgram[2];
    private static final int CAPACITY=1_048_576;
    static boolean supported() {return GL.getCapabilities().GL_ARB_shader_storage_buffer_object && GL.getCapabilities().GL_ARB_shading_language_420pack;}
    private TerrainReplay() {}
    static String interopOnly() {
        try(PackState pack=new PackState()) {
            Path path=Path.of("rtx-native","interop-"+System.currentTimeMillis());Files.createDirectories(path);
            Class.forName("io.github.rohrl.interstellar.client.RtxInteropSmoke").getMethod("run",Path.class).invoke(null,path);
            return "GPU image-sharing checks saved under "+path;
        } catch(Exception failure) {Interstellar.LOGGER.error("RTX interop smoke failed",failure);return "Interop check failed; see game log";}
    }

    static Path capture(Path scene,int w,int h,int mask,WorldMesh terrain,WorldMesh moving,
                        ShaderProgram[] baseline,Consumer<ShaderProgram> configure,Runnable draw,String metadata) {
        if(GL30.glGetIntegeri(GL43.GL_SHADER_STORAGE_BUFFER_BINDING,0)!=0)
            throw new IllegalStateException("Diagnostic binding zero already in use");
        int oldBuffer=GL11.glGetInteger(GL43.GL_SHADER_STORAGE_BUFFER_BINDING),ssbo=GL15.glGenBuffers();
        SimpleFramebuffer target=new SimpleFramebuffer(w*2,h,false,false);
        FloatBuffer reference=MemoryUtil.memAllocFloat(w*2*h*4),pixels=MemoryUtil.memAllocFloat(w*2*h*4);
        StringBuilder checks=new StringBuilder(metadata+"\nlogical="+w+"x"+h+"; renderer="+GL11.glGetString(GL11.GL_RENDERER)+"; GL="+GL11.glGetString(GL11.GL_VERSION)+"\nstride=16; two AA samples; raw native chords; tree masks preserve recorded empty-region reuse\n");
        try(PackState pack=new PackState()) {
            if(scene==null) {
                scene=Path.of("rtx-native","capture-"+System.currentTimeMillis());Files.createDirectories(scene);
                exportGeometry(scene,terrain,moving);
            }
            Path view=scene.resolve("view-"+System.currentTimeMillis());Files.createDirectory(view);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D,target.getColorAttachment());
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL30.GL_RGBA32F,w*2,h,0,GL11.GL_RGBA,GL11.GL_FLOAT,(FloatBuffer)null);
            target.beginWrite(false);
            if(GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER)!=GL30.GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Replay target incomplete");
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER,ssbo);GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER,16L+CAPACITY*48L,GL15.GL_DYNAMIC_READ);
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER,0,ssbo);
            for(int pass=0;pass<2;pass++) {
                draw(baseline[pass],w,h,mask,target,configure,draw);reference.clear();GL11.glReadPixels(0,0,w*2,h,GL11.GL_RGBA,GL11.GL_FLOAT,reference);
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER,ssbo);GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER,0,new int[4]);
                draw(PROGRAMS[pass],w,h,mask,target,configure,draw);
                GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT|GL42.GL_BUFFER_UPDATE_BARRIER_BIT);
                pixels.clear();GL11.glReadPixels(0,0,w*2,h,GL11.GL_RGBA,GL11.GL_FLOAT,pixels);
                long differences=0;double maximum=0;
                for(int i=0;i<pixels.capacity();i++) {
                    float a=reference.get(i),b=pixels.get(i);
                    if(!Float.isFinite(a)||!Float.isFinite(b))throw new IllegalStateException("Nonfinite replay colour");
                    if(a!=b)differences++;maximum=Math.max(maximum,Math.abs(a-b));
                }
                int[] header=new int[4];GL15.glGetBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER,0,header);
                if(header[0]<0||header[0]>CAPACITY||header[1]!=0||header[2]!=0)throw new IllegalStateException("Incomplete replay: "+java.util.Arrays.toString(header));
                ByteBuffer records=MemoryUtil.memAlloc(header[0]*48).order(ByteOrder.LITTLE_ENDIAN);
                try {
                    if(records.hasRemaining())GL15.glGetBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER,16,records);
                    for(int i=0;i<header[0];i++) {
                        double length=0;
                        for(int a=0;a<3;a++) {float start=records.getFloat(i*48+a*4),delta=records.getFloat(i*48+16+a*4);
                            if(!Float.isFinite(start)||!Float.isFinite(delta))throw new IllegalStateException("Nonfinite replay segment");length+=delta*delta;}
                        if(length==0||records.getInt(i*48+44)>7)throw new IllegalStateException("Invalid/unwritten replay segment");
                    }
                    write(view.resolve(pass==0?"probe.bin":"mask.bin"),new int[]{0x49525259,1,header[0],48},records);
                } finally {MemoryUtil.memFree(records);}
                checks.append("pass=").append(pass).append(" queries=").append(header[0]).append(" colourDifferences=").append(differences).append(" maxError=").append(maximum).append('\n');
                if(differences!=0)throw new IllegalStateException("Replay instrumentation changed pixels: "+checks);
            }
            Files.writeString(view.resolve("capture.txt"),checks);
            Interstellar.LOGGER.info("Native RTX replay completed: {}; {}",view.toAbsolutePath(),checks);
            return scene;
        } catch(Exception failure) {throw new IllegalStateException("RTX diagnostic failed",failure);}
        finally {
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER,0,0);GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER,oldBuffer);GL15.glDeleteBuffers(ssbo);
            MemoryUtil.memFree(reference);MemoryUtil.memFree(pixels);target.delete();
        }
    }
    private static void draw(ShaderProgram program,int w,int h,int mask,SimpleFramebuffer target,Consumer<ShaderProgram> configure,Runnable draw) {
        configure.accept(program);program.addSampler("PendingRays",mask);RenderSystem.setShader(()->program);
        target.beginWrite(false);RenderSystem.viewport(0,0,w*2,h);GL11.glClearColor(0,0,0,0);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        for(int sample=0;sample<2;sample++) {RenderSystem.viewport(sample*w,0,w,h);program.getUniformOrDefault("SampleOffset").set(sample==0?-.25f:.25f);draw.run();}
    }
    static Path exportFrozenGeometry(WorldMesh terrain,WorldMesh moving) throws Exception {
        Path directory=Path.of("rtx-image","scene-"+System.currentTimeMillis());Files.createDirectories(directory);
        try(PackState state=new PackState()) {exportGeometry(directory,terrain,moving);}
        return directory;
    }
    private static void exportGeometry(Path directory,WorldMesh terrain,WorldMesh moving) throws Exception {
        int[][] spans=terrain.replaySpans();int terrainCount=java.util.Arrays.stream(spans).mapToInt(s->s[1]*2).sum();
        try(FileChannel file=FileChannel.open(directory.resolve("triangles.bin"),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)) {
            ByteBuffer header=ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN);
            header.asIntBuffer().put(new int[]{0x49525458,1,terrainCount+moving.triangleCount(),terrainCount,moving.triangleCount()});write(file,header);
            for(int[] span:spans) {
                int rows=(span[1]+340)/341;
                ByteBuffer quads=readTexture(terrain.triangleTexture,0,span[0],4092,rows,true);
                ByteBuffer triangles=MemoryUtil.memAlloc(span[1]*72*4).order(ByteOrder.LITTLE_ENDIAN);
                try {
                    for(int q=0;q<span[1];q++)for(int corner:new int[]{0,1,2,2,3,0})for(int component=0;component<12;component++)triangles.putFloat(quads.getFloat((q*48+corner*12+component)*4));
                    triangles.flip();write(file,triangles);
                } finally {MemoryUtil.memFree(quads);MemoryUtil.memFree(triangles);}
            }
            ByteBuffer actors=MemoryUtil.memAlloc(moving.triangleCount()*36*4).order(ByteOrder.LITTLE_ENDIAN);
            try {actors.asFloatBuffer().put(moving.triangleData(),0,moving.triangleCount()*36);write(file,actors);}finally {MemoryUtil.memFree(actors);}
        }
        var client=MinecraftClient.getInstance();
        texture(directory.resolve("atlas.bin"),client.getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).getGlId());
        texture(directory.resolve("entities.bin"),moving.entities.texture);texture(directory.resolve("clouds.bin"),moving.clouds.texture);
        Files.writeString(directory.resolve("scene.txt"),"version=1; triangles=36 native floats (position/tag, UV/light, RGBA per corner); little endian\nterrain="+terrainCount+" moving="+moving.triangleCount()+"\n");
    }
    private static void texture(Path file,int texture) throws Exception {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);int w=GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_WIDTH),h=GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_HEIGHT);
        ByteBuffer pixels=readTexture(texture,0,0,w,h,false);
        try {write(file,new int[]{w,h},pixels);}finally {MemoryUtil.memFree(pixels);}
    }
    static ByteBuffer readTexture(int texture,int x,int y,int w,int h,boolean floats) {
        int previous=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),fbo=GL30.glGenFramebuffers();
        ByteBuffer data=MemoryUtil.memAlloc(w*h*4*(floats?4:1)).order(ByteOrder.LITTLE_ENDIAN);
        try {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,fbo);GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,GL11.GL_TEXTURE_2D,texture,0);
            if(GL30.glCheckFramebufferStatus(GL30.GL_READ_FRAMEBUFFER)!=GL30.GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Export texture is not readable");
            GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);GL11.glReadPixels(x,y,w,h,GL11.GL_RGBA,floats?GL11.GL_FLOAT:GL11.GL_UNSIGNED_BYTE,data);return data;
        } catch(RuntimeException failure){MemoryUtil.memFree(data);throw failure;}
        finally {GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,previous);GL30.glDeleteFramebuffers(fbo);}
    }
    private static void write(Path path,int[] header,ByteBuffer body) throws Exception {
        try(FileChannel file=FileChannel.open(path,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)) {ByteBuffer bytes=ByteBuffer.allocate(header.length*4).order(ByteOrder.LITTLE_ENDIAN);bytes.asIntBuffer().put(header);write(file,bytes);write(file,body);}
    }
    private static void write(FileChannel file,ByteBuffer data) throws java.io.IOException {while(data.hasRemaining())file.write(data);}
    static final class PackState implements AutoCloseable {
        final int texture=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D),pack=GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING),unpack=GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        final int[] names={GL11.GL_PACK_ALIGNMENT,GL11.GL_PACK_ROW_LENGTH,GL11.GL_PACK_SKIP_PIXELS,GL11.GL_PACK_SKIP_ROWS,GL11.GL_UNPACK_ALIGNMENT,GL11.GL_UNPACK_ROW_LENGTH,GL11.GL_UNPACK_SKIP_PIXELS,GL11.GL_UNPACK_SKIP_ROWS},values=new int[names.length];
        PackState(){for(int i=0;i<names.length;i++){values[i]=GL11.glGetInteger(names[i]);GL11.glPixelStorei(names[i],i%4==0?4:0);}GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,0);GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,0);}
        public void close(){for(int i=0;i<names.length;i++)GL11.glPixelStorei(names[i],values[i]);GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,pack);GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,unpack);GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);}
    }
}
