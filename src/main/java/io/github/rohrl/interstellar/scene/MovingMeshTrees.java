package io.github.rohrl.interstellar.scene;

/** Independent actor/cloud roots in one vertex buffer and one node buffer. All attributes stay exact. */
public record MovingMeshTrees(float[] triangles,float[] nodes,int actorNodes,int cloudNodes,int cloudVertexBase) {
    public static MovingMeshTrees build(float[] source,int count) {
        return build(source,count,false);
    }
    public static MovingMeshTrees build(float[] source,int count,boolean cloudQuads) {
        if(count<0 || count>source.length/MeshTree.STRIDE)throw new IllegalArgumentException("Invalid triangle count");
        int actors=0;
        for(int i=0;i<count;i++)if(!cloud(source[i*36+3]))actors++;
        float[] data=new float[count*36];int actor=0,cloud=actors;
        for(int i=0;i<count;i++) {
            int destination=cloud(source[i*36+3])?cloud++:actor++;
            System.arraycopy(source,i*36,data,destination*36,36);
        }
        var actorTree=new MeshTree(data,0,actors,3);
        MeshTree cloudTree;
        if(cloudQuads) {
            float[] quads=QuadVertices.pack(java.util.Arrays.copyOfRange(data,actors*36,count*36),count-actors);
            for(int q=0;q<quads.length;q+=48)CloudQuads.describe(quads,q);
            cloudTree=new MeshTree(quads,quads.length/48,4);
            data=java.util.Arrays.copyOf(data,actors*36+quads.length);
            System.arraycopy(quads,0,data,actors*36,quads.length);
        } else cloudTree=new MeshTree(data,actors,count-actors,3);
        float[] a=actorTree.nodes(),c=cloudTree.nodes(),nodes=new float[a.length+c.length];
        System.arraycopy(a,0,nodes,0,a.length);
        for(int i=0;i<c.length;i+=12)c[i+3]+=actorTree.size();
        // Triangle leaves use absolute triangle indices; quad leaves use quad indices
        // relative to cloudVertexBase (in vec4 texels). Escape links always address nodes.
        System.arraycopy(c,0,nodes,a.length,c.length);
        return new MovingMeshTrees(data,nodes,actorTree.size(),cloudTree.size(),cloudQuads?actors*9:0);
    }
    private static boolean cloud(float material) {return material==5 || material==6;}
}
