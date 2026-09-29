package io.github.rohrl.interstellar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;
import java.nio.FloatBuffer;
import io.github.rohrl.interstellar.scene.CompactMeshNodes;

/** Aligned rows allow independent chunk replacement:4095-wide triangles/nodes,4092-wide quads. */
final class MeshArena implements AutoCloseable {
    static final int WIDTH=4095,FLOATS=WIDTH*4,TRIANGLES=FLOATS/36,NODES=FLOATS/12;
    int texture;
    private int height;
    private final boolean compact;
    private final int width,floats;
    MeshArena(int height) {this(height,false);}
    MeshArena(int height,boolean compact) {
        this(height,compact,WIDTH);
    }
    MeshArena(int height,boolean compact,int width) {
        this.height=height;
        this.compact=compact;
        this.width=width;floats=width*4;
        if(compact && width!=WIDTH)throw new IllegalArgumentException("Compact nodes need their original row layout");
        if(height>GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE))throw new IllegalStateException("Native mesh atlas exceeds device limits");
        texture=GL11.glGenTextures();
        try {withUnpack(()-> {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL30.GL_RGBA32F,width,height,0,GL11.GL_RGBA,GL11.GL_FLOAT,(FloatBuffer)null);
            if(GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_WIDTH)!=width)throw new IllegalStateException("Native mesh atlas allocation failed");
        });} catch(RuntimeException e) {RenderSystem.deleteTexture(texture);throw e;}
    }
    int rows(){return height;}
    /** Preserve every texel/address. Only capacity changes; the shader layout is identical. */
    void grow(int rows) {
        grow(rows,Boolean.getBoolean("interstellar.forceFramebufferCopy"));
    }
    private void grow(int rows,boolean framebufferCopy) {
        if(rows<=height)return;
        var next=new MeshArena(rows,compact,width);
        try {
            var caps=GL.getCapabilities();
            if(!framebufferCopy && (caps.OpenGL43 || caps.GL_ARB_copy_image))
                GL43.glCopyImageSubData(texture,GL11.GL_TEXTURE_2D,0,0,0,0,next.texture,GL11.GL_TEXTURE_2D,0,0,0,0,width,height,1);
            else {
                int read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),draw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
                int a=GL30.glGenFramebuffers(),b=GL30.glGenFramebuffers();boolean scissor=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
                try {
                    GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,a);GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,GL11.GL_TEXTURE_2D,texture,0);
                    GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,b);GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,GL11.GL_TEXTURE_2D,next.texture,0);
                    if(GL30.glCheckFramebufferStatus(GL30.GL_READ_FRAMEBUFFER)!=GL30.GL_FRAMEBUFFER_COMPLETE || GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER)!=GL30.GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Terrain storage copy framebuffer unavailable");
                    GL11.glDisable(GL11.GL_SCISSOR_TEST);
                    GL30.glBlitFramebuffer(0,0,width,height,0,0,width,height,GL11.GL_COLOR_BUFFER_BIT,GL11.GL_NEAREST);
                } finally {
                    if(scissor)GL11.glEnable(GL11.GL_SCISSOR_TEST);
                    GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw);GL30.glDeleteFramebuffers(a);GL30.glDeleteFramebuffers(b);
                }
            }
        } catch(RuntimeException e){next.close();throw e;}
        int old=texture;texture=next.texture;height=rows;RenderSystem.deleteTexture(old);
    }
    /** Opt-in runtime check, including finite bit-carrier values used by compact nodes. */
    static void validateCopies() {
        for(boolean fallback:new boolean[]{false,true})try(var arena=new MeshArena(3)) {
            float[] data=new float[3*FLOATS];
            for(int i=0;i<data.length;i++)data[i]=i%3==0?Float.intBitsToFloat(0x40000000|i*137):i%3==1?-i*.125f:i/65536f;
            arena.write(0,data,data.length);arena.grow(7,fallback);
            try(var state=new TerrainReplay.PackState()) {
                var actual=TerrainReplay.readTexture(arena.texture,0,0,WIDTH,3,true);
                try {for(int i=0;i<data.length;i++)if(actual.getInt(i*4)!=Float.floatToRawIntBits(data[i]))throw new IllegalStateException("Terrain growth changed texel "+i+", framebuffer="+fallback);}
                finally {MemoryUtil.memFree(actual);}
            }
            if(GL11.glGetError()!=GL11.GL_NO_ERROR)throw new IllegalStateException("Terrain growth GL error");
        }
        io.github.rohrl.interstellar.Interstellar.LOGGER.info("Terrain storage copy audit passed: direct and framebuffer, 49140 exact floats each");
    }
    void write(int row,float[] data,int length) {
        int rows=(length+floats-1)/floats;if(rows==0)return;
        var staging=MemoryUtil.memCallocFloat(rows*floats);
        try {if(compact)CompactMeshNodes.write(data,length,row,staging);else staging.put(data,0,length);staging.position(0);withUnpack(()->GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D,0,0,row,width,rows,GL11.GL_RGBA,GL11.GL_FLOAT,staging));}
        finally {MemoryUtil.memFree(staging);}
    }
    private void withUnpack(Runnable action) {
        int previous=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D),pbo=GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        int[] names={GL11.GL_UNPACK_ALIGNMENT,GL11.GL_UNPACK_ROW_LENGTH,GL11.GL_UNPACK_SKIP_ROWS,GL11.GL_UNPACK_SKIP_PIXELS},saved=new int[4];
        for(int i=0;i<4;i++){saved[i]=GL11.glGetInteger(names[i]);GL11.glPixelStorei(names[i],i==0?4:0);}
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,0);RenderSystem.bindTexture(texture);
        try {action.run();} finally {
            for(int i=0;i<4;i++)GL11.glPixelStorei(names[i],saved[i]);
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,pbo);RenderSystem.bindTexture(previous);
        }
    }
    @Override public void close() {RenderSystem.deleteTexture(texture);}
}
