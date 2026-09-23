package io.github.rohrl.interstellar.scene;

import java.util.ArrayList;
import java.util.Arrays;

/** Native geometry trees per actor, flattened under a spatial tree of their exact bounds. */
final class ActorMeshTree {
    private record Group(float[] nodes) {}
    private final Group[] groups;
    private final float[] nodes;
    private int size;

    static float[] build(float[] triangles,int count,int[] owners) {
        if(count==0)return new float[0];
        if(owners.length<count)throw new IllegalArgumentException("Missing actor ownership");
        long[] order=new long[count];
        for(int i=0;i<count;i++)order[i]=((long)owners[i]<<32)|(i&0xffffffffL);
        Arrays.sort(order);
        float[] sorted=new float[count*36];
        for(int i=0;i<count;i++)System.arraycopy(triangles,(int)order[i]*36,sorted,i*36,36);
        System.arraycopy(sorted,0,triangles,0,sorted.length);
        var groups=new ArrayList<Group>();int total=0;
        for(int first=0;first<count;) {
            int end=first+1;
            while(end<count && (order[end]>>32)==(order[first]>>32))end++;
            var tree=new MeshTree(triangles,first,end-first,3);
            groups.add(new Group(tree.nodes()));total+=tree.size();first=end;
        }
        var tree=new ActorMeshTree(groups.toArray(Group[]::new),total+groups.size()-1);
        tree.build(0,groups.size());return tree.nodes;
    }
    private ActorMeshTree(Group[] groups,int nodes) {this.groups=groups;this.nodes=new float[nodes*12];}
    private void build(int first,int count) {
        int root=size,base=root*12;
        if(count==1) {
            float[] source=groups[first].nodes;
            System.arraycopy(source,0,nodes,base,source.length);
            for(int i=0;i<source.length;i+=12)nodes[base+i+3]+=root;
            size+=source.length/12;return;
        }
        size++;
        Arrays.fill(nodes,base,base+3,Float.POSITIVE_INFINITY);
        Arrays.fill(nodes,base+4,base+7,Float.NEGATIVE_INFINITY);
        for(int i=first;i<first+count;i++)for(int a=0;a<3;a++) {
            nodes[base+a]=Math.min(nodes[base+a],groups[i].nodes[a]);
            nodes[base+4+a]=Math.max(nodes[base+4+a],groups[i].nodes[4+a]);
        }
        int axis=0;
        for(int a=1;a<3;a++)if(nodes[base+4+a]-nodes[base+a]>nodes[base+4+axis]-nodes[base+axis])axis=a;
        float middle=(nodes[base+axis]+nodes[base+4+axis])*.5f;
        int left=first,right=first+count-1;
        while(left<=right) {
            float[] bounds=groups[left].nodes;
            if((bounds[axis]+bounds[axis+4])*.5f<middle)left++;
            else {Group old=groups[left];groups[left]=groups[right];groups[right--]=old;}
        }
        int split=left==first || left==first+count?first+count/2:left;
        build(first,split-first);build(split,first+count-split);
        nodes[base+3]=size;
    }
}
