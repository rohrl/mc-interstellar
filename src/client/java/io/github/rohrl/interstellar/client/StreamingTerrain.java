package io.github.rohrl.interstellar.client;

import io.github.rohrl.interstellar.Interstellar;
import io.github.rohrl.interstellar.scene.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Loaded-chunk geometry cache with independent GPU allocation and bounded capture slices. */
public final class StreamingTerrain implements AutoCloseable {
    private static volatile StreamingTerrain active;
    private final ClientWorld world;
    private final BlockPos origin,centre;
    private final MeshArena vertices,nodes;
    private final RowArena vertexRows=new RowArena(0,10924),nodeRows=new RowArena(4,4092);
    private record Entry(int vertexRow,int vertexRowCount,int nodeRow,int nodeRows,int triangles,SceneTree.Part part,boolean materials) {}
    private final Map<Long,Entry> entries=new HashMap<>();
    private final Map<Long,Long> versions=new HashMap<>();
    private final Set<Long> incoming=ConcurrentHashMap.newKeySet();
    private final Set<Long> wanted=new HashSet<>();
    private final LinkedHashSet<Long> queue=new LinkedHashSet<>();
    private WorldMesh capture;
    private long capturing,captureVersion,published;
    private int cameraX=Integer.MIN_VALUE,cameraZ,distance,triangleCount,materialChunks;
    private record Window(int x,int z,int radius) {}
    private volatile Window window;
    int nodeCount;
    float extent=512;
    private boolean ready;
    private final long started=System.nanoTime();
    public static void register() {
        ClientChunkEvents.CHUNK_LOAD.register((world,chunk)->chunkEvent(world,chunk.getPos()));
        ClientChunkEvents.CHUNK_UNLOAD.register((world,chunk)->chunkEvent(world,chunk.getPos()));
    }
    private static void chunkEvent(ClientWorld world,ChunkPos pos) {
        var cache=active;if(cache==null || cache.world!=world)return;
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)dirty(pos.x+x,pos.z+z);
    }
    public static void dirty(int x,int z) {
        var cache=active;if(cache==null)return;var w=cache.window;
        if(w!=null && Math.abs((long)x-w.x)<=w.radius+1 && Math.abs((long)z-w.z)<=w.radius+1)cache.incoming.add(ChunkPos.toLong(x,z));
    }
    StreamingTerrain(ClientWorld world,BlockPos origin,BlockPos centre) {
        this.world=world;this.origin=origin;this.centre=centre;
        vertices=new MeshArena(10924,false,QuadVertices.WIDTH);
        try {nodes=new MeshArena(4096,true);}catch(RuntimeException e){vertices.close();throw e;}
        active=this;
    }
    boolean ready() {return ready;}
    boolean hasMaterials() {return materialChunks>0;}
    int loadingPercent() {return wanted.isEmpty()?0:Math.min(100,entries.size()*100/wanted.size());}
    int triangleTexture() {return vertices.texture;}
    int nodeTexture() {return nodes.texture;}
    int compactNodeTexture() {return nodes.texture;}
    int quantizedNodeTexture() {return nodes.quantizedTexture();}
    String status() {return ready?"Streaming terrain | "+triangleCount+" triangles | "+queue.size()+" queued chunks":"Loading terrain: "+entries.size()+"/"+wanted.size()+" chunks";}
    void advance() {
        var client=MinecraftClient.getInstance();
        int range=client.options.getViewDistance().getValue();
        if(range>16)throw new IllegalStateException("Native mesh supports render distance up to16");
        var position=BlockPos.ofFloored(client.gameRenderer.getCamera().getPos());
        int x=position.getX()>>4,z=position.getZ()>>4;
        if(x!=cameraX || z!=cameraZ || range!=distance)window(x,z,range);
        for(var iterator=incoming.iterator();iterator.hasNext();) {
            long key=iterator.next();iterator.remove();
            if(wanted.contains(key)){versions.merge(key,1L,Long::sum);queue.add(key);}
        }
        boolean indexChanged=false;
        // Unloaded geometry disappears promptly, even if other chunks are waiting to rebuild.
        for(long key:List.copyOf(queue))if(!loaded(key)) {
            var old=entries.put(key,new Entry(0,0,0,0,0,null,false));if(old!=null){release(old);indexChanged|=old.part!=null;}
            queue.remove(key);
            if(capture!=null && capturing==key){capture.close();capture=null;}
        }
        if(indexChanged)index();
        if(capture==null && !queue.isEmpty()) {
            var iterator=queue.iterator();capturing=iterator.next();iterator.remove();
            captureVersion=versions.getOrDefault(capturing,0L);
            capture=WorldMesh.chunk(world,origin,centre,ChunkPos.getPackedX(capturing),ChunkPos.getPackedZ(capturing));
        }
        if(capture!=null) {
            capture.advance();
            if(capture.ready()) {
                if(wanted.contains(capturing) && loaded(capturing) && captureVersion==versions.getOrDefault(capturing,0L))publish(capturing,capture);
                else if(wanted.contains(capturing))queue.add(capturing);
                capture.close();capture=null;
            }
        }
        if(!ready && entries.keySet().containsAll(wanted)) {
            ready=true;
            Interstellar.LOGGER.info("Streaming terrain ready: {}; initial capture={} ms",status(),(System.nanoTime()-started)/1e6);
            Interstellar.LOGGER.info("Quad terrain ready: {} quads; vertex payload={} bytes; single retained vertex/node arenas",triangleCount/2,triangleCount/2*192L);
        }
    }
    private boolean loaded(long key) {return world.getChunkManager().isChunkLoaded(ChunkPos.getPackedX(key),ChunkPos.getPackedZ(key));}
    private void window(int x,int z,int range) {
        cameraX=x;cameraZ=z;distance=range;int radius=Math.max(2,range)+1;
        window=new Window(x,z,radius);
        wanted.clear();
        for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++)wanted.add(ChunkPos.toLong(x+dx,z+dz));
        boolean changed=false;
        for(var iterator=entries.entrySet().iterator();iterator.hasNext();) {
            var entry=iterator.next();if(!wanted.contains(entry.getKey())){release(entry.getValue());iterator.remove();changed=true;}
        }
        versions.keySet().retainAll(wanted);queue.retainAll(wanted);
        if(capture!=null && !wanted.contains(capturing)){capture.close();capture=null;}
        var add=new ArrayList<Long>();for(long key:wanted)if(!entries.containsKey(key) && (capture==null || key!=capturing))add.add(key);
        add.sort(Comparator.comparingLong(key->{long dx=ChunkPos.getPackedX(key)-x,dz=ChunkPos.getPackedZ(key)-z;return dx*dx+dz*dz;}));queue.addAll(add);
        if(changed)index();
        Interstellar.LOGGER.info("Streaming window: camera chunk=({}, {}), radius={}, retained={}, queued={}",x,z,radius,entries.size(),queue.size());
    }
    private void publish(long key,WorldMesh mesh) {
        int count=mesh.triangleCount();var old=entries.get(key);
        if(triangleCount-(old==null?0:old.triangles)+count>7_000_000)throw new IllegalStateException("Streaming terrain exceeds seven million triangles");
        Entry next=new Entry(0,0,0,0,0,null,false);
        if(count>0) {
            var data=QuadVertices.pack(mesh.triangleData(),count);var tree=new MeshTree(data,count/2,4);var treeNodes=tree.nodes();
            int tr=(count/2+QuadVertices.PER_ROW-1)/QuadVertices.PER_ROW,nr=(tree.size()+MeshArena.NODES-1)/MeshArena.NODES;
            int t=vertexRows.allocate(tr),n;
            try {n=nodeRows.allocate(nr);}catch(RuntimeException e){vertexRows.release(t,tr);throw e;}
            int base=n*MeshArena.NODES,first=t*QuadVertices.PER_ROW;
            var part=new SceneTree.Part(treeNodes[0],treeNodes[1],treeNodes[2],treeNodes[4],treeNodes[5],treeNodes[6],base);
            for(int i=0;i<tree.size();i++) {int p=i*12;treeNodes[p+3]=treeNodes[p+3]==tree.size()?-1:treeNodes[p+3]+base;treeNodes[p+7]+=first;}
            try {vertices.write(t,data,data.length);nodes.write(n,treeNodes,treeNodes.length);}
            catch(RuntimeException e){vertexRows.release(t,tr);nodeRows.release(n,nr);throw e;}
            next=new Entry(t,tr,n,nr,count,part,mesh.hasMaterials());
        }
        entries.put(key,next);if(old!=null)release(old);triangleCount+=count;if(next.materials)materialChunks++;index();
        if(++published<=3 || published%100==0 || ready && Math.abs(ChunkPos.getPackedX(key)-cameraX)<=1 && Math.abs(ChunkPos.getPackedZ(key)-cameraZ)<=1)
            Interstellar.LOGGER.info("Streaming chunk published: ({}, {}), triangles={}, replacement={}, pending={}",ChunkPos.getPackedX(key),ChunkPos.getPackedZ(key),count,old!=null,queue.size());
    }
    private void index() {
        var parts=new ArrayList<SceneTree.Part>();for(var entry:entries.values())if(entry.part!=null)parts.add(entry.part);
        float[] data=new SceneTree(parts).nodes();nodeCount=data.length/12;
        if(data.length>4*MeshArena.FLOATS)throw new IllegalStateException("Streaming index capacity exceeded");
        nodes.write(0,data,data.length);
        if(nodeCount>0) {
            double r=0;int[] source={centre.getX()-origin.getX(),centre.getY()-origin.getY(),centre.getZ()-origin.getZ()};
            for(int a=0;a<3;a++){double d=Math.max(Math.abs(data[a]-source[a]),Math.abs(data[a+4]-source[a]));r+=d*d;}extent=(float)Math.sqrt(r)+2;
        }
    }
    private void release(Entry entry) {
        if(entry.part==null)return;vertexRows.release(entry.vertexRow,entry.vertexRowCount);nodeRows.release(entry.nodeRow,entry.nodeRows);triangleCount-=entry.triangles;if(entry.materials)materialChunks--;
    }
    @Override public void close() {
        if(active==this)active=null;if(capture!=null)capture.close();vertices.close();nodes.close();entries.clear();incoming.clear();queue.clear();
    }
}
