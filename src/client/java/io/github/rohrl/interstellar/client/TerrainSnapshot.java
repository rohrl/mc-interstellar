package io.github.rohrl.interstellar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.rohrl.interstellar.Interstellar;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;

/** Bounded client-only frozen voxel capture. 0=air, 1=unknown, 2=unsupported, >=3=material. */
final class TerrainSnapshot implements AutoCloseable {
    static final int SIDE=96, TOTAL=SIDE*SIDE*SIDE;
    final ClientWorld world;
    final BlockPos origin;
    private final FloatBuffer cells=MemoryUtil.memAllocFloat(TOTAL);
    private final HashMap<BlockState,Integer> materials=new HashMap<>();
    private final ArrayList<float[]> palette=new ArrayList<>();
    final ArrayList<Integer> occupied=new ArrayList<>();
    int value(int index) {return (int)cells.get(index);}
    private boolean closed;
    private int cursor, opaque, unknown, unsupported;
    int voxelTexture, paletteTexture;
    private final long started=System.nanoTime();
    TerrainSnapshot(ClientWorld world, Vec3d source) {
        this.world=world;
        origin=BlockPos.ofFloored(source).add(-SIDE/2,-SIDE/2,-SIDE/2);
        for(int i=0;i<3;i++) palette.add(new float[72]);
    }
    boolean ready() { return voxelTexture!=0; }
    String status() { return ready()?opaque+" opaque | "+unknown+" unknown | "+unsupported+" unsupported":
            "Capturing nearby blocks: "+(100*cursor/TOTAL)+"%"; }
    boolean contains(Vec3d point) {
        Vec3d p=point.subtract(Vec3d.of(origin));
        return p.x>=1 && p.y>=1 && p.z>=1 && p.x<SIDE-1 && p.y<SIDE-1 && p.z<SIDE-1;
    }
    void advance() {
        if (ready()) return;
        long deadline=System.nanoTime()+3_000_000;
        BlockPos.Mutable pos=new BlockPos.Mutable();
        for(int reads=0;reads<8192 && cursor<TOTAL;reads++,cursor++) {
            int x=cursor%SIDE, z=(cursor/SIDE)%SIDE, y=cursor/(SIDE*SIDE);
            pos.set(origin.getX()+x,origin.getY()+y,origin.getZ()+z);
            int value;
            if (!world.isInBuildLimit(pos)) value=0;
            else if (!world.getChunkManager().isChunkLoaded(pos.getX()>>4,pos.getZ()>>4)) { value=1;unknown++; }
            else {
                var state=world.getBlockState(pos);
                if (state.isAir()) value=0;
                else if (!state.isOpaqueFullCube(world,pos)) { value=2;unsupported++; }
                else {
                    value=materials.computeIfAbsent(state,key->material(key,pos));
                    if(value==2) unsupported++; else opaque++;
                }
            }
            cells.put(cursor,value);
            if(value!=0) occupied.add(cursor);
            if((reads&63)==63 && System.nanoTime()>=deadline) {cursor++;break;}
        }
        if(cursor==TOTAL) upload();
    }
    private int material(BlockState state,BlockPos pos) {
        if(palette.size()>=512) return 2;
        var client=MinecraftClient.getInstance();
        var model=client.getBlockRenderManager().getModel(state);
        float[] data=new float[72];
        for(var face:Direction.values()) {
            var quads=model.getQuads(state,face,Random.create(0));
            if(quads.isEmpty()) return 2;
            BakedQuad quad=quads.getFirst();
            int[] vertex=quad.getVertexData(); int stride=vertex.length/4;
            // Affine UV mapping from the baked face's actual positions; preserves rotations.
            int a=face.getAxis()==Direction.Axis.X?2:0;
            int b=face.getAxis()==Direction.Axis.Y?2:1;
            float s0=Float.intBitsToFloat(vertex[a]),t0=Float.intBitsToFloat(vertex[b]);
            float ds1=Float.intBitsToFloat(vertex[stride+a])-s0,dt1=Float.intBitsToFloat(vertex[stride+b])-t0;
            float ds2=Float.intBitsToFloat(vertex[2*stride+a])-s0,dt2=Float.intBitsToFloat(vertex[2*stride+b])-t0;
            float det=ds1*dt2-ds2*dt1;
            if(Math.abs(det)<.5) return 2; // Restrict the prototype to full, planar cube faces.
            int offset=face.getId()*12;
            for(int uv=0;uv<2;uv++) {
                float v0=Float.intBitsToFloat(vertex[4+uv]);
                float dv1=Float.intBitsToFloat(vertex[stride+4+uv])-v0;
                float dv2=Float.intBitsToFloat(vertex[2*stride+4+uv])-v0;
                float u=(dv1*dt2-dv2*dt1)/det,v=(ds1*dv2-ds2*dv1)/det;
                data[offset+uv*4]=u; data[offset+uv*4+1]=v; data[offset+uv*4+2]=v0-u*s0-v*t0;
            }
            int tint=quad.hasColor()?client.getBlockColors().getColor(state,world,pos,quad.getColorIndex()):0xFFFFFF;
            data[offset+8]=((tint>>16)&255)/255f;data[offset+9]=((tint>>8)&255)/255f;data[offset+10]=(tint&255)/255f;data[offset+11]=1;
        }
        palette.add(data); return palette.size()-1;
    }
    private void upload() {
        int previous=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int[] names={GL11.GL_UNPACK_ALIGNMENT,GL11.GL_UNPACK_ROW_LENGTH,GL11.GL_UNPACK_SKIP_ROWS,GL11.GL_UNPACK_SKIP_PIXELS};
        int[] saved=new int[names.length];
        for(int i=0;i<names.length;i++)saved[i]=GL11.glGetInteger(names[i]);
        int unpackBuffer=GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        Interstellar.LOGGER.info("Terrain upload unpack state: alignment={}, rowLength={}, skipRows={}, skipPixels={}, buffer={}",saved[0],saved[1],saved[2],saved[3],unpackBuffer);
        try {
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,0);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT,1);
            for(int i=1;i<names.length;i++)GL11.glPixelStorei(names[i],0);
            if(GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE)<SIDE*SIDE) throw new IllegalStateException("Terrain texture size unsupported");
            voxelTexture=GL11.glGenTextures(); RenderSystem.bindTexture(voxelTexture);
            nearest();
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL30.GL_R32F,SIDE*SIDE,SIDE,0,GL11.GL_RED,GL11.GL_FLOAT,cells);
            var values=MemoryUtil.memAllocFloat(palette.size()*72);
            try {
            for(float[] row:palette) values.put(row); values.flip();
            paletteTexture=GL11.glGenTextures();RenderSystem.bindTexture(paletteTexture);nearest();
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL30.GL_RGBA32F,18,palette.size(),0,GL11.GL_RGBA,GL11.GL_FLOAT,values);
            } finally {MemoryUtil.memFree(values);}
        } finally {
            for(int i=0;i<names.length;i++)GL11.glPixelStorei(names[i],saved[i]);
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,unpackBuffer);
            RenderSystem.bindTexture(previous);
        }
        Interstellar.LOGGER.info("Terrain snapshot: origin={}, side={}, materials={}, {}, capture wall time={} ms",origin,SIDE,palette.size()-3,status(),(System.nanoTime()-started)/1e6);
    }
    private static void nearest() {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL30.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL30.GL_CLAMP_TO_EDGE);
    }
    @Override public void close() {
        if(closed)return;closed=true;MemoryUtil.memFree(cells);
        if(voxelTexture!=0) RenderSystem.deleteTexture(voxelTexture);
        if(paletteTexture!=0) RenderSystem.deleteTexture(paletteTexture);
        voxelTexture=paletteTexture=0;
    }
}
