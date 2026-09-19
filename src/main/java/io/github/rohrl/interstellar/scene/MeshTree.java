package io.github.rohrl.interstellar.scene;

import java.util.Arrays;

/** Preorder BVH. Each vertex is position/pad, UV/light UV, RGBA (12 floats).
 * Nodes are min/escape, max/first triangle, count/padding; leaves contain at most 8 triangles. */
public final class MeshTree {
    public static final int STRIDE=36;
    private static final int BINS=12;
    private final float[] triangles;
    private final boolean surfaceArea;
    // Scratch storage is reused: a split is finished before either child recurses.
    private final int[] binCounts;
    private final float[] binBounds,centroidBounds,running;
    private final double[] leftCosts;
    private float[] nodes=new float[1200];
    private int size;
    public MeshTree(float[] triangles,int count) {
        this(triangles,count,false);
    }
    public MeshTree(float[] triangles,int count,boolean surfaceArea) {
        if(count<0 || count>triangles.length/STRIDE)throw new IllegalArgumentException("Invalid triangle count");
        this.triangles=triangles;this.surfaceArea=surfaceArea;
        binCounts=surfaceArea?new int[BINS]:null;binBounds=surfaceArea?new float[BINS*6]:null;
        centroidBounds=surfaceArea?new float[6]:null;running=surfaceArea?new float[6]:null;leftCosts=surfaceArea?new double[BINS]:null;
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
            int split=surfaceArea?areaSplit(first,count):-1;
            if(split<0)split=midpointSplit(first,count,base);
            build(first,split-first,depth+1);build(split,first+count-split,depth+1);
        }
        nodes[base+3]=size;
        return node;
    }
    private int midpointSplit(int first,int count,int base) {
            int axis=0;
            for(int a=1;a<3;a++)if(nodes[base+4+a]-nodes[base+a]>nodes[base+4+axis]-nodes[base+axis])axis=a;
            float middle=(nodes[base+axis]+nodes[base+4+axis])*.5f;
            int left=first,right=first+count-1;
            while(left<=right) {
                if(centroid(left,axis)<middle)left++;
                else {swap(left,right);right--;}
            }
            return left==first || left==first+count?first+count/2:left;
    }
    /** Choose the binned split minimizing child area times triangle count.
     * This changes only grouping/order, never geometry or the intersection tests. */
    private int areaSplit(int first,int count) {
        emptyBounds(centroidBounds,0);
        for(int i=first;i<first+count;i++)for(int a=0;a<3;a++) {
            float c=centroid(i,a);
            centroidBounds[a]=Math.min(centroidBounds[a],c);centroidBounds[a+3]=Math.max(centroidBounds[a+3],c);
        }
        double bestCost=Double.POSITIVE_INFINITY;
        int bestAxis=-1,bestBin=-1;float bestLow=0,bestScale=0;
        for(int axis=0;axis<3;axis++) {
            float low=centroidBounds[axis],span=centroidBounds[axis+3]-low;
            if(!(span>0))continue;
            float scale=BINS/span;
            Arrays.fill(binCounts,0);for(int b=0;b<BINS;b++)emptyBounds(binBounds,b*6);
            for(int i=first;i<first+count;i++) {
                int bin=bin(centroid(i,axis),low,scale),base=bin*6;binCounts[bin]++;
                for(int a=0;a<3;a++)for(int v=0;v<3;v++) {
                    float value=triangles[i*STRIDE+v*12+a];
                    binBounds[base+a]=Math.min(binBounds[base+a],value);
                    binBounds[base+a+3]=Math.max(binBounds[base+a+3],value);
                }
            }
            emptyBounds(running,0);int left=0;
            for(int b=0;b<BINS-1;b++) {
                left+=binCounts[b];if(binCounts[b]>0)includeBin(b);
                leftCosts[b]=left==0?Double.POSITIVE_INFINITY:area(running)*left;
            }
            emptyBounds(running,0);int right=0;
            for(int b=BINS-1;b>0;b--) {
                right+=binCounts[b];if(binCounts[b]>0)includeBin(b);
                double cost=right==0?Double.POSITIVE_INFINITY:leftCosts[b-1]+area(running)*right;
                if(cost<bestCost) {bestCost=cost;bestAxis=axis;bestBin=b-1;bestLow=low;bestScale=scale;}
            }
        }
        if(bestAxis<0)return -1;
        int left=first,right=first+count-1;
        while(left<=right) {
            if(bin(centroid(left,bestAxis),bestLow,bestScale)<=bestBin)left++;
            else {swap(left,right);right--;}
        }
        // Degenerate data must still make progress, without dropping any triangle.
        return left==first || left==first+count?-1:left;
    }
    private static int bin(float value,float low,float scale) {return Math.max(0,Math.min(BINS-1,(int)((value-low)*scale)));}
    private static void emptyBounds(float[] data,int base) {
        Arrays.fill(data,base,base+3,Float.POSITIVE_INFINITY);Arrays.fill(data,base+3,base+6,Float.NEGATIVE_INFINITY);
    }
    private void includeBin(int bin) {
        for(int a=0;a<3;a++) {running[a]=Math.min(running[a],binBounds[bin*6+a]);running[a+3]=Math.max(running[a+3],binBounds[bin*6+a+3]);}
    }
    private static double area(float[] bounds) {
        double x=bounds[3]-bounds[0],y=bounds[4]-bounds[1],z=bounds[5]-bounds[2];
        return x*y+y*z+z*x; // Common factor two does not affect split selection.
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
