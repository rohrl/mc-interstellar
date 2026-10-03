package io.github.rohrl.interstellar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.*;
import org.lwjgl.opengl.*;
import java.nio.FloatBuffer;

/** Disk-only glare at a bounded resolution. Shared final presentation for both backends. */
final class AccretionBloom implements AutoCloseable {
    private static ShaderProgram shader;
    private SimpleFramebuffer first,second;
    static void setShader(ShaderProgram next){shader=next;}
    int render(SimpleFramebuffer scene) {
        int w=Math.min(160,(scene.textureWidth+3)/4);
        int h=Math.max(1,Math.round(w*(float)scene.textureHeight/scene.textureWidth));
        if(first==null||first.textureWidth!=w||first.textureHeight!=h) {
            close();first=create(w,h);second=create(w,h);
        }
        RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableBlend();
        // Contiguous taps: stretching a sparse kernel at fullscreen resolution
        // makes repeated images of narrow bright rings instead of a smooth glow.
        pass(scene,first,true,0,0);pass(first,second,false,1,0);pass(second,first,false,0,1);
        return first.getColorAttachment();
    }
    private static SimpleFramebuffer create(int w,int h) {
        var target=new SimpleFramebuffer(w,h,false,false);
        int texture=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D),unpack=GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        try {
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D,target.getColorAttachment());
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL30.GL_RGBA16F,w,h,0,GL11.GL_RGBA,GL11.GL_FLOAT,(FloatBuffer)null);
            target.setTexFilter(GL11.GL_LINEAR);
        } finally {GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,unpack);}
        return target;
    }
    private static void pass(SimpleFramebuffer input,SimpleFramebuffer output,boolean seed,float x,float y) {
        output.beginWrite(true);RenderSystem.setShader(()->shader);
        shader.addSampler("Scene",input.getColorAttachment());
        shader.getUniformOrDefault("Viewport").set((float)output.textureWidth,(float)output.textureHeight);
        shader.getUniformOrDefault("InputSize").set((float)input.textureWidth,(float)input.textureHeight);
        shader.getUniformOrDefault("Direction").set(x,y);shader.getUniformOrDefault("Seed").set(seed?1f:0f);
        var q=Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,VertexFormats.POSITION);
        q.vertex(0,0,0);q.vertex(0,output.textureHeight,0);q.vertex(output.textureWidth,output.textureHeight,0);q.vertex(output.textureWidth,0,0);
        BufferRenderer.drawWithGlobalProgram(q.end());
    }
    @Override public void close(){if(first!=null){first.delete();first=null;}if(second!=null){second.delete();second=null;}}
}
