package io.github.rohrl.interstellar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;
import java.nio.FloatBuffer;
import io.github.rohrl.interstellar.scene.CompactMeshNodes;

/** Render-thread streaming texture. Caller establishes/restores unpack state. */
final class ReusableMeshTexture implements AutoCloseable {
    final int id=GL11.glGenTextures();
    private int rows;
    private FloatBuffer staging;
    int allocations;
    private final boolean compact;
    private final QuantizedBoundsTexture quantized;
    int quantizedTexture() {return quantized==null?0:quantized.id;}
    ReusableMeshTexture() {this(false);}
    ReusableMeshTexture(boolean compact) {this.compact=compact;quantized=compact && TerrainScreen.hasQuantizedShader()?new QuantizedBoundsTexture():null;}
    void upload(float[] data,int length) {
        int width=compact?4095:4096,stride=width*4;
        int needed=compact?CompactMeshNodes.rows(length):Math.max(1,(length+stride-1)/stride);
        RenderSystem.bindTexture(id);
        if(needed>rows) {
            int capacity=Integer.highestOneBit(needed-1)<<1;if(capacity==0)capacity=1;
            if(capacity>GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE))throw new IllegalStateException("Moving mesh exceeds GPU texture capacity");
            var next=MemoryUtil.memCallocFloat(capacity*stride);
            if(staging!=null)MemoryUtil.memFree(staging);staging=next;rows=capacity;
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL30.GL_RGBA32F,width,rows,0,GL11.GL_RGBA,GL11.GL_FLOAT,(FloatBuffer)null);
            allocations++;
        }
        staging.clear();if(compact)CompactMeshNodes.write(data,length,0,staging);else staging.put(data,0,length);staging.position(0).limit(needed*stride);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D,0,0,0,width,needed,GL11.GL_RGBA,GL11.GL_FLOAT,staging);
        if(quantized!=null)quantized.write(0,data,length);
    }
    @Override public void close() {RenderSystem.deleteTexture(id);if(staging!=null){MemoryUtil.memFree(staging);staging=null;}if(quantized!=null)quantized.close();}
}
