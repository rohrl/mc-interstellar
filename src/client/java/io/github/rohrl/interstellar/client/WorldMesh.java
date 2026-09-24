package io.github.rohrl.interstellar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.rohrl.interstellar.Interstellar;
import io.github.rohrl.interstellar.scene.MeshTree;
import io.github.rohrl.interstellar.scene.MovingMeshTrees;
import net.minecraft.block.BlockRenderType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;
import java.util.Arrays;

/** Frozen, client-only native model capture. No filled-column approximation or generated chunks. */
final class WorldMesh implements VertexConsumer,AutoCloseable {
    private ReusableMeshTexture movingTriangles,movingNodes;
    private ReusableMeshTexture compactNodes;
    private static final int MAX_TRIANGLES=7_000_000;
    private final ClientWorld world;
    private final BlockPos origin;
    private final int minChunkX,minChunkZ,chunks,sections,total,viewDistance;
    private final MatrixStack matrices=new MatrixStack();
    private final Random random=Random.create(0);
    private final float[] quadData=new float[48];
    private final Vector3f position=new Vector3f();
    private float[] triangles=new float[36*8192];
    private int cursor,count,missingSections,omittedBlocks;
    private boolean materials;
    private float terrainMaterial;
    private final FluidMesh fluids=new FluidMesh(this);
    private final long started=System.nanoTime();
    int triangleTexture,nodeTexture,nodeCount,compactNodeTexture;
    int actorNodeCount,cloudNodeCount;
    private boolean splitMoving;
    EntityMesh entities;
    final CloudMesh clouds=new CloudMesh();
    private final BlockPos centre;
    private final boolean terrainOnly;
    private final boolean singleChunk;
    private boolean prepared;
    private StreamingTerrain streaming;
    private boolean dynamic;
    private long updates;
    float extent=512;
    WorldMesh(ClientWorld world,BlockPos origin,BlockPos centre) {
        this(world,origin,centre,false);
    }
    WorldMesh(ClientWorld world,BlockPos origin,BlockPos centre,boolean terrainOnly) {
        this(world,origin,centre,terrainOnly,false,0,0);
    }
    static WorldMesh chunk(ClientWorld world,BlockPos origin,BlockPos centre,int x,int z) {
        return new WorldMesh(world,origin,centre,true,true,x,z);
    }
    private WorldMesh(ClientWorld world,BlockPos origin,BlockPos centre,boolean terrainOnly,boolean singleChunk,int chunkX,int chunkZ) {
        this.world=world;this.origin=origin;this.centre=centre;
        this.terrainOnly=terrainOnly;this.singleChunk=singleChunk;
        var client=MinecraftClient.getInstance();
        var camera=BlockPos.ofFloored(client.gameRenderer.getCamera().getPos());
        viewDistance=client.options.getViewDistance().getValue();
        // Include every direction for bent rays, plus one chunk beyond the native fog distance.
        int radius=Math.max(2,Math.min(16,viewDistance))+1;chunks=singleChunk?1:radius*2+1;
        minChunkX=singleChunk?chunkX:(camera.getX()>>4)-radius;minChunkZ=singleChunk?chunkZ:(camera.getZ()>>4)-radius;
        sections=world.countVerticalSections();total=chunks*chunks*sections*4096;
        if(terrainOnly && !singleChunk){streaming=new StreamingTerrain(world,origin,centre);triangles=null;}
    }
    float[] triangleData() {return triangles;}
    int triangleCount() {return count;}
    float sourceShift(net.minecraft.util.math.Vec3d source) {return (float)source.distanceTo(net.minecraft.util.math.Vec3d.of(centre));}
    WorldMesh movingScene() {
        var moving=new WorldMesh(world,origin,centre,false);moving.dynamic=true;
        moving.entities=new EntityMesh(moving);return moving;
    }
    boolean dynamic() {return dynamic;}
    boolean hasMaterials() {return streaming!=null?streaming.hasMaterials():materials;}
    void updateMoving() {
        if(!dynamic)throw new IllegalStateException("Not a moving scene");
        count=0;materials=false;
        var camera=BlockPos.ofFloored(MinecraftClient.getInstance().gameRenderer.getCamera().getPos());
        int radius=Math.max(2,Math.min(16,MinecraftClient.getInstance().options.getViewDistance().getValue()))+1;
        long profileStart=TerrainProfile.cpuStart();
        if(profileMovingContents!=2)entities.capture(origin,(camera.getX()>>4)-radius,(camera.getZ()>>4)-radius,2*radius+1);
        TerrainProfile.cpuEnd(0,profileStart);profileStart=TerrainProfile.cpuStart();
        if(profileMovingContents!=1)clouds.capture(this,origin);
        TerrainProfile.cpuEnd(1,profileStart);
        finishTree();
        if(++updates==1 || updates==300 || updates==600 || updates%3600==0)
            Interstellar.LOGGER.info("Live mesh update {}: {} triangles, {}; geometry fingerprint={}",updates,count,entities.status(),Arrays.hashCode(Arrays.copyOf(triangles,count*36)));
    }
    // Developer-only scene ablation, never selected by normal rendering controls.
    int profileMovingContents;
    boolean ready() {return streaming!=null?streaming.ready():singleChunk?prepared:nodeTexture!=0;}
    boolean streamed() {return streaming!=null;}
    boolean quadStorage() {return streaming!=null;}
    String viewStatus() {return ready()?"World view ready":streaming!=null?"Preparing world view: "+streaming.loadingPercent()+"%":"Preparing world view...";}
    String status() {return streaming!=null?streaming.status():ready()?"Native mesh: "+count+" triangles | "+missingSections+" missing sections"+(entities==null?"": " | "+entities.status()):
            "Capturing native mesh: "+(100L*cursor/total)+"%";}
    void advance() {
        if(streaming!=null) {
            streaming.advance();triangleTexture=streaming.triangleTexture();nodeTexture=streaming.nodeTexture();compactNodeTexture=streaming.compactNodeTexture();nodeCount=streaming.nodeCount;extent=streaming.extent;return;
        }
        if(ready())return;
        // Fail inside the preview's guarded capture loop, not its key handler.
        if(viewDistance>16)throw new IllegalStateException("Native mesh reference supports render distance up to 16 chunks; lower it and reopen");
        var manager=MinecraftClient.getInstance().getBlockRenderManager();
        var pos=new BlockPos.Mutable();
        long deadline=System.nanoTime()+5_000_000;
        net.minecraft.client.render.block.BlockModelRenderer.enableBrightnessCache();
        try {
            while(cursor<total && System.nanoTime()<deadline) {
                int section=cursor/4096,block=cursor%4096,chunk=section/sections;
                int cx=minChunkX+chunk%chunks,cz=minChunkZ+chunk/chunks,sy=section%sections;
                if(!world.getChunkManager().isChunkLoaded(cx,cz)) {missingSections++;cursor=(section+1)*4096;continue;}
                var terrain=world.getChunk(cx,cz);
                if(terrain.getSection(sy).isEmpty()) {cursor=(section+1)*4096;continue;}
                pos.set(cx*16+(block&15),world.getBottomY()+sy*16+(block>>8),cz*16+((block>>4)&15));
                cursor++;
                var state=world.getBlockState(pos);
                if(state.isAir())continue;
                if(!state.getFluidState().isEmpty()) {
                    fluids.begin(pos,origin,state.getFluidState().isIn(net.minecraft.registry.tag.FluidTags.WATER));
                    manager.renderFluid(pos,world,fluids,state,state.getFluidState());fluids.finish();
                }
                if(state.getRenderType()!=BlockRenderType.MODEL)continue;
                terrainMaterial=state.getBlock() instanceof io.github.rohrl.interstellar.source.MassBlock?-4:RenderLayers.getBlockLayer(state)==RenderLayer.getTranslucent()?-3:0;
                if(terrainMaterial==-3)materials=true;
                matrices.push();
                try {
                    matrices.translate(pos.getX()-origin.getX(),pos.getY()-origin.getY(),pos.getZ()-origin.getZ());
                    manager.getModelRenderer().render(world,manager.getModel(state),state,pos,matrices,this,true,random,state.getRenderingSeed(pos),OverlayTexture.DEFAULT_UV);
                } finally {matrices.pop();}
            }
        } finally {net.minecraft.client.render.block.BlockModelRenderer.disableBrightnessCache();}
        if(cursor==total) {
            if(singleChunk){prepared=true;return;}
            if(!terrainOnly) {
                entities=new EntityMesh(this);entities.capture(origin,minChunkX,minChunkZ,chunks);
                clouds.capture(this,origin);
            }
            finishTree();triangles=null;
            Interstellar.LOGGER.info("World mesh ready: {}; {} omitted blocks; camera coverage {}x{} chunks=({}, {})..({}, {}), full build height; capture/build/upload={} ms",status(),omittedBlocks,chunks,chunks,minChunkX,minChunkZ,minChunkX+chunks-1,minChunkZ+chunks-1,(System.nanoTime()-started)/1e6);
        }
    }
    private void finishTree() {
        long profileStart=dynamic?TerrainProfile.cpuStart():0;
        float[] nodes;
        if(dynamic && splitMoving) {
            var trees=MovingMeshTrees.build(triangles,count);
            // Keep capture capacity: rebuilding a different layout must not grow the buffer.
            System.arraycopy(trees.triangles(),0,triangles,0,count*36);
            nodes=trees.nodes();actorNodeCount=trees.actorNodes();cloudNodeCount=trees.cloudNodes();
            nodeCount=actorNodeCount+cloudNodeCount;
        } else {
            var tree=new MeshTree(triangles,count);nodeCount=tree.size();nodes=tree.nodes();
            actorNodeCount=nodeCount;cloudNodeCount=0;
        }
        if(nodes.length>0) {
            double radiusSquared=0;
            int[] source={centre.getX()-origin.getX(),centre.getY()-origin.getY(),centre.getZ()-origin.getZ()};
            for(int a=0;a<3;a++) {
                float low=nodes[a],high=nodes[a+4];
                if(splitMoving && actorNodeCount>0 && cloudNodeCount>0) {
                    low=Math.min(low,nodes[actorNodeCount*12+a]);high=Math.max(high,nodes[actorNodeCount*12+a+4]);
                }
                double distance=Math.max(Math.abs(low-source[a]),Math.abs(high-source[a]));radiusSquared+=distance*distance;
            }
            extent=(float)Math.sqrt(radiusSquared)+2; // Include fractional source-centre rounding.
        }
        if(dynamic)TerrainProfile.cpuEnd(2,profileStart);
        profileStart=dynamic?TerrainProfile.cpuStart():0;
        upload(nodes);
        if(dynamic)TerrainProfile.cpuEnd(3,profileStart);
    }
    void movingLayout(boolean separate) {
        if(!dynamic || splitMoving==separate)return;
        splitMoving=separate;finishTree();
    }
    @Override public void quad(MatrixStack.Entry entry,BakedQuad quad,float[] brightness,float red,float green,float blue,float alpha,int[] light,int overlay,boolean useQuadColor) {
        int[] vertices=quad.getVertexData();int stride=vertices.length/4;
        for(int i=0;i<4;i++) {
            int src=i*stride,dst=i*12;
            position.set(Float.intBitsToFloat(vertices[src]),Float.intBitsToFloat(vertices[src+1]),Float.intBitsToFloat(vertices[src+2]));
            entry.getPositionMatrix().transformPosition(position);
            quadData[dst]=position.x;quadData[dst+1]=position.y;quadData[dst+2]=position.z;
            quadData[dst+3]=terrainMaterial;
            quadData[dst+4]=Float.intBitsToFloat(vertices[src+4]);quadData[dst+5]=Float.intBitsToFloat(vertices[src+5]);
            // Terrain's native shader filters UV2/256; entity shaders use discrete texel centres.
            quadData[dst+6]=(light[i]&65535)/256f;quadData[dst+7]=(light[i]>>>16)/256f;
            int colour=vertices[src+3];
            quadData[dst+8]=red*brightness[i]*(useQuadColor?(colour&255)/255f:1);
            quadData[dst+9]=green*brightness[i]*(useQuadColor?((colour>>>8)&255)/255f:1);
            quadData[dst+10]=blue*brightness[i]*(useQuadColor?((colour>>>16)&255)/255f:1);
            quadData[dst+11]=alpha;
        }
        add(0,1,2);add(2,3,0);
    }
    private void add(int a,int b,int c) {
        if(dynamic && count>=200_000)throw new IllegalStateException("Moving scene exceeds 200,000 triangles");
        if(count==MAX_TRIANGLES)throw new IllegalStateException("Native mesh exceeds seven million triangles; lower render distance and reopen (no silent truncation)");
        if((count+1)*36>triangles.length)triangles=Arrays.copyOf(triangles,Math.min(MAX_TRIANGLES*36,triangles.length*2));
        int dst=count++*36;
        System.arraycopy(quadData,a*12,triangles,dst,12);System.arraycopy(quadData,b*12,triangles,dst+12,12);System.arraycopy(quadData,c*12,triangles,dst+24,12);
    }
    void entityQuad(float[] data,boolean twoSided) {
        System.arraycopy(data,0,quadData,0,48);
        if(twoSided)for(int v=0;v<4;v++){int p=v*12+3;quadData[p]+=Math.signum(quadData[p]);}
        if(data[3]==-3 || Math.abs(data[3])%32>=7)materials=true;
        add(0,1,2);add(2,3,0);
    }
    void entityTriangle(float[] a,float[] b,float[] c) {
        System.arraycopy(a,0,quadData,0,12);System.arraycopy(b,0,quadData,12,12);System.arraycopy(c,0,quadData,24,12);
        for(int v=0;v<3;v++)quadData[v*12+3]+=Math.signum(quadData[v*12+3]);
        add(0,1,2);
    }
    private void upload(float[] nodes) {
        int previous=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D),pbo=GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        int[] names={GL11.GL_UNPACK_ALIGNMENT,GL11.GL_UNPACK_ROW_LENGTH,GL11.GL_UNPACK_SKIP_ROWS,GL11.GL_UNPACK_SKIP_PIXELS};
        int[] saved=new int[names.length];
        for(int i=0;i<names.length;i++) {saved[i]=GL11.glGetInteger(names[i]);GL11.glPixelStorei(names[i],i==0?4:0);}
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,0);
        int newTriangles=0,newNodes=0;
        try {
            if(TerrainScreen.hasCompactShader()) {
                if(compactNodes==null)compactNodes=new ReusableMeshTexture(true);
                compactNodes.upload(nodes,nodes.length);compactNodeTexture=compactNodes.id;
            }
            if(dynamic) {
                if(movingTriangles==null)movingTriangles=new ReusableMeshTexture();
                if(movingNodes==null)movingNodes=new ReusableMeshTexture();
                movingTriangles.upload(triangles,count*36);movingNodes.upload(nodes,nodes.length);
                triangleTexture=movingTriangles.id;nodeTexture=movingNodes.id;
                if(updates==0 || updates==599)Interstellar.LOGGER.info("Moving mesh texture reuse: updates={}, triangle allocations={}, node allocations={}",updates,movingTriangles.allocations,movingNodes.allocations);
                return;
            }
            newTriangles=texture(triangles,count*36);newNodes=texture(nodes,nodes.length);
            if(triangleTexture!=0)RenderSystem.deleteTexture(triangleTexture);
            if(nodeTexture!=0)RenderSystem.deleteTexture(nodeTexture);
            triangleTexture=newTriangles;nodeTexture=newNodes;newTriangles=newNodes=0;
        }
        finally {
            if(newTriangles!=0)RenderSystem.deleteTexture(newTriangles);
            if(newNodes!=0)RenderSystem.deleteTexture(newNodes);
            for(int i=0;i<names.length;i++)GL11.glPixelStorei(names[i],saved[i]);
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,pbo);RenderSystem.bindTexture(previous);
        }
    }
    private static int texture(float[] data,int length) {
        int height=Math.max(1,(length+16383)/16384);
        if(height>GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE))throw new IllegalStateException("Native mesh exceeds GPU texture dimensions");
        var buffer=MemoryUtil.memCallocFloat(height*16384);
        int id=GL11.glGenTextures();
        try {
            buffer.put(data,0,length).position(0);RenderSystem.bindTexture(id);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL30.GL_RGBA32F,4096,height,0,GL11.GL_RGBA,GL11.GL_FLOAT,buffer);
            return id;
        } catch(RuntimeException e) {RenderSystem.deleteTexture(id);throw e;}
        finally {MemoryUtil.memFree(buffer);}
    }
    @Override public void close() {triangles=null;if(entities!=null) {entities.close();entities=null;}if(compactNodes!=null){compactNodes.close();compactNodes=null;}if(movingTriangles!=null){movingTriangles.close();movingTriangles=null;triangleTexture=0;}if(movingNodes!=null){movingNodes.close();movingNodes=null;nodeTexture=0;}if(streaming!=null){streaming.close();streaming=null;}else {if(triangleTexture!=0)RenderSystem.deleteTexture(triangleTexture);if(nodeTexture!=0)RenderSystem.deleteTexture(nodeTexture);}triangleTexture=nodeTexture=compactNodeTexture=0;}
    @Override public VertexConsumer vertex(float x,float y,float z) {throw new IllegalStateException("Expected native quad");}
    @Override public VertexConsumer color(int r,int g,int b,int a) {return this;}
    @Override public VertexConsumer texture(float u,float v) {return this;}
    @Override public VertexConsumer overlay(int u,int v) {return this;}
    @Override public VertexConsumer light(int u,int v) {return this;}
    @Override public VertexConsumer normal(float x,float y,float z) {return this;}
}
