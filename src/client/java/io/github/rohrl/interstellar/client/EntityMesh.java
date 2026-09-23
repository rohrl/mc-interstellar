package io.github.rohrl.interstellar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.rohrl.interstellar.Interstellar;
import io.github.rohrl.interstellar.mixin.client.*;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;
import java.nio.ByteBuffer;
import java.util.*;

/** Native model output. Supported opaque/cutout layers share terrain's nearest-hit tree. */
final class EntityMesh implements VertexConsumerProvider,AutoCloseable {
    private static final int SIZE=2048,MAX_VERTICES=1_200_000;
    private static Vector3f light0=new Vector3f(),light1=new Vector3f();
    private final WorldMesh mesh;
    private final Map<RenderLayer,Collector> collectors=new LinkedHashMap<>();
    private final Map<Identifier,Tile> tiles=new HashMap<>();
    private final Set<String> unsupported=new TreeSet<>();
    private final ByteBuffer pixels=MemoryUtil.memCalloc(SIZE*SIZE*4);
    private int x,y,rowHeight,vertices,entities,blockEntities,omittedEntities;
    private int actorId;
    private final List<BlockEntity> blockEntityList=new ArrayList<>();
    private long blockEntityTick=Long.MIN_VALUE;
    private int blockEntityX,blockEntityZ,blockEntityChunks;
    int texture;
    private record Tile(int x,int y,int width,int height,boolean blockAtlas,boolean fractionalAlpha) {}
    EntityMesh(WorldMesh mesh) {this.mesh=mesh;}
    static void register() {
        WorldRenderEvents.BEFORE_ENTITIES.register(context -> {
            var lights=ShaderLightsAccessor.interstellar$lights();
            light0=new Vector3f(lights[0]);light1=new Vector3f(lights[1]);
        });
    }
    void capture(BlockPos origin,int minChunkX,int minChunkZ,int chunks) {
        collectors.clear();unsupported.clear();vertices=entities=blockEntities=omittedEntities=0;actorId=0;
        int oldTextures=tiles.size();
        var client=MinecraftClient.getInstance();var dispatcher=client.getEntityRenderDispatcher();
        float delta=client.getRenderTickCounter().getTickDelta(false);
        for(var entity:client.world.getEntities()) {
            if(entity.isInvisible() || entity.isRemoved())continue;
            if(entity==client.getCameraEntity() && client.options.getPerspective().isFirstPerson())continue;
            var box=entity.getBoundingBox();
            if(box.maxX<minChunkX*16 || box.minX>(minChunkX+chunks)*16 || box.maxZ<minChunkZ*16 || box.minZ>(minChunkZ+chunks)*16)continue;
            double px=MathHelper.lerp(delta,entity.lastRenderX,entity.getX())-origin.getX();
            double py=MathHelper.lerp(delta,entity.lastRenderY,entity.getY())-origin.getY();
            double pz=MathHelper.lerp(delta,entity.lastRenderZ,entity.getZ())-origin.getZ();
            int before=vertices;
            actorId++;
            dispatcher.render(entity,px,py,pz,entity.getYaw(delta),delta,new MatrixStack(),this,dispatcher.getLight(entity,delta));
            if(vertices>before)entities++;else omittedEntities++;
        }
        // Snapshot loaded chunks' small block-entity maps at most once per game tick.
        // This never generates chunks or scans their block arrays. Keep animated output live.
        long tick=client.world.getTime();
        if(tick!=blockEntityTick || blockEntityX!=minChunkX || blockEntityZ!=minChunkZ || blockEntityChunks!=chunks) {
            blockEntityList.clear();blockEntityTick=tick;blockEntityX=minChunkX;blockEntityZ=minChunkZ;blockEntityChunks=chunks;
            for(int cx=minChunkX;cx<minChunkX+chunks;cx++)for(int cz=minChunkZ;cz<minChunkZ+chunks;cz++)
                if(client.world.getChunkManager().isChunkLoaded(cx,cz))blockEntityList.addAll(client.world.getChunk(cx,cz).getBlockEntities().values());
        }
        var blockDispatcher=client.getBlockEntityRenderDispatcher();
        var matrices=new MatrixStack();
        for(var entity:blockEntityList) {
            if(entity.isRemoved())continue;
            var pos=entity.getPos();matrices.push();
            try {
                matrices.translate(pos.getX()-origin.getX(),pos.getY()-origin.getY(),pos.getZ()-origin.getZ());
                int before=vertices;actorId++;blockDispatcher.render(entity,delta,matrices,this);
                if(vertices>before)blockEntities++;
            } finally {matrices.pop();}
        }
        for(var collector:collectors.values())collector.finish();
        if(texture==0 || tiles.size()!=oldTextures)upload();
        if(!mesh.dynamic() || tiles.size()!=oldTextures)
            Interstellar.LOGGER.info("Entity mesh: {} entities and {} block entities with supported geometry, {} entities wholly omitted, {} vertices, {} textures; omitted layers={}",entities,blockEntities,omittedEntities,vertices,tiles.size(),unsupported);
    }
    String status() {return entities+" entities | "+blockEntities+" block entities | "+omittedEntities+" omitted";}
    @Override public VertexConsumer getBuffer(RenderLayer layer) {
        return collectors.computeIfAbsent(layer,key -> {
            if(!(key instanceof RenderLayerAccessor access) || key.getDrawMode()!=VertexFormat.DrawMode.QUADS) return omitted(key);
            var phases=(RenderPhasesAccessor)(Object)access.interstellar$phases();
            var program=phases.interstellar$program();
            boolean supported=program==RenderPhase.ENTITY_SOLID_PROGRAM || program==RenderPhase.ENTITY_CUTOUT_PROGRAM
                    || program==RenderPhase.ENTITY_CUTOUT_NONULL_PROGRAM || program==RenderPhase.ENTITY_CUTOUT_NONULL_OFFSET_Z_PROGRAM
                    || program==RenderPhase.ARMOR_CUTOUT_NO_CULL_PROGRAM || program==RenderPhase.ENTITY_SMOOTH_CUTOUT_PROGRAM;
            boolean translucent=program==RenderPhase.ENTITY_TRANSLUCENT_PROGRAM || program==RenderPhase.ENTITY_TRANSLUCENT_CULL_PROGRAM || program==RenderPhase.ITEM_ENTITY_TRANSLUCENT_CULL_PROGRAM;
            if((!supported && !translucent) || !(phases.interstellar$texture() instanceof RenderTextureAccessor texture))return omitted(key);
            var id=texture.interstellar$id();
            if(id.isEmpty())return omitted(key);
            return new Collector(tile(id.get()),phases.interstellar$cull()==RenderPhase.DISABLE_CULLING,translucent);
        });
    }
    private Collector omitted(RenderLayer layer) {
        String name=layer.toString();int from=name.indexOf('['),until=name.indexOf(':',from+1);
        unsupported.add(from>=0 && until>from?name.substring(from+1,until):name);
        return new Collector(null,false,false);
    }
    private Tile tile(Identifier id) {
        var old=tiles.get(id);if(old!=null)return old;
        if(id.equals(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)) {var tile=new Tile(0,0,1,1,true,true);tiles.put(id,tile);return tile;}
        int previous=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D),pbo=GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
        int[] names={GL11.GL_PACK_ALIGNMENT,GL11.GL_PACK_ROW_LENGTH,GL11.GL_PACK_SKIP_ROWS,GL11.GL_PACK_SKIP_PIXELS};
        int[] saved=new int[names.length];
        for(int i=0;i<names.length;i++) {saved[i]=GL11.glGetInteger(names[i]);GL11.glPixelStorei(names[i],i==0?4:0);}
        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,0);
        try {
            RenderSystem.bindTexture(MinecraftClient.getInstance().getTextureManager().getTexture(id).getGlId());
            int width=GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_WIDTH);
            int height=GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_HEIGHT);
            if(width<=0 || height<=0 || width>SIZE || height>SIZE)throw new IllegalStateException("Unsupported entity texture dimensions: "+id);
            if(x+width>SIZE) {x=0;y+=rowHeight;rowHeight=0;}
            if(y+height>SIZE)throw new IllegalStateException("Entity texture atlas is full (capture refused)");
            boolean fractionalAlpha=false;
            var source=MemoryUtil.memAlloc(width*height*4);
            try {
                GL11.glGetTexImage(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,source);
                for(int pixel=3;pixel<width*height*4;pixel+=4) {int alpha=source.get(pixel)&255;if(alpha>0 && alpha<255){fractionalAlpha=true;break;}}
                for(int row=0;row<height;row++)MemoryUtil.memCopy(MemoryUtil.memAddress(source)+(long)row*width*4,MemoryUtil.memAddress(pixels)+((long)(y+row)*SIZE+x)*4,(long)width*4);
            } finally {MemoryUtil.memFree(source);}
            var tile=new Tile(x,y,width,height,false,fractionalAlpha);
            x+=width;rowHeight=Math.max(rowHeight,height);tiles.put(id,tile);return tile;
        } finally {
            for(int i=0;i<names.length;i++)GL11.glPixelStorei(names[i],saved[i]);
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,pbo);RenderSystem.bindTexture(previous);
        }
    }
    private void upload() {
        int previous=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D),pbo=GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        int[] names={GL11.GL_UNPACK_ALIGNMENT,GL11.GL_UNPACK_ROW_LENGTH,GL11.GL_UNPACK_SKIP_ROWS,GL11.GL_UNPACK_SKIP_PIXELS};
        int[] saved=new int[names.length];
        for(int i=0;i<names.length;i++) {saved[i]=GL11.glGetInteger(names[i]);GL11.glPixelStorei(names[i],i==0?4:0);}
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,0);
        try {
            if(texture==0)texture=GL11.glGenTextures();RenderSystem.bindTexture(texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,SIZE,SIZE,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
        } finally {
            for(int i=0;i<names.length;i++)GL11.glPixelStorei(names[i],saved[i]);
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,pbo);RenderSystem.bindTexture(previous);
        }
    }
    private final class Collector implements VertexConsumer {
        final Tile tile;final boolean twoSided,translucent;final float[] quad=new float[48];
        int count=-1,owner;float red=1,green=1,blue=1,alpha=1,u,v,shade=1;int blockLight=240,skyLight=240;
        Collector(Tile tile,boolean twoSided,boolean translucent) {this.tile=tile;this.twoSided=twoSided;this.translucent=translucent;}
        private void endVertex() {
            if(tile==null || count<0)return;
            int p=count*12;
            quad[p+3]=(tile.blockAtlas?-1:1)*(translucent && (tile.fractionalAlpha || alpha<.999f)?7:1); // sign chooses the texture atlas.
            quad[p+4]=tile.blockAtlas?u:(tile.x+Math.clamp(u,0,1)*tile.width)/SIZE;
            quad[p+5]=tile.blockAtlas?v:(tile.y+Math.clamp(v,0,1)*tile.height)/SIZE;
            quad[p+6]=((blockLight/16)+.5f)/16;quad[p+7]=((skyLight/16)+.5f)/16;
            quad[p+8]=red*shade;quad[p+9]=green*shade;quad[p+10]=blue*shade;quad[p+11]=alpha;
            if(count==3) {
                // A material flag belongs to the entire quad, even when vertex opacity varies.
                boolean blend=translucent && (tile.fractionalAlpha || quad[11]<.999f || quad[23]<.999f || quad[35]<.999f || quad[47]<.999f);
                for(int vertex=0;vertex<4;vertex++)quad[vertex*12+3]=(tile.blockAtlas?-1:1)*(blend?7:1);
                mesh.entityQuad(quad,twoSided,owner);count=-1;
            }
        }
        void finish() {endVertex();if(tile!=null && count!=-1)throw new IllegalStateException("Incomplete entity quad");}
        @Override public VertexConsumer vertex(float a,float b,float c) {
            if(tile==null)return this;
            endVertex();if(++vertices>MAX_VERTICES)throw new IllegalStateException("Entity mesh vertex cap exceeded");
            if(count==-1)owner=actorId;
            count++;int p=count*12;quad[p]=a;quad[p+1]=b;quad[p+2]=c;return this;
        }
        @Override public VertexConsumer color(int r,int g,int b,int a) {red=r/255f;green=g/255f;blue=b/255f;alpha=a/255f;return this;}
        @Override public VertexConsumer texture(float a,float b) {u=a;v=b;return this;}
        @Override public VertexConsumer overlay(int a,int b) {return this;} // Hurt/flash overlay is explicitly not captured yet.
        @Override public VertexConsumer light(int a,int b) {blockLight=a;skyLight=b;return this;}
        @Override public VertexConsumer normal(float a,float b,float c) {
            shade=Math.min(1,.4f+.6f*(Math.max(0,light0.dot(a,b,c))+Math.max(0,light1.dot(a,b,c))));return this;
        }
    }
    @Override public void close() {MemoryUtil.memFree(pixels);if(texture!=0)RenderSystem.deleteTexture(texture);}
}
