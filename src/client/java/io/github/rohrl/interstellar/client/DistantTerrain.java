package io.github.rohrl.interstellar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;
import java.nio.FloatBuffer;
import net.minecraft.util.math.Vec3d;
import io.github.rohrl.interstellar.science.BoxRay;

/** Experimental 256-square height field. Local cube owns its volume exclusively.
 * RG = upper surface height/material, BA = surface below the local cube.
 * Unknown columns use material 1, empty columns 0, water -1. Heights are local. */
final class DistantTerrain implements AutoCloseable {
    static final int SIDE=256, OFFSET=(SIDE-TerrainSnapshot.SIDE)/2;
    private final TerrainSnapshot scene;
    private final FloatBuffer data=MemoryUtil.memCallocFloat(SIDE*SIDE*4);
    private final BlockPos.Mutable pos=new BlockPos.Mutable();
    private int cursor, lowerY=Integer.MIN_VALUE;
    int texture;
    float maxHeight=-1024;
    DistantTerrain(TerrainSnapshot scene) {this.scene=scene;}
    boolean ready() {return cursor==SIDE*SIDE;}
    void advance() {
        long deadline=System.nanoTime()+2_000_000;
        for(int reads=0;reads<4096 && !ready();reads++) {
            int x=cursor%SIDE-OFFSET,z=cursor/SIDE-OFFSET,base=cursor*4;
            pos.set(scene.origin.getX()+x,0,scene.origin.getZ()+z);
            if(!scene.world.getChunkManager().isChunkLoaded(pos.getX()>>4,pos.getZ()>>4)) {
                data.put(base+1,1);data.put(base+3,1);cursor++;lowerY=Integer.MIN_VALUE;
            } else if(lowerY!=Integer.MIN_VALUE) {
                pos.setY(lowerY);
                if(lowerY<scene.world.getBottomY()) {cursor++;lowerY=Integer.MIN_VALUE;}
                else if(!scene.world.getBlockState(pos).isAir()) {
                    surface(base+2);cursor++;lowerY=Integer.MIN_VALUE;
                } else lowerY--;
            } else {
                int top=scene.world.getTopY(Heightmap.Type.WORLD_SURFACE,pos.getX(),pos.getZ())-1;
                pos.setY(top);
                if(top>=scene.world.getBottomY()) surface(base);
                boolean local=x>=0 && x<TerrainSnapshot.SIDE && z>=0 && z<TerrainSnapshot.SIDE;
                if(local && top>=scene.origin.getY()) lowerY=scene.origin.getY()-1;
                else {data.put(base+2,data.get(base));data.put(base+3,data.get(base+1));cursor++;}
            }
            if((reads&63)==63 && System.nanoTime()>=deadline)break;
        }
    }
    private void surface(int index) {
        var state=scene.world.getBlockState(pos);
        float height=state.isOf(Blocks.SNOW)?state.get(SnowBlock.LAYERS)/8f:1;
        int material=state.isAir()?0:!state.getFluidState().isEmpty()?-1:scene.materialId(state,pos);
        data.put(index,pos.getY()-scene.origin.getY()+height);
        data.put(index+1,material);
        int x=pos.getX()-scene.origin.getX(),z=pos.getZ()-scene.origin.getZ();
        boolean local=x>=0 && x<TerrainSnapshot.SIDE && z>=0 && z<TerrainSnapshot.SIDE;
        float top=data.get(index);
        if(material!=0 && material!=1 && (!local || top<=0 || top>TerrainSnapshot.SIDE))maxHeight=Math.max(maxHeight,top);
    }
    record Hit(double distance,int material) {}
    // Independent brute-force slabs, used only by the explicit GPU diagnostic.
    Hit reference(Vec3d camera,Vec3d direction) {
        double nearest=1024;int material=0;
        for(int z=-OFFSET;z<SIDE-OFFSET;z++)for(int x=-OFFSET;x<SIDE-OFFSET;x++) {
            boolean local=x>=0 && x<TerrainSnapshot.SIDE && z>=0 && z<TerrainSnapshot.SIDE;
            int base=((z+OFFSET)*SIDE+x+OFFSET)*4;
            for(int layer=0;layer<(local?2:1);layer++) {
                int id=(int)data.get(base+layer*2+1);if(id==0 || id==1)continue;
                int bottom=local && layer==0?TerrainSnapshot.SIDE:-1024;
                double top=data.get(base+layer*2);
                if(local && layer==1)top=Math.min(0,top);
                if(top<=bottom)continue;
                double t=BoxRay.entry(camera.x,camera.y,camera.z,direction.x,direction.y,direction.z,x,bottom,z,top-bottom);
                if(t<nearest) {nearest=t;material=id;}
            }
        }
        return new Hit(nearest,material);
    }
    // Caller owns unpack state isolation, shared with local texture upload.
    void upload() {
        texture=GL11.glGenTextures();RenderSystem.bindTexture(texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL30.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL30.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL30.GL_RGBA32F,SIDE,SIDE,0,GL11.GL_RGBA,GL11.GL_FLOAT,data);
    }
    @Override public void close() {MemoryUtil.memFree(data);if(texture!=0)RenderSystem.deleteTexture(texture);}
}
