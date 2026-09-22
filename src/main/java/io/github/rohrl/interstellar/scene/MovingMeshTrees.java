package io.github.rohrl.interstellar.scene;

/** Independent actor/cloud roots in one vertex buffer and one node buffer. All attributes stay exact. */
public record MovingMeshTrees(float[] triangles,float[] nodes,int actorNodes,int cloudNodes) {
    public static MovingMeshTrees build(float[] source,int count) {
        if(count<0 || count>source.length/MeshTree.STRIDE)throw new IllegalArgumentException("Invalid triangle count");
        int actors=0;
        for(int i=0;i<count;i++)if(!cloud(source[i*36+3]))actors++;
        float[] data=new float[count*36];int actor=0,cloud=actors;
        for(int i=0;i<count;i++) {
            int destination=cloud(source[i*36+3])?cloud++:actor++;
            System.arraycopy(source,i*36,data,destination*36,36);
        }
        var actorTree=new MeshTree(data,0,actors,3);
        var cloudTree=new MeshTree(data,actors,count-actors,3);
        float[] a=actorTree.nodes(),c=cloudTree.nodes(),nodes=new float[a.length+c.length];
        System.arraycopy(a,0,nodes,0,a.length);
        for(int i=0;i<c.length;i+=12)c[i+3]+=actorTree.size();
        // Cloud primitive offsets are already absolute because its builder uses a source range.
        System.arraycopy(c,0,nodes,a.length,c.length);
        return new MovingMeshTrees(data,nodes,actorTree.size(),cloudTree.size());
    }
    private static boolean cloud(float material) {return material==5 || material==6;}
}
