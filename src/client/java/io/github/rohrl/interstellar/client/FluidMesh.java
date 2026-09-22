package io.github.rohrl.interstellar.client;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.BlockPos;

/** Vanilla FluidRenderer emits section-local quads with biome tint, light and face shade. */
final class FluidMesh implements VertexConsumer {
    private final WorldMesh mesh;
    private final float[] quad=new float[48];
    private int count=-1,x,y,z;
    private float material;
    FluidMesh(WorldMesh mesh) {this.mesh=mesh;}
    void begin(BlockPos pos,BlockPos origin,boolean water) {
        x=(pos.getX()&~15)-origin.getX();y=(pos.getY()&~15)-origin.getY();z=(pos.getZ()&~15)-origin.getZ();
        count=-1;material=water?-3:0;
    }
    private void endVertex() {if(count==3){mesh.entityQuad(quad,false);count=-1;}}
    void finish() {endVertex();if(count!=-1)throw new IllegalStateException("Incomplete fluid quad");}
    @Override public VertexConsumer vertex(float a,float b,float c) {
        endVertex();int p=++count*12;quad[p]=a+x;quad[p+1]=b+y;quad[p+2]=c+z;quad[p+3]=material;return this;
    }
    @Override public VertexConsumer color(int r,int g,int b,int a) {int p=count*12+8;quad[p]=r/255f;quad[p+1]=g/255f;quad[p+2]=b/255f;quad[p+3]=a/255f;return this;}
    @Override public VertexConsumer texture(float u,float v) {int p=count*12+4;quad[p]=u;quad[p+1]=v;return this;}
    @Override public VertexConsumer light(int block,int sky) {int p=count*12+6;quad[p]=block/256f;quad[p+1]=sky/256f;return this;}
    @Override public VertexConsumer normal(float a,float b,float c) {return this;}
    @Override public VertexConsumer overlay(int a,int b) {return this;}
}
