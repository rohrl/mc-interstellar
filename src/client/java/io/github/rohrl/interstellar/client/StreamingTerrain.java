package io.github.rohrl.interstellar.client;

import io.github.rohrl.interstellar.Interstellar;
import io.github.rohrl.interstellar.scene.*;
import io.github.rohrl.interstellar.wormhole.WormholePair;
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
    private record Entry(int vertexRow,int vertexRowCount,int nodeRow,int nodeRows,int triangles,SceneTree.Part part,boolean materials,long revision) {}
    private long geometryRevision,chunkRevision;
    private final Map<Long,Entry> entries=new HashMap<>();
    private final TerrainRefreshes refreshes=new TerrainRefreshes();
    private final Map<Long,Integer> incoming=new ConcurrentHashMap<>();
    private final Map<Long,Long> editStarted=new HashMap<>();
    private static final boolean TRACE_EDITS=Boolean.getBoolean("interstellar.traceEdits");
    private static final boolean PREP_PROFILE=Boolean.getBoolean("interstellar.profilePreparation");
    // Opt-out reference switches are for developer A/B measurements only.
    private static final boolean FAIR_PREPARATION=Boolean.parseBoolean(System.getProperty("interstellar.fairPreparation","true"));
    private static final boolean COMPLETE_PREPARATION=Boolean.parseBoolean(System.getProperty("interstellar.completePreparation","true"));
    private long captureNanos,publishNanos,abandoned,modelBlocks,enclosedBlocks;
    private final Set<Long> wanted=new HashSet<>();
    private final Set<Long> localWanted=new HashSet<>();
    private WormholePair.Layout layout;
    private final LinkedHashSet<Long> queue=new LinkedHashSet<>();
    private WorldMesh capture;
    private long capturing,captureVersion,published;
    private boolean captureEdit;
    private int cameraX=Integer.MIN_VALUE,cameraZ,distance,triangleCount,materialChunks;
    private record Window(int x,int z,int radius) {}
    private volatile Window window;
    int nodeCount;
    float extent=512;
    private boolean ready,localPrepared;
    private boolean profileCaughtUp;
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
        changed(x,z,TerrainRefreshes.CONTENT);
    }
    public static void lightingDirty(int x,int z) {changed(x,z,TerrainRefreshes.LIGHT);}
    public static void edited(net.minecraft.world.BlockView world,BlockPos pos) {
        var cache=active;if(cache==null||cache.world!=world)return;
        for(long key:TerrainRefreshes.affected(pos.getX(),pos.getZ()))changed(ChunkPos.getPackedX(key),ChunkPos.getPackedZ(key),TerrainRefreshes.CONTENT|TerrainRefreshes.EDIT);
        if(TRACE_EDITS)Interstellar.LOGGER.info("Terrain edit received: pos={}, queued={}",pos,cache.queue.size());
    }
    private static void changed(int x,int z,int kind) {
        var cache=active;if(cache==null)return;var w=cache.window;
        if(w!=null && (Math.abs((long)x-w.x)<=w.radius+1 && Math.abs((long)z-w.z)<=w.radius+1
                || WormholePair.active(cache.world)&&WormholePair.contains(cache.world,x,z)))cache.incoming.merge(ChunkPos.toLong(x,z),kind,(a,b)->a|b);
    }
    StreamingTerrain(ClientWorld world,BlockPos origin,BlockPos centre) {
        this.world=world;this.origin=origin;this.centre=centre;
        if(Boolean.getBoolean("interstellar.auditArenaCopy"))MeshArena.validateCopies();
        vertices=new MeshArena(10924,false,QuadVertices.WIDTH);
        try {nodes=new MeshArena(4096,true);}catch(RuntimeException e){vertices.close();throw e;}
        active=this;
    }
    boolean ready() {return ready;}
    boolean localReady() {return localPrepared;}
    // Unlike localPrepared (latched for seamless streaming), loading needs the current
    // server-visible region, including packets which have not arrived yet.
    private net.minecraft.server.network.ChunkFilter localFilter() {
        var client=MinecraftClient.getInstance();
        return net.minecraft.server.network.ChunkFilter.cylindrical(client.player.getChunkPos(),client.options.getClampedViewDistance());
    }
    boolean localCaughtUp() {
        if(localWanted.isEmpty())return false;
        var filter=localFilter();
        for(long key:localWanted)if(filter.isWithinDistance(ChunkPos.getPackedX(key),ChunkPos.getPackedZ(key)) || loaded(key)) {
            var entry=entries.get(key);if(entry==null || entry.revision==0)return false;
        }
        return true;
    }
    int localLoadingPercent() {
        int total=0,captured=0;var filter=localFilter();
        for(long key:localWanted)if(filter.isWithinDistance(ChunkPos.getPackedX(key),ChunkPos.getPackedZ(key)) || loaded(key)) {
            total++;var entry=entries.get(key);if(entry!=null && entry.revision!=0)captured++;
        }
        return total==0?0:captured*100/total;
    }
    boolean complete() {
        if(!WormholePair.active(world))return ready;
        // A previously unloaded local slot can become a remote destination. Its
        // empty placeholder is not captured geometry, even after packets arrive.
        for(var chunk:WormholePair.chunks(world)) {
            var entry=entries.get(chunk.toLong());if(entry==null || entry.revision==0)return false;
        }
        return true;
    }
    double openingProgress() {
        var chunks=WormholePair.chunks(world);if(chunks.isEmpty())return 0;
        int captured=0,total=chunks.size();
        for(var chunk:chunks) {var entry=entries.get(chunk.toLong());if(entry!=null && entry.revision!=0)captured++;}
        // First activation needs the loaded local world too. Do not report 95% while
        // that work is still missing. Later replacements retain the prepared local cache.
        if(COMPLETE_PREPARATION && !localPrepared)for(long key:localWanted) {
            if(!loaded(key) || WormholePair.contains(world,ChunkPos.getPackedX(key),ChunkPos.getPackedZ(key)))continue;
            total++;var entry=entries.get(key);if(entry!=null && entry.revision!=0)captured++;
        }
        return captured/(double)total;
    }
    // Diagnostic export reads only occupied rows; normal capture retains no extra CPU geometry.
    int[][] replaySpans() {return entries.entrySet().stream().sorted(Map.Entry.comparingByKey())
        .filter(e->e.getValue().triangles>0).map(e->new int[]{e.getValue().vertexRow,e.getValue().triangles/2}).toArray(int[][]::new);}
    WorldRenderBackend.Terrain backendTerrain() {
        return new WorldRenderBackend.Terrain() {
            public long revision(){return geometryRevision;}
            public int rows(){return vertices.rows();}
            public List<WorldRenderBackend.Chunk> chunks(){return entries.entrySet().stream().filter(e->e.getValue().triangles>0).sorted(Map.Entry.comparingByKey())
                .map(e->new WorldRenderBackend.Chunk(e.getKey(),e.getValue().revision,e.getValue().vertexRow,e.getValue().triangles/2)).toList();}
            public java.nio.ByteBuffer read(WorldRenderBackend.Chunk chunk){
                try(var state=new TerrainReplay.PackState()){return TerrainReplay.readTexture(vertices.texture,0,chunk.row(),QuadVertices.WIDTH,(chunk.quads()+QuadVertices.PER_ROW-1)/QuadVertices.PER_ROW,true);}
            }
        };
    }
    boolean hasMaterials() {return materialChunks>0;}
    int loadingPercent() {
        int captured=0,total=0;
        for(long key:wanted)if(loaded(key) || WormholePair.contains(world,ChunkPos.getPackedX(key),ChunkPos.getPackedZ(key))) {
            total++;var entry=entries.get(key);if(entry!=null && entry.revision!=0)captured++;
        }
        return total==0?0:captured*100/total;
    }
    int triangleTexture() {return vertices.texture;}
    int nodeTexture() {return nodes.texture;}
    int compactNodeTexture() {return nodes.texture;}
    String status() {return ready?"Streaming terrain | "+triangleCount+" triangles | "+queue.size()+" queued chunks":"Loading terrain: "+entries.size()+"/"+wanted.size()+" chunks";}
    void advance() {
        var client=MinecraftClient.getInstance();
        int range=client.options.getViewDistance().getValue();
        if(range>16)throw new IllegalStateException("Native mesh supports render distance up to16");
        var position=BlockPos.ofFloored(client.gameRenderer.getCamera().getPos());
        int x=position.getX()>>4,z=position.getZ()>>4;
        if(x!=cameraX || z!=cameraZ || range!=distance || layout!=WormholePair.layout(world))window(x,z,range);
        for(var event:incoming.entrySet()) {
            long key=event.getKey();int kind=event.getValue();
            // A concurrent light update must not be erased while draining events.
            if(!incoming.remove(key,kind))continue;
            if(wanted.contains(key)) {
                refreshes.changed(key,kind);queue.add(key);
                if((kind&TerrainRefreshes.EDIT)!=0 && TRACE_EDITS)editStarted.putIfAbsent(key,System.nanoTime());
                // Finish a prompt lighting follow-up when it arrives during an edit rebuild.
                if(capture!=null && capturing==key && captureEdit)refreshes.retry(key,true);
            }
        }
        if(capture!=null && captureVersion!=refreshes.version(capturing)) {
            abandoned++;
            refreshes.retry(capturing,captureEdit);capture.close();capture=null;
        }
        boolean indexChanged=false;
        // Unloaded geometry disappears promptly, even if other chunks are waiting to rebuild.
        for(long key:List.copyOf(queue))if(!loaded(key)) {
            if(WormholePair.active(world)&&WormholePair.contains(world,ChunkPos.getPackedX(key),ChunkPos.getPackedZ(key)))continue;
            var old=entries.put(key,new Entry(0,0,0,0,0,null,false,0));if(old!=null){release(old);indexChanged|=old.part!=null;}
            queue.remove(key);
            refreshes.remove(key);editStarted.remove(key);
            if(capture!=null && capturing==key){capture.close();capture=null;}
        }
        if(indexChanged)index();
        if(capture==null) {Long edit=refreshes.nextEdit(key->loaded(key) && (!FAIR_PREPARATION || Math.abs(ChunkPos.getPackedX(key)-cameraX)<=1 && Math.abs(ChunkPos.getPackedZ(key)-cameraZ)<=1));if(edit!=null)beginCapture(edit);}
        if(capture==null && client.currentScreen instanceof WorldPreparationScreen) {
            // World entry waits only for the local view; remote portals open later.
            for(long key:queue)if(localWanted.contains(key) && loaded(key)) {
                var entry=entries.get(key);
                if(entry==null || entry.revision==0){beginCapture(key);break;}
            }
        }
        if(capture==null && !queue.isEmpty()) {
            // Finish the fixed destination set before ordinary camera-window
            // churn. Moving around must not keep missing portal chunks at the tail.
            for(var chunk:WormholePair.chunks(world)) {
                long key=chunk.toLong();var entry=entries.get(key);
                if((entry==null || entry.revision==0) && loaded(key)) {beginCapture(key);break;}
            }
        }
        if(capture==null && !queue.isEmpty()) {
            if(FAIR_PREPARATION)for(long key:queue) {
                var entry=entries.get(key);
                if((entry==null || entry.revision==0) && loaded(key)){beginCapture(key);break;}
            }
        }
        if(capture==null && FAIR_PREPARATION) {Long edit=refreshes.nextEdit(this::loaded);if(edit!=null)beginCapture(edit);}
        if(capture==null && !queue.isEmpty()) {
            for(var iterator=queue.iterator();iterator.hasNext();) {
                long key=iterator.next();if(!loaded(key))continue;
                iterator.remove();beginCapture(key);break;
            }
        }
        if(capture!=null) {
            long profileStart=PREP_PROFILE?System.nanoTime():0;
            capture.advance();
            if(PREP_PROFILE)captureNanos+=System.nanoTime()-profileStart;
            if(capture.ready()) {
                if(wanted.contains(capturing) && loaded(capturing) && captureVersion==refreshes.version(capturing))publish(capturing,capture);
                else if(wanted.contains(capturing)){queue.add(capturing);refreshes.retry(capturing,captureEdit);}
                capture.close();capture=null;
            }
        }
        if(!localPrepared && !localWanted.isEmpty() && captured(localWanted) && (!COMPLETE_PREPARATION || localCaughtUp()))localPrepared=true;
        if(!ready && localPrepared && captured(wanted)) {
            ready=true;
            Interstellar.LOGGER.info("Streaming terrain ready: {}; initial capture={} ms",status(),(System.nanoTime()-started)/1e6);
            Interstellar.LOGGER.info("Quad terrain ready: {} quads; vertex payload={} bytes; single retained vertex/node arenas",triangleCount/2,triangleCount/2*192L);
        }
    }
    private boolean captured(Set<Long> required) {
        for(long key:required) {
            var entry=entries.get(key);if(entry==null || COMPLETE_PREPARATION && entry.revision==0 && loaded(key))return false;
        }
        return true;
    }
    private void beginCapture(long key) {
        capturing=key;queue.remove(key);captureVersion=refreshes.version(key);captureEdit=refreshes.begin(key);
        capture=WorldMesh.chunk(world,origin,centre,ChunkPos.getPackedX(key),ChunkPos.getPackedZ(key));
    }
    private boolean loaded(long key) {return world.getChunkManager().isChunkLoaded(ChunkPos.getPackedX(key),ChunkPos.getPackedZ(key));}
    private void window(int x,int z,int range) {
        layout=WormholePair.layout(world);
        cameraX=x;cameraZ=z;distance=range;int radius=Math.max(2,range)+1;
        window=new Window(x,z,radius);
        wanted.clear();
        for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++)wanted.add(ChunkPos.toLong(x+dx,z+dz));
        localWanted.clear();localWanted.addAll(wanted);
        if(WormholePair.active(world))for(var pos:WormholePair.chunks(world))wanted.add(pos.toLong());
        boolean changed=false;
        for(var iterator=entries.entrySet().iterator();iterator.hasNext();) {
            var entry=iterator.next();if(!wanted.contains(entry.getKey())){release(entry.getValue());iterator.remove();changed=true;}
        }
        refreshes.retain(wanted);editStarted.keySet().retainAll(wanted);queue.retainAll(wanted);
        if(capture!=null && !wanted.contains(capturing)){capture.close();capture=null;}
        var add=new ArrayList<Long>();for(long key:wanted)if(!entries.containsKey(key) && (capture==null || key!=capturing))add.add(key);
        add.sort(Comparator.comparingLong(key->{long dx=ChunkPos.getPackedX(key)-x,dz=ChunkPos.getPackedZ(key)-z;return dx*dx+dz*dz;}));queue.addAll(add);
        if(changed)index();
        Interstellar.LOGGER.info("Streaming window: camera chunk=({}, {}), radius={}, retained={}, queued={}",x,z,radius,entries.size(),queue.size());
    }
    private void publish(long key,WorldMesh mesh) {
        long profileStart=PREP_PROFILE?System.nanoTime():0;
        int count=mesh.triangleCount();var old=entries.get(key);
        Entry next=new Entry(0,0,0,0,0,null,false,++chunkRevision);
        if(count>0) {
            var data=QuadVertices.pack(mesh.triangleData(),count);var tree=new MeshTree(data,count/2,4);var treeNodes=tree.nodes();
            int tr=(count/2+QuadVertices.PER_ROW-1)/QuadVertices.PER_ROW,nr=(tree.size()+MeshArena.NODES-1)/MeshArena.NODES;
            int t=allocate(vertexRows,vertices,0,24576,tr,old==null?-1:old.vertexRow,old==null?0:old.vertexRowCount),n;
            try {n=allocate(nodeRows,nodes,4,6140,nr,old==null?-1:old.nodeRow,old==null?0:old.nodeRows);}
            catch(RuntimeException e){if(old==null || t!=old.vertexRow)vertexRows.release(t,tr);throw e;}
            int base=n*MeshArena.NODES,first=t*QuadVertices.PER_ROW;
            var part=new SceneTree.Part(treeNodes[0],treeNodes[1],treeNodes[2],treeNodes[4],treeNodes[5],treeNodes[6],base);
            for(int i=0;i<tree.size();i++) {int p=i*12;treeNodes[p+3]=treeNodes[p+3]==tree.size()?-1:treeNodes[p+3]+base;treeNodes[p+7]+=first;}
            try {vertices.write(t,data,data.length);nodes.write(n,treeNodes,treeNodes.length);}
            catch(RuntimeException e){if(old==null || t!=old.vertexRow)vertexRows.release(t,tr);if(old==null || n!=old.nodeRow)nodeRows.release(n,nr);throw e;}
            next=new Entry(t,tr,n,nr,count,part,mesh.hasMaterials(),chunkRevision);
        }
        entries.put(key,next);
        if(old!=null && old.part!=null) {
            if(next.part==null || old.vertexRow!=next.vertexRow)vertexRows.release(old.vertexRow,old.vertexRowCount);
            if(next.part==null || old.nodeRow!=next.nodeRow)nodeRows.release(old.nodeRow,old.nodeRows);
            triangleCount-=old.triangles;if(old.materials)materialChunks--;
        }
        triangleCount+=count;if(next.materials)materialChunks++;index();
        if(PREP_PROFILE) {
            publishNanos+=System.nanoTime()-profileStart;
            modelBlocks+=mesh.modelBlocks;enclosedBlocks+=mesh.enclosedBlocks;
            if(published%100==0)Interstellar.LOGGER.info("Preparation profile: unique={}, published={}, abandoned={}, triangles={}, captureMs={}, publishMs={}, vertexFree={}, vertexLargest={}, nodeFree={}, queued={}, wallMs={}",entries.size(),published+1,abandoned,triangleCount,captureNanos/1e6,publishNanos/1e6,vertexRows.freeRows(),vertexRows.largestFree(),nodeRows.freeRows(),queue.size(),(System.nanoTime()-started)/1e6);
            if(published%100==0)Interstellar.LOGGER.info("Preparation model work: blocks={}, enclosed={}, audit={}",modelBlocks,enclosedBlocks,Boolean.getBoolean("interstellar.auditEnclosed"));
            int missing=0,loadedCount=0;
            for(long expected:wanted)if(loaded(expected)){loadedCount++;var entry=entries.get(expected);if(entry==null || entry.revision==0)missing++;}
            if(missing==0 && !profileCaughtUp)Interstellar.LOGGER.info("Preparation caught up: loadedChunks={}, triangles={}, published={}, wallMs={}",loadedCount,triangleCount,published+1,(System.nanoTime()-started)/1e6);
            profileCaughtUp=missing==0;
        }
        Long edited=editStarted.remove(key);
        if(edited!=null)Interstellar.LOGGER.info("Terrain edit published: chunk=({}, {}), latencyMs={}, revision={}, queued={}",ChunkPos.getPackedX(key),ChunkPos.getPackedZ(key),(System.nanoTime()-edited)/1e6,next.revision,queue.size());
        if(++published<=3 || published%100==0 || ready && Math.abs(ChunkPos.getPackedX(key)-cameraX)<=1 && Math.abs(ChunkPos.getPackedZ(key)-cameraZ)<=1)
            Interstellar.LOGGER.info("Streaming chunk published: ({}, {}), triangles={}, replacement={}, pending={}",ChunkPos.getPackedX(key),ChunkPos.getPackedZ(key),count,old!=null,queue.size());
    }
    private int allocate(RowArena pool,MeshArena storage,int first,int limit,int rows,int oldRow,int oldRows) {
        if(oldRows>0 && (rows<=oldRows || pool.extend(oldRow,oldRows,rows))) {
            if(rows<oldRows)pool.release(oldRow+rows,oldRows-rows);
            return oldRow;
        }
        if(pool.largestFree()<rows) {
            int max=Math.min(limit,org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_MAX_TEXTURE_SIZE)-first);
            int capacity=Math.min(max,Math.max(pool.capacity()+rows,(pool.capacity()*3+1)/2));
            if(capacity>pool.capacity()) {
                long started=System.nanoTime();int before=pool.capacity();
                storage.grow(first+capacity);pool.grow(capacity);
                Interstellar.LOGGER.info("Terrain storage grown: {} rows {} -> {}, copyMs={}",first==0?"vertices":"nodes",before,capacity,(System.nanoTime()-started)/1e6);
            }
        }
        return pool.allocate(rows);
    }
    private void index() {
        geometryRevision++;
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
        if(active==this)active=null;if(capture!=null)capture.close();vertices.close();nodes.close();entries.clear();incoming.clear();queue.clear();refreshes.clear();editStarted.clear();
    }
}
