package io.github.rohrl.interstellar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.SimpleFramebuffer;
import org.lwjgl.opengl.*;
import java.nio.FloatBuffer;

/** Two full-precision subpixel images; average before the existing 8-bit resolve input. */
final class TerrainSamples implements AutoCloseable {
    private static ShaderProgram foldShader;
    private static int maxTextureWidth;
    private final SimpleFramebuffer target;
    private SimpleFramebuffer mask;
    final int width,height;
    static void setShader(ShaderProgram program) {foldShader=program;maxTextureWidth=GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);}
    static boolean supported(int width) {return foldShader!=null && width<=maxTextureWidth/2;}
    TerrainSamples(int width,int height) {
        this.width=width;this.height=height;
        target=new SimpleFramebuffer(width*2,height,false,false);
        int oldTexture=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int oldUnpack=GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        try {
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D,target.getColorAttachment());
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL30.GL_RGBA32F,width*2,height,0,GL11.GL_RGBA,GL11.GL_FLOAT,(FloatBuffer)null);
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D,oldTexture);
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,oldUnpack);
        }
        target.beginWrite(false);
        if(GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER)!=GL30.GL_FRAMEBUFFER_COMPLETE) {
            target.delete();throw new IllegalStateException("AA sample framebuffer incomplete");
        }
    }
    void begin(int sample) {
        target.beginWrite(false);
        RenderSystem.viewport(sample*width,0,width,height);
    }
    void fold(SimpleFramebuffer destination,Runnable draw) {
        destination.beginWrite(true);
        RenderSystem.setShader(()->foldShader);
        foldShader.addSampler("Samples",target.getColorAttachment());
        foldShader.getUniformOrDefault("Viewport").set((float)width,(float)height);
        draw.run();
    }
    int copyMask() {
        if(mask==null)mask=new SimpleFramebuffer(width*2,height,false,false);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,target.fbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,mask.fbo);
        GL30.glBlitFramebuffer(0,0,width*2,height,0,0,width*2,height,GL11.GL_COLOR_BUFFER_BIT,GL11.GL_NEAREST);
        return mask.getColorAttachment();
    }
    @Override public void close() {target.delete();if(mask!=null)mask.delete();}
}
