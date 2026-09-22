package io.github.rohrl.interstellar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;
import java.nio.FloatBuffer;
import io.github.rohrl.interstellar.scene.CompactMeshNodes;

/** Rows align both 36-float triangles and 12-float nodes, allowing independent chunk replacement. */
final class MeshArena implements AutoCloseable {
    static final int WIDTH=4095,FLOATS=WIDTH*4,TRIANGLES=FLOATS/36,NODES=FLOATS/12;
    final int texture;
    private final boolean compact;
    private final int width,floats;
    MeshArena(int height) {this(height,false);}
    MeshArena(int height,boolean compact) {
        this(height,compact,WIDTH);
    }
    MeshArena(int height,boolean compact,int width) {
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
