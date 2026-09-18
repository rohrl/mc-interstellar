package io.github.rohrl.interstellar.scene;

import java.util.*;

/** Small top-level BVH. Negative leaf count means jump to an independently allocated chunk BVH. */
public final class SceneTree {
    public record Part(float minX,float minY,float minZ,float maxX,float maxY,float maxZ,int root) {
        float min(int a) {return a==0?minX:a==1?minY:minZ;}
        float max(int a) {return a==0?maxX:a==1?maxY:maxZ;}
    }
    private final Part[] parts;
    private final float[] nodes;
    private int size;
    public SceneTree(Collection<Part> input) {
        parts=input.toArray(Part[]::new);nodes=new float[Math.max(0,parts.length*2-1)*12];
        if(parts.length>0)build(0,parts.length);
    }
    public float[] nodes() {return nodes;}
    private void build(int from,int to) {
        int node=size++,base=node*12;
        Arrays.fill(nodes,base,base+3,Float.POSITIVE_INFINITY);Arrays.fill(nodes,base+4,base+7,Float.NEGATIVE_INFINITY);
        for(int i=from;i<to;i++)for(int a=0;a<3;a++) {nodes[base+a]=Math.min(nodes[base+a],parts[i].min(a));nodes[base+4+a]=Math.max(nodes[base+4+a],parts[i].max(a));}
        if(to-from==1) {nodes[base+7]=parts[from].root();nodes[base+8]=-1;}
        else {
            int axis=0;for(int a=1;a<3;a++)if(nodes[base+4+a]-nodes[base+a]>nodes[base+4+axis]-nodes[base+axis])axis=a;
            final int sortAxis=axis;
            Arrays.sort(parts,from,to,Comparator.comparingDouble(p->p.min(sortAxis)+p.max(sortAxis)));
            int mid=(from+to)/2;build(from,mid);build(mid,to);
        }
        nodes[base+3]=size;
    }
}
