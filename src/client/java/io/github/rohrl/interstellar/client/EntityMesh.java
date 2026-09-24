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
import org.joml.Matrix4f;
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
    private final List<StripCollector> strips=new ArrayList<>();
    private final Map<Identifier,Tile> tiles=new HashMap<>();
    private final Set<String> unsupported=new TreeSet<>();
    private final Map<Identifier,Long> glyphHashes=new HashMap<>(),previousGlyphHashes=new HashMap<>();
    private final ByteBuffer pixels=MemoryUtil.memCalloc(SIZE*SIZE*4);
    private int x,y,rowHeight,vertices,entities,blockEntities,omittedEntities;
    private final List<BlockEntity> blockEntityList=new ArrayList<>();
    private long blockEntityTick=Long.MIN_VALUE;
    private int blockEntityX,blockEntityZ,blockEntityChunks;
    int texture;
    private record Tile(int x,int y,int width,int height,boolean blockAtlas,boolean fractionalAlpha) {}
    private final Tile white=new Tile(0,0,1,1,false,false);
    private boolean cameraBody;
    final GlowingMesh glowing=new GlowingMesh();
    private int glowColour=-1;
    EntityMesh(WorldMesh mesh) {this.mesh=mesh;pixels.putInt(0,-1);x=1;rowHeight=1;}
    static void register() {
        WorldRenderEvents.BEFORE_ENTITIES.register(context -> {
            var lights=ShaderLightsAccessor.interstellar$lights();
            light0=new Vector3f(lights[0]);light1=new Vector3f(lights[1]);
        });
    }
    void capture(BlockPos origin,int minChunkX,int minChunkZ,int chunks) {
        collectors.clear();strips.clear();unsupported.clear();glyphHashes.clear();glowing.reset();vertices=entities=blockEntities=omittedEntities=0;
        int oldTextures=tiles.size();
        var client=MinecraftClient.getInstance();var dispatcher=client.getEntityRenderDispatcher();
        float delta=client.getRenderTickCounter().getTickDelta(false);
        for(var entity:client.world.getEntities()) {
            if(entity.isRemoved() || entity.isInvisible() && !client.hasOutline(entity))continue;
            glowColour=client.hasOutline(entity)?entity.getTeamColorValue():-1;
            cameraBody=entity==client.getCameraEntity() && client.options.getPerspective().isFirstPerson();
            if(cameraBody && !WorldFeatures.body)continue;
            var box=entity.getBoundingBox();
            if(box.maxX<minChunkX*16 || box.minX>(minChunkX+chunks)*16 || box.maxZ<minChunkZ*16 || box.minZ>(minChunkZ+chunks)*16)continue;
            double px=MathHelper.lerp(delta,entity.lastRenderX,entity.getX())-origin.getX();
            double py=MathHelper.lerp(delta,entity.lastRenderY,entity.getY())-origin.getY();
            double pz=MathHelper.lerp(delta,entity.lastRenderZ,entity.getZ())-origin.getZ();
            int before=vertices;
            captureCue=0;
            var source=SelectedSource.current();
            if(entity instanceof net.minecraft.entity.mob.MobEntity && source!=null && source.blackHoleProxy()
                    && !client.world.getRegistryKey().getValue().toString().equals("interstellar:demo")) {
                double ratio=entity.getPos().add(0,entity.getHeight()*.5,0).distanceTo(new net.minecraft.util.math.Vec3d(source.x(),source.y(),source.z()))/source.schwarzschildRadius();
                float t=(float)Math.clamp((3-ratio)/2,0,1);
                captureCue=t*t*(3-2*t);
            }
            dispatcher.render(entity,px,py,pz,entity.getYaw(delta),delta,new MatrixStack(),this,dispatcher.getLight(entity,delta));
            if(vertices>before)entities++;else omittedEntities++;
        }
        captureCue=0;cameraBody=false;glowColour=-1;
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
                int before=vertices;blockDispatcher.render(entity,delta,matrices,this);
                if(vertices>before)blockEntities++;
            } finally {matrices.pop();}
        }
        InteractionMesh.capture(this,origin);
        for(var strip:strips)strip.finish();
        for(var collector:collectors.values())collector.finish();
        glowing.upload();
        boolean glyphChanged=false;
        for(var entry:glyphHashes.entrySet())if(!entry.getValue().equals(previousGlyphHashes.get(entry.getKey()))) {
            refreshTile(entry.getKey(),tiles.get(entry.getKey()));glyphChanged=true;
        }
        previousGlyphHashes.clear();previousGlyphHashes.putAll(glyphHashes);
        if(texture==0 || tiles.size()!=oldTextures || glyphChanged)upload();
        if(!mesh.dynamic() || tiles.size()!=oldTextures)
            Interstellar.LOGGER.info("Entity mesh: {} entities and {} block entities with supported geometry, {} entities wholly omitted, {} vertices, {} textures; omitted layers={}",entities,blockEntities,omittedEntities,vertices,tiles.size(),unsupported);
    }
    String status() {return entities+" entities | "+blockEntities+" block entities | "+omittedEntities+" omitted";}
    @Override public VertexConsumer getBuffer(RenderLayer layer) {
        if(layer==RenderLayer.getLeash() || layer==RenderLayer.getLineStrip()) {
            var strip=new StripCollector(layer==RenderLayer.getLineStrip());strips.add(strip);return strip;
        }
        return collectors.computeIfAbsent(layer,key -> {
            if(!(key instanceof RenderLayerAccessor access) || key.getDrawMode()!=VertexFormat.DrawMode.QUADS) return omitted(key);
            var phases=(RenderPhasesAccessor)(Object)access.interstellar$phases();
            var program=phases.interstellar$program();
            if(program==RenderPhase.OUTLINE_PROGRAM && phases.interstellar$texture() instanceof RenderTextureAccessor outlineTexture && outlineTexture.interstellar$id().isPresent()) {
                var collector=new Collector(tile(outlineTexture.interstellar$id().get()),true,1,null,null,0);collector.outlineOnly=true;collector.glowEligible=true;return collector;
            }
            boolean supported=program==RenderPhase.ENTITY_SOLID_PROGRAM || program==RenderPhase.ENTITY_CUTOUT_PROGRAM
                    || program==RenderPhase.ENTITY_CUTOUT_NONULL_PROGRAM || program==RenderPhase.ENTITY_CUTOUT_NONULL_OFFSET_Z_PROGRAM
                    || program==RenderPhase.ARMOR_CUTOUT_NO_CULL_PROGRAM || program==RenderPhase.ENTITY_SMOOTH_CUTOUT_PROGRAM;
            boolean translucent=program==RenderPhase.ENTITY_TRANSLUCENT_PROGRAM || program==RenderPhase.ENTITY_TRANSLUCENT_CULL_PROGRAM || program==RenderPhase.ITEM_ENTITY_TRANSLUCENT_CULL_PROGRAM;
            boolean shadow=program==RenderPhase.ENTITY_SHADOW_PROGRAM;
            boolean eyes=program==RenderPhase.EYES_PROGRAM;
            boolean emissive=program==RenderPhase.ENTITY_TRANSLUCENT_EMISSIVE_PROGRAM;
            boolean glint=program==RenderPhase.GLINT_PROGRAM || program==RenderPhase.ENTITY_GLINT_PROGRAM || program==RenderPhase.DIRECT_ENTITY_GLINT_PROGRAM
                    || program==RenderPhase.ARMOR_ENTITY_GLINT_PROGRAM || program==RenderPhase.TRANSLUCENT_GLINT_PROGRAM;
            boolean font=program==RenderPhase.TEXT_PROGRAM || program==RenderPhase.TEXT_INTENSITY_PROGRAM;
            boolean intensity=program==RenderPhase.TEXT_INTENSITY_PROGRAM;
            boolean cracks=program==RenderPhase.CRUMBLING_PROGRAM;
            if(program==RenderPhase.TEXT_BACKGROUND_PROGRAM)return new Collector(white,true,7,null,null,0);
            if(!supported && !translucent && !shadow && !eyes && !emissive && !glint && !font && !cracks)return omitted(key);
            if(!(phases.interstellar$texture() instanceof RenderTextureAccessor texture))return omitted(key);
            var id=texture.interstellar$id();
            if(id.isEmpty())return omitted(key);
            Matrix4f transform=null;
            if(glint) {
                var previous=new Matrix4f(RenderSystem.getTextureMatrix());
                try {phases.interstellar$texturing().startDrawing();transform=new Matrix4f(RenderSystem.getTextureMatrix());}
                finally {RenderSystem.setTextureMatrix(previous);}
            }
            int material=glint?13:eyes?11:shadow || emissive?9:intensity?15:cracks?17:translucent || font?7:1;
            var collector=new Collector(tile(id.get()),phases.interstellar$cull()==RenderPhase.DISABLE_CULLING,material,font?id.get():null,transform,
                    glint || eyes || cracks || shadow?.002f:0);
            collector.glowEligible=supported || translucent;return collector;
        });
    }
    private Collector omitted(RenderLayer layer) {
        String name=layer.toString();int from=name.indexOf('['),until=name.indexOf(':',from+1);
        unsupported.add(from>=0 && until>from?name.substring(from+1,until):name);
        return new Collector(null,false,1,null,null,0);
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
    private void refreshTile(Identifier id,Tile tile) {
        // Font pages grow as new glyphs are requested. Read back only when the set of
        // emitted glyph UVs changes, never because a label moved or a camera rotated.
        int previous=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D),pbo=GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
        int[] names={GL11.GL_PACK_ALIGNMENT,GL11.GL_PACK_ROW_LENGTH,GL11.GL_PACK_SKIP_ROWS,GL11.GL_PACK_SKIP_PIXELS},saved=new int[4];
        var source=MemoryUtil.memAlloc(tile.width*tile.height*4);
        try {
            for(int i=0;i<4;i++){saved[i]=GL11.glGetInteger(names[i]);GL11.glPixelStorei(names[i],i==0?4:0);}
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,0);
            RenderSystem.bindTexture(MinecraftClient.getInstance().getTextureManager().getTexture(id).getGlId());
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,source);
            for(int row=0;row<tile.height;row++)MemoryUtil.memCopy(MemoryUtil.memAddress(source)+(long)row*tile.width*4,
                    MemoryUtil.memAddress(pixels)+((long)(tile.y+row)*SIZE+tile.x)*4,(long)tile.width*4);
        } finally {
            MemoryUtil.memFree(source);for(int i=0;i<4;i++)GL11.glPixelStorei(names[i],saved[i]);
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,pbo);RenderSystem.bindTexture(previous);
        }
    }
    VertexConsumer solidColour() {return collectors.computeIfAbsent(RenderLayer.getDebugQuads(),key->new Collector(white,true,9,null,null,0));}
    // A cheap approach/capture cue, not spectral transport. Stored with each vertex's colour
    // so delayed quad completion cannot inherit the next entity's tint. No new draw or blend pass.
    private float captureCue;
    private final class StripCollector implements VertexConsumer {
        private final boolean line;
        private final List<float[]> points=new ArrayList<>();
        private float[] point;
        StripCollector(boolean line) {this.line=line;}
        public VertexConsumer vertex(float x,float y,float z) {
            if(++vertices>MAX_VERTICES)throw new IllegalStateException("Entity mesh vertex cap exceeded");
            point=new float[]{x,y,z,1,0,0,15.5f/16,15.5f/16,1,1,1,1};points.add(point);return this;
        }
        public VertexConsumer color(int r,int g,int b,int a) {point[8]=r/255f;point[9]=g/255f;point[10]=b/255f;point[11]=a/255f;return this;}
        public VertexConsumer texture(float u,float v) {return this;}
        public VertexConsumer overlay(int u,int v) {return this;}
        public VertexConsumer light(int u,int v) {point[6]=(u/16+.5f)/16;point[7]=(v/16+.5f)/16;return this;}
        public VertexConsumer normal(float x,float y,float z) {return this;}
        void finish() {
            if(line) {
                var out=solidColour();
                for(int i=1;i<points.size();i++) {
                    var a=points.get(i-1);var b=points.get(i);
                    InteractionMesh.line(out,new net.minecraft.util.math.Vec3d(a[0],a[1],a[2]),new net.minecraft.util.math.Vec3d(b[0],b[1],b[2]),.004,
                            Math.round(b[8]*255),Math.round(b[9]*255),Math.round(b[10]*255),Math.round(b[11]*255));
                }
            } else for(int i=2;i<points.size();i++)mesh.entityTriangle(points.get(i-2),points.get(i-1),points.get(i));
        }
    }
    private final class Collector implements VertexConsumer {
        final Tile tile;final boolean twoSided;final int material;final Identifier font;final Matrix4f transform;final float bias;final float[] quad=new float[48];
        int count=-1;float red=1,green=1,blue=1,alpha=1,u,v,shade=1;int blockLight=240,skyLight=240;
        int quadGlow=-1;boolean outlineOnly,glowEligible;
        Collector(Tile tile,boolean twoSided,int material,Identifier font,Matrix4f transform,float bias) {
            this.tile=tile;this.twoSided=twoSided;this.material=material;this.font=font;this.transform=transform;this.bias=bias;
        }
        private void endVertex() {
            if(tile==null || count<0)return;
            int p=count*12;
            quad[p+4]=tile.blockAtlas?u:(tile.x+Math.clamp(u,0,1)*tile.width)/SIZE;
            quad[p+5]=tile.blockAtlas?v:(tile.y+Math.clamp(v,0,1)*tile.height)/SIZE;
            quad[p+6]=((blockLight/16)+.5f)/16;quad[p+7]=((skyLight/16)+.5f)/16;
            float lighting=material==9 || material==11 || material==13 || material==17?1:shade;
            if(material==13) {
                var uv=transform.transformPosition(u,v,0,new Vector3f());quad[p+4]=uv.x;quad[p+5]=uv.y;
                // Exact integer packing; the material reuses light coordinates for its
                // atlas rectangle. Wrap after interpolation, avoiding UV seam artifacts.
                quad[p+6]=tile.x+4096*tile.width;quad[p+7]=tile.y+4096*tile.height;
                lighting=MinecraftClient.getInstance().options.getGlintStrength().getValue().floatValue();
            }
            if(material==17) {
                // Native damage projection repeats negative/local face coordinates.
                quad[p+4]=u;quad[p+5]=v;quad[p+6]=tile.x+4096*tile.width;quad[p+7]=tile.y+4096*tile.height;
            }
            quad[p+8]=red*lighting;quad[p+9]=green*lighting;quad[p+10]=blue*lighting;quad[p+11]=alpha;
            if(count==3) {
                // A material flag belongs to the entire quad, even when vertex opacity varies.
                int mode=material;
                if(mode==7 && font==null && !tile.fractionalAlpha && quad[11]>=.999f && quad[23]>=.999f && quad[35]>=.999f && quad[47]>=.999f)mode=1;
                for(int vertex=0;vertex<4;vertex++)quad[vertex*12+3]=(tile.blockAtlas?-1:1)*(mode+quad[vertex*12+3]);
                if(bias>0) {
                    var normal=new Vector3f(quad[12]-quad[0],quad[13]-quad[1],quad[14]-quad[2])
                            .cross(quad[24]-quad[0],quad[25]-quad[1],quad[26]-quad[2]);
                    if(normal.lengthSquared()>1e-16f) {normal.normalize(bias);for(int vertex=0;vertex<4;vertex++){int j=vertex*12;quad[j]+=normal.x;quad[j+1]+=normal.y;quad[j+2]+=normal.z;}}
                }
                if(quadGlow!=-1 && glowEligible)glowing.quad(quad,twoSided,quadGlow);
                if(!outlineOnly)mesh.entityQuad(quad,twoSided);count=-1;
            }
        }
        void finish() {endVertex();if(tile!=null && count!=-1)throw new IllegalStateException("Incomplete entity quad");}
        @Override public VertexConsumer vertex(float a,float b,float c) {
            if(tile==null)return this;
            endVertex();if(++vertices>MAX_VERTICES)throw new IllegalStateException("Entity mesh vertex cap exceeded");
            count++;if(count==0)quadGlow=glowColour;int p=count*12;quad[p]=a;quad[p+1]=b;quad[p+2]=c;quad[p+3]=cameraBody?32:0;return this;
        }
        @Override public VertexConsumer color(int r,int g,int b,int a) {
            float dim=1-.7f*captureCue;
            red=r/255f*dim;green=g/255f*dim*(1-.65f*captureCue);blue=b/255f*dim*(1-.9f*captureCue);alpha=a/255f;return this;
        }
        @Override public VertexConsumer texture(float a,float b) {
            u=a;v=b;
            if(font!=null)glyphHashes.merge(font,((long)Float.floatToIntBits(a)<<32)^(Float.floatToIntBits(b)&0xffffffffL),(h,n)->h*31+n);
            return this;
        }
        @Override public VertexConsumer overlay(int a,int b) {return this;} // Hurt/flash overlay is explicitly not captured yet.
        @Override public VertexConsumer light(int a,int b) {blockLight=a;skyLight=b;return this;}
        @Override public VertexConsumer normal(float a,float b,float c) {
            shade=Math.min(1,.4f+.6f*(Math.max(0,light0.dot(a,b,c))+Math.max(0,light1.dot(a,b,c))));return this;
        }
    }
    @Override public void close() {glowing.close();MemoryUtil.memFree(pixels);if(texture!=0)RenderSystem.deleteTexture(texture);}
}
