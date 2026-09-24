package io.github.rohrl.interstellar.client;

import io.github.rohrl.interstellar.scene.MeshTree;
import java.util.Arrays;

/** Only glowing model surfaces. Separate from terrain so native through-wall outlines
 * can follow curved rays without retracing the entire terrain tree. Allocated on demand. */
final class GlowingMesh implements AutoCloseable {
    private float[] data=new float[36*256];
    private int count,vertexRows,nodeRows;
    int nodes;
    MeshArena vertices,tree;
    void reset() {count=nodes=0;}
    void quad(float[] quad,boolean twoSided,int colour) {
        if((count+2)*36>data.length)data=Arrays.copyOf(data,data.length*2);
        int[] indices={0,1,2,2,3,0};
        int cursor=count*36;
        for(int i:indices) {
            System.arraycopy(quad,i*12,data,cursor,12);
            float sign=Math.signum(data[cursor+3]);
            data[cursor+3]=sign*((Math.abs(data[cursor+3])>=32?32:0)+(twoSided?2:1));
            data[cursor+8]=((colour>>16)&255)/255f;data[cursor+9]=((colour>>8)&255)/255f;data[cursor+10]=(colour&255)/255f;
            cursor+=12;
        }
        count+=2;
    }
    void upload() {
        if(count==0)return;
        var mesh=new MeshTree(data,count);var values=mesh.nodes();nodes=mesh.size();
        int vr=(count*36+16383)/16384,nr=(values.length+16379)/16380;
        if(vertices==null || vr>vertexRows) {if(vertices!=null)vertices.close();vertexRows=Integer.highestOneBit(vr)*2;vertices=new MeshArena(vertexRows,false,4096);}
        if(tree==null || nr>nodeRows) {if(tree!=null)tree.close();nodeRows=Integer.highestOneBit(nr)*2;tree=new MeshArena(nodeRows,true);}
        vertices.write(0,data,count*36);tree.write(0,values,values.length);
    }
    public void close() {if(vertices!=null)vertices.close();if(tree!=null)tree.close();}
}
