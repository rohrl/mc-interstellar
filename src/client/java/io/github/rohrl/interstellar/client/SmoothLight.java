package io.github.rohrl.interstellar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.rohrl.interstellar.Interstellar;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/** Native block renderer supplies corner AO and lightmap coordinates; identical records are shared. */
final class SmoothLight implements AutoCloseable,VertexConsumer {
    private static final int PER_ROW=128,MAX_RECORDS=65536;
    private record Key(int[] values) {
        @Override public int hashCode() {return Arrays.hashCode(values);}
        @Override public boolean equals(Object other) {return other instanceof Key key && Arrays.equals(values,key.values);}
    }
    private final ArrayList<int[]> rows=new ArrayList<>();
    private final HashMap<Key,Integer> ids=new HashMap<>();
    private final MatrixStack matrices=new MatrixStack();
    private final Random random=Random.create(0);
    private int[] values=new int[24];
    private int seen,capped;
    int texture;
    SmoothLight() {rows.add(new int[24]);}
    int capture(ClientWorld world,BlockPos pos,BlockState state) {
        Arrays.fill(values,0);seen=0;
        var manager=MinecraftClient.getInstance().getBlockRenderManager();
        matrices.push();
        try {manager.getModelRenderer().render(world,manager.getModel(state),state,pos,matrices,this,false,random,0,OverlayTexture.DEFAULT_UV);}
        finally {matrices.pop();}
        if(seen!=63)return 0; // Preserve face-light fallback for unsupported model layouts.
        var key=new Key(values);var existing=ids.get(key);if(existing!=null)return existing;
        if(rows.size()==MAX_RECORDS) {capped++;return 0;}
        int id=rows.size();var stored=values.clone();rows.add(stored);ids.put(new Key(stored),id);return id;
    }
    @Override public void quad(MatrixStack.Entry entry,BakedQuad quad,float[] brightness,float red,float green,float blue,float alpha,int[] light,int overlay,boolean useQuadColor) {
        int face=quad.getFace().getId();if((seen&(1<<face))!=0)return;
        int[] vertex=quad.getVertexData();int stride=vertex.length/4;
        int s=quad.getFace().getAxis()==Direction.Axis.X?2:0;
        int t=quad.getFace().getAxis()==Direction.Axis.Y?2:1;
        float minS=Float.POSITIVE_INFINITY,maxS=Float.NEGATIVE_INFINITY,minT=minS,maxT=maxS;
        for(int i=0;i<4;i++) {
            float a=Float.intBitsToFloat(vertex[i*stride+s]),b=Float.intBitsToFloat(vertex[i*stride+t]);
            minS=Math.min(minS,a);maxS=Math.max(maxS,a);minT=Math.min(minT,b);maxT=Math.max(maxT,b);
        }
        int corners=0,first=0,third=0;
        for(int i=0;i<4;i++) {
            int corner=(Float.intBitsToFloat(vertex[i*stride+s])>(minS+maxS)*.5f?1:0)
                    +(Float.intBitsToFloat(vertex[i*stride+t])>(minT+maxT)*.5f?2:0);
            corners|=1<<corner;if(i==0)first=corner;if(i==2)third=corner;
            int shade=Math.clamp(Math.round(brightness[i]*255),0,255);
            values[face*4+corner]=(light[i]&255)|(((light[i]>>>16)&255)<<8)|(shade<<16);
        }
        if(corners!=15)return;
        // Sign encodes the quad's triangulation; magnitude remains an exact 24-bit integer.
        if((first==0 && third==3)||(first==3 && third==0))values[face*4]=-values[face*4];
        seen|=1<<face;
    }
    // BlockModelRenderer uses quad above; receiving a raw vertex would invalidate the capture contract.
    @Override public VertexConsumer vertex(float x,float y,float z) {throw new IllegalStateException("Expected native block quad");}
    @Override public VertexConsumer color(int r,int g,int b,int a) {return this;}
    @Override public VertexConsumer texture(float u,float v) {return this;}
    @Override public VertexConsumer overlay(int u,int v) {return this;}
    @Override public VertexConsumer light(int u,int v) {return this;}
    @Override public VertexConsumer normal(float x,float y,float z) {return this;}
    void upload() {
        int height=(rows.size()+PER_ROW-1)/PER_ROW;
        var buffer=MemoryUtil.memCallocFloat(PER_ROW*height*24);
        try {
            for(int i=0;i<rows.size();i++)for(int j=0;j<24;j++)buffer.put(i*24+j,rows.get(i)[j]);
            texture=GL11.glGenTextures();RenderSystem.bindTexture(texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL30.GL_RGBA32F,PER_ROW*6,height,0,GL11.GL_RGBA,GL11.GL_FLOAT,buffer);
            Interstellar.LOGGER.info("Smooth light atlas: {} records, {} KiB GPU, {} capped captures",rows.size(),buffer.capacity()*4/1024,capped);
        } finally {MemoryUtil.memFree(buffer);rows.clear();ids.clear();values=null;}
    }
    @Override public void close() {rows.clear();ids.clear();values=null;if(texture!=0)RenderSystem.deleteTexture(texture);}
}
