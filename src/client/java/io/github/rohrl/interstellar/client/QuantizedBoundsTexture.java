package io.github.rohrl.interstellar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.rohrl.interstellar.scene.QuantizedMeshBounds;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;
import java.nio.IntBuffer;

/** Companion bounds texture; caller establishes/restores unpack and binding state. */
final class QuantizedBoundsTexture implements AutoCloseable {
    final int id=GL11.glGenTextures();
    private int height;
    private IntBuffer staging;
    void allocate(int rows) {
        RenderSystem.bindTexture(id);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL30.GL_RGBA32UI,QuantizedMeshBounds.WIDTH,rows,0,GL30.GL_RGBA_INTEGER,GL11.GL_UNSIGNED_INT,(IntBuffer)null);
        if(GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_WIDTH)!=QuantizedMeshBounds.WIDTH)
            throw new IllegalStateException("Quantized bounds allocation failed");
        height=rows;
    }
    void write(int row,float[] data,int length) {
        int rows=Math.max(1,(length/12+QuantizedMeshBounds.WIDTH-1)/QuantizedMeshBounds.WIDTH);
        if(row+rows>height) {
            int capacity=Integer.highestOneBit(row+rows-1)<<1;if(capacity==0)capacity=1;
            if(capacity>GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE))throw new IllegalStateException("Quantized bounds exceed texture capacity");
            allocate(capacity);
        }
        int ints=rows*QuantizedMeshBounds.WIDTH*4;
        if(staging==null || staging.capacity()<ints) {
            if(staging!=null)MemoryUtil.memFree(staging);
            staging=MemoryUtil.memCallocInt(ints);
        }
        staging.clear();QuantizedMeshBounds.write(data,length,staging);staging.position(0).limit(ints);
        RenderSystem.bindTexture(id);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D,0,0,row,QuantizedMeshBounds.WIDTH,rows,GL30.GL_RGBA_INTEGER,GL11.GL_UNSIGNED_INT,staging);
    }
    @Override public void close() {RenderSystem.deleteTexture(id);if(staging!=null)MemoryUtil.memFree(staging);}
}
