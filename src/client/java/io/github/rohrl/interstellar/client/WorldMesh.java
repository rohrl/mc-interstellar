package io.github.rohrl.interstellar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.rohrl.interstellar.Interstellar;
import io.github.rohrl.interstellar.scene.MeshTree;
import net.minecraft.block.BlockRenderType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;
import java.util.Arrays;

/** Frozen, client-only native model capture. No filled-column approximation or generated chunks. */
final class WorldMesh implements VertexConsumer,AutoCloseable {
    private static final int CHUNKS=16,MAX_TRIANGLES=4_000_000;
    private final ClientWorld world;
    private final BlockPos origin;
    private final int minChunkX,minChunkZ,sections,total;
    private final MatrixStack matrices=new MatrixStack();
    private final Random random=Random.create(0);
    private final float[] quadData=new float[48];
    private final Vector3f position=new Vector3f();
    private float[] triangles=new float[36*8192];
    private int cursor,count,missingSections,omittedBlocks;
    private final long started=System.nanoTime();
    int triangleTexture,nodeTexture,nodeCount;
    EntityMesh entities;
    WorldMesh(ClientWorld world,BlockPos origin,BlockPos centre) {
        this.world=world;this.origin=origin;
        minChunkX=(centre.getX()>>4)-CHUNKS/2;minChunkZ=(centre.getZ()>>4)-CHUNKS/2;
        sections=world.countVerticalSections();total=CHUNKS*CHUNKS*sections*4096;
    }
    boolean ready() {return nodeTexture!=0;}
    String status() {return ready()?"Native mesh: "+count+" triangles | "+missingSections+" missing sections | "+entities.status():
            "Capturing native mesh: "+(100L*cursor/total)+"%";}
    void advance() {
        if(ready())return;
        var manager=MinecraftClient.getInstance().getBlockRenderManager();
        var pos=new BlockPos.Mutable();
        long deadline=System.nanoTime()+5_000_000;
        net.minecraft.client.render.block.BlockModelRenderer.enableBrightnessCache();
        try {
            while(cursor<total && System.nanoTime()<deadline) {
                int section=cursor/4096,block=cursor%4096,chunk=section/sections;
                int cx=minChunkX+chunk%CHUNKS,cz=minChunkZ+chunk/CHUNKS,sy=section%sections;
                if(!world.getChunkManager().isChunkLoaded(cx,cz)) {missingSections++;cursor=(section+1)*4096;continue;}
                var terrain=world.getChunk(cx,cz);
                if(terrain.getSection(sy).isEmpty()) {cursor=(section+1)*4096;continue;}
                pos.set(cx*16+(block&15),world.getBottomY()+sy*16+(block>>8),cz*16+((block>>4)&15));
                cursor++;
                var state=world.getBlockState(pos);
                if(state.isAir())continue;
                if(!state.getFluidState().isEmpty())omittedBlocks++;
                if(state.getRenderType()!=BlockRenderType.MODEL)continue;
                if(RenderLayers.getBlockLayer(state)==RenderLayer.getTranslucent()) {omittedBlocks++;continue;}
                matrices.push();
                try {
                    matrices.translate(pos.getX()-origin.getX(),pos.getY()-origin.getY(),pos.getZ()-origin.getZ());
                    manager.getModelRenderer().render(world,manager.getModel(state),state,pos,matrices,this,true,random,state.getRenderingSeed(pos),OverlayTexture.DEFAULT_UV);
                } finally {matrices.pop();}
            }
        } finally {net.minecraft.client.render.block.BlockModelRenderer.disableBrightnessCache();}
        if(cursor==total) {
            entities=new EntityMesh(this);entities.capture(origin,minChunkX,minChunkZ,CHUNKS);
            var tree=new MeshTree(triangles,count);nodeCount=tree.size();
            upload(tree.nodes());triangles=null;
            Interstellar.LOGGER.info("World mesh ready: {}; {} omitted blocks; chunks=({}, {})..({}, {}), full build height; capture/build/upload={} ms",status(),omittedBlocks,minChunkX,minChunkZ,minChunkX+CHUNKS-1,minChunkZ+CHUNKS-1,(System.nanoTime()-started)/1e6);
        }
    }
    @Override public void quad(MatrixStack.Entry entry,BakedQuad quad,float[] brightness,float red,float green,float blue,float alpha,int[] light,int overlay,boolean useQuadColor) {
        int[] vertices=quad.getVertexData();int stride=vertices.length/4;
        for(int i=0;i<4;i++) {
            int src=i*stride,dst=i*12;
            position.set(Float.intBitsToFloat(vertices[src]),Float.intBitsToFloat(vertices[src+1]),Float.intBitsToFloat(vertices[src+2]));
            entry.getPositionMatrix().transformPosition(position);
            quadData[dst]=position.x;quadData[dst+1]=position.y;quadData[dst+2]=position.z;
            quadData[dst+4]=Float.intBitsToFloat(vertices[src+4]);quadData[dst+5]=Float.intBitsToFloat(vertices[src+5]);
            // Terrain's native shader filters UV2/256; entity shaders use discrete texel centres.
            quadData[dst+6]=(light[i]&65535)/256f;quadData[dst+7]=(light[i]>>>16)/256f;
            int colour=vertices[src+3];
            quadData[dst+8]=red*brightness[i]*(useQuadColor?(colour&255)/255f:1);
            quadData[dst+9]=green*brightness[i]*(useQuadColor?((colour>>>8)&255)/255f:1);
            quadData[dst+10]=blue*brightness[i]*(useQuadColor?((colour>>>16)&255)/255f:1);
            quadData[dst+11]=alpha;
        }
        add(0,1,2);add(2,3,0);
    }
    private void add(int a,int b,int c) {
        if(count==MAX_TRIANGLES)throw new IllegalStateException("Native mesh exceeds four million triangles; capture refused (no silent truncation)");
        if((count+1)*36>triangles.length)triangles=Arrays.copyOf(triangles,Math.min(MAX_TRIANGLES*36,triangles.length*2));
        int dst=count++*36;
        System.arraycopy(quadData,a*12,triangles,dst,12);System.arraycopy(quadData,b*12,triangles,dst+12,12);System.arraycopy(quadData,c*12,triangles,dst+24,12);
    }
    void entityQuad(float[] data,boolean twoSided) {
        System.arraycopy(data,0,quadData,0,48);
        if(twoSided)for(int v=0;v<4;v++)quadData[v*12+3]*=2;
        add(0,1,2);add(2,3,0);
    }
    private void upload(float[] nodes) {
        int previous=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D),pbo=GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        int[] names={GL11.GL_UNPACK_ALIGNMENT,GL11.GL_UNPACK_ROW_LENGTH,GL11.GL_UNPACK_SKIP_ROWS,GL11.GL_UNPACK_SKIP_PIXELS};
        int[] saved=new int[names.length];
        for(int i=0;i<names.length;i++) {saved[i]=GL11.glGetInteger(names[i]);GL11.glPixelStorei(names[i],i==0?4:0);}
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,0);
        try {triangleTexture=texture(triangles,count*36);nodeTexture=texture(nodes,nodes.length);}
        finally {
            for(int i=0;i<names.length;i++)GL11.glPixelStorei(names[i],saved[i]);
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,pbo);RenderSystem.bindTexture(previous);
        }
    }
    private static int texture(float[] data,int length) {
        int height=Math.max(1,(length+16383)/16384);
        if(height>GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE))throw new IllegalStateException("Native mesh exceeds GPU texture dimensions");
        var buffer=MemoryUtil.memCallocFloat(height*16384);
        int id=GL11.glGenTextures();
        try {
            buffer.put(data,0,length).position(0);RenderSystem.bindTexture(id);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL30.GL_RGBA32F,4096,height,0,GL11.GL_RGBA,GL11.GL_FLOAT,buffer);
            return id;
        } catch(RuntimeException e) {RenderSystem.deleteTexture(id);throw e;}
        finally {MemoryUtil.memFree(buffer);}
    }
    @Override public void close() {triangles=null;if(entities!=null) {entities.close();entities=null;}if(triangleTexture!=0)RenderSystem.deleteTexture(triangleTexture);if(nodeTexture!=0)RenderSystem.deleteTexture(nodeTexture);triangleTexture=nodeTexture=0;}
    @Override public VertexConsumer vertex(float x,float y,float z) {throw new IllegalStateException("Expected native quad");}
    @Override public VertexConsumer color(int r,int g,int b,int a) {return this;}
    @Override public VertexConsumer texture(float u,float v) {return this;}
    @Override public VertexConsumer overlay(int u,int v) {return this;}
    @Override public VertexConsumer light(int u,int v) {return this;}
    @Override public VertexConsumer normal(float x,float y,float z) {return this;}
}
