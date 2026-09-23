package io.github.rohrl.interstellar.scene;

/** Independent actor/cloud roots in one vertex buffer and one node buffer. All attributes stay exact. */
public record MovingMeshTrees(float[] triangles,float[] nodes,int actorNodes,int cloudNodes) {
    public static MovingMeshTrees build(float[] source,int count) {
        return build(source,count,null);
    }
    /** Ownership follows raw capture order, including quads flushed on the next actor's first vertex. */
    public static MovingMeshTrees build(float[] source,int count,int[] owners) {
        if(count<0 || count>source.length/MeshTree.STRIDE)throw new IllegalArgumentException("Invalid triangle count");
        if(owners!=null && owners.length<count)throw new IllegalArgumentException("Missing actor ownership");
        int actors=0;
        for(int i=0;i<count;i++)if(!cloud(source[i*36+3]))actors++;
        float[] data=new float[count*36];int actor=0,cloud=actors;
        int[] actorOwners=owners==null?null:new int[actors];
        for(int i=0;i<count;i++) {
            int destination=cloud(source[i*36+3])?cloud++:actor++;
            System.arraycopy(source,i*36,data,destination*36,36);
            if(actorOwners!=null && destination<actors)actorOwners[destination]=owners[i];
        }
        float[] a=actorOwners==null?new MeshTree(data,0,actors,3).nodes():ActorMeshTree.build(data,actors,actorOwners);
        int actorNodes=a.length/12;
        var cloudTree=new MeshTree(data,actors,count-actors,3);
        float[] c=cloudTree.nodes(),nodes=new float[a.length+c.length];
        System.arraycopy(a,0,nodes,0,a.length);
        for(int i=0;i<c.length;i+=12)c[i+3]+=actorNodes;
        // Cloud primitive offsets are already absolute because its builder uses a source range.
        System.arraycopy(c,0,nodes,a.length,c.length);
        return new MovingMeshTrees(data,nodes,actorNodes,cloudTree.size());
    }
    private static boolean cloud(float material) {return material==5 || material==6;}
}
