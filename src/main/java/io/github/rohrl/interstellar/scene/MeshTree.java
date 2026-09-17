package io.github.rohrl.interstellar.scene;

import java.util.Arrays;

/** Preorder BVH. Each vertex is position/pad, UV/light UV, RGBA (12 floats).
 * Nodes are min/escape, max/first triangle, count/padding; leaves contain at most 8 triangles. */
public final class MeshTree {
    public static final int STRIDE=36;
    private final float[] triangles;
    private float[] nodes=new float[1200];
    private int size;
    public MeshTree(float[] triangles,int count) {
        if(count<0 || count>triangles.length/STRIDE)throw new IllegalArgumentException("Invalid triangle count");
        this.triangles=triangles;
        if(count>0)build(0,count,0);
    }
    public float[] nodes() {return Arrays.copyOf(nodes,size*12);}
    public int size() {return size;}
    private int build(int first,int count,int depth) {
        int node=size++;
        if(size*12>nodes.length)nodes=Arrays.copyOf(nodes,nodes.length*2);
        int base=node*12;
        Arrays.fill(nodes,base,base+3,Float.POSITIVE_INFINITY);
        Arrays.fill(nodes,base+4,base+7,Float.NEGATIVE_INFINITY);
        for(int i=first;i<first+count;i++)for(int v=0;v<3;v++)for(int a=0;a<3;a++) {
            float value=triangles[i*STRIDE+v*12+a];
            nodes[base+a]=Math.min(nodes[base+a],value);
            nodes[base+4+a]=Math.max(nodes[base+4+a],value);
        }
        nodes[base+7]=first;
        if(count<=8 || depth>=48)nodes[base+8]=count;
        else {
            int axis=0;
            for(int a=1;a<3;a++)if(nodes[base+4+a]-nodes[base+a]>nodes[base+4+axis]-nodes[base+axis])axis=a;
            float middle=(nodes[base+axis]+nodes[base+4+axis])*.5f;
            int left=first,right=first+count-1;
            while(left<=right) {
                if(centroid(left,axis)<middle)left++;
                else {swap(left,right);right--;}
            }
            int split=left==first || left==first+count?first+count/2:left;
            build(first,split-first,depth+1);build(split,first+count-split,depth+1);
        }
        nodes[base+3]=size;
        return node;
    }
    private float centroid(int triangle,int axis) {
        int p=triangle*STRIDE+axis;
        return (triangles[p]+triangles[p+12]+triangles[p+24])/3;
    }
    private void swap(int a,int b) {
        if(a==b)return;
        for(int i=0;i<STRIDE;i++) {float v=triangles[a*STRIDE+i];triangles[a*STRIDE+i]=triangles[b*STRIDE+i];triangles[b*STRIDE+i]=v;}
    }
}
