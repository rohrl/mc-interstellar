package io.github.rohrl.interstellar.scene;

import java.util.Arrays;

/** Preorder BVH. Each vertex is position/pad, UV/light UV, RGBA (12 floats).
 * Nodes are min/escape, max/first primitive, count/padding; leaves contain at most 8 triangle tests. */
public final class MeshTree {
    public static final int STRIDE=36;
    private final float[] triangles;
    private final int verticesPerPrimitive,stride;
    private float[] nodes=new float[1200];
    private int size;
    public MeshTree(float[] triangles,int count) {
        this(triangles,count,3);
    }
    public MeshTree(float[] triangles,int count,int verticesPerPrimitive) {
        this(triangles,0,count,verticesPerPrimitive);
    }
    public MeshTree(float[] triangles,int first,int count,int verticesPerPrimitive) {
        if(verticesPerPrimitive!=3 && verticesPerPrimitive!=4)throw new IllegalArgumentException("Expected triangles or quads");
        this.verticesPerPrimitive=verticesPerPrimitive;stride=verticesPerPrimitive*12;
        if(first<0 || count<0 || first>triangles.length/stride || count>triangles.length/stride-first)throw new IllegalArgumentException("Invalid primitive range");
        this.triangles=triangles;
        if(count>0)build(first,count,0);
    }
    public float[] nodes() {return Arrays.copyOf(nodes,size*12);}
    public int size() {return size;}
    private int build(int first,int count,int depth) {
        int node=size++;
        if(size*12>nodes.length)nodes=Arrays.copyOf(nodes,nodes.length*2);
        int base=node*12;
        Arrays.fill(nodes,base,base+3,Float.POSITIVE_INFINITY);
        Arrays.fill(nodes,base+4,base+7,Float.NEGATIVE_INFINITY);
        for(int i=first;i<first+count;i++)for(int v=0;v<verticesPerPrimitive;v++)for(int a=0;a<3;a++) {
            float value=triangles[i*stride+v*12+a];
            nodes[base+a]=Math.min(nodes[base+a],value);
            nodes[base+4+a]=Math.max(nodes[base+4+a],value);
        }
        nodes[base+7]=first;
        if(count<=(verticesPerPrimitive==4?4:8) || depth>=48)nodes[base+8]=count;
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
        int p=triangle*stride+axis;
        return verticesPerPrimitive==3?(triangles[p]+triangles[p+12]+triangles[p+24])/3:
            (triangles[p]+triangles[p+12]+triangles[p+24]+triangles[p+36])/4;
    }
    private void swap(int a,int b) {
        if(a==b)return;
        for(int i=0;i<stride;i++) {float v=triangles[a*stride+i];triangles[a*stride+i]=triangles[b*stride+i];triangles[b*stride+i]=v;}
    }
}
