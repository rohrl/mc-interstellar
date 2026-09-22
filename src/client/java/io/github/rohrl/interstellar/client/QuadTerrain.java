package io.github.rohrl.interstellar.client;

import io.github.rohrl.interstellar.scene.*;
import java.util.*;

/** Parallel terrain representation for same-scene A/B tests; moving actors remain unchanged. */
final class QuadTerrain implements AutoCloseable {
    private static final int ROWS=10924;
    final MeshArena vertices=new MeshArena(ROWS,false,QuadVertices.WIDTH);
    final MeshArena nodes;
    private final RowArena vertexRows=new RowArena(0,ROWS),nodeRows=new RowArena(4,4092);
    private record Entry(int vertexRow,int vertexRows,int nodeRow,int nodeRows,int quads,int nodes,SceneTree.Part part) {}
    private final Map<Long,Entry> entries=new HashMap<>();
    int nodeCount;
    private int quads,nodeTotal;
    private long buildNanos;
    private boolean dirty;
    QuadTerrain() {try {nodes=new MeshArena(4096,true);}catch(RuntimeException e){vertices.close();throw e;}}
    void publish(long key,float[] triangles,int count) {
        long started=System.nanoTime();
        if(count==0){remove(key);return;}
        var data=QuadVertices.pack(triangles,count);int size=count/2;
        var tree=new MeshTree(data,size,4);var treeNodes=tree.nodes();
        int vr=(size+QuadVertices.PER_ROW-1)/QuadVertices.PER_ROW,nr=(tree.size()+MeshArena.NODES-1)/MeshArena.NODES;
        int v=vertexRows.allocate(vr),n;
        try {n=nodeRows.allocate(nr);}catch(RuntimeException e){vertexRows.release(v,vr);throw e;}
        int base=n*MeshArena.NODES,first=v*QuadVertices.PER_ROW;
        var part=new SceneTree.Part(treeNodes[0],treeNodes[1],treeNodes[2],treeNodes[4],treeNodes[5],treeNodes[6],base);
        for(int i=0;i<tree.size();i++){int p=i*12;treeNodes[p+3]=treeNodes[p+3]==tree.size()?-1:treeNodes[p+3]+base;treeNodes[p+7]+=first;}
        try {vertices.write(v,data,data.length);nodes.write(n,treeNodes,treeNodes.length);}
        catch(RuntimeException e){vertexRows.release(v,vr);nodeRows.release(n,nr);throw e;}
        var old=entries.put(key,new Entry(v,vr,n,nr,size,tree.size(),part));if(old!=null)release(old);
        quads+=size;nodeTotal+=tree.size();index();buildNanos+=System.nanoTime()-started;
    }
    void remove(long key) {var old=entries.remove(key);if(old!=null){release(old);dirty=true;}}
    void refresh() {if(dirty)index();}
    private void release(Entry e) {vertexRows.release(e.vertexRow,e.vertexRows);nodeRows.release(e.nodeRow,e.nodeRows);quads-=e.quads;nodeTotal-=e.nodes;}
    private void index() {
        var parts=new ArrayList<SceneTree.Part>();for(var e:entries.values())parts.add(e.part);
        var data=new SceneTree(parts).nodes();nodeCount=data.length/12;
        if(data.length>4*MeshArena.FLOATS)throw new IllegalStateException("Quad scene index capacity exceeded");
        nodes.write(0,data,data.length);dirty=false;
    }
    String status() {return quads+" quads | "+nodeTotal+" nodes | vertex payload "+(quads*192L)+" bytes | added packing/build/upload "+(buildNanos/1e6)+" ms";}
    @Override public void close() {vertices.close();nodes.close();entries.clear();}
}
