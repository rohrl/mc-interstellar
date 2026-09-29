package io.github.rohrl.interstellar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.*;
import org.lwjgl.opengl.GL11;

/** Retains one completed BH image for a short reveal; never traces two scenes per frame. */
final class WormholeReveal implements AutoCloseable {
    private static ShaderProgram shader;
    private SimpleFramebuffer previous;
    static void setShader(ShaderProgram program) {shader=program;}
    void hold(SimpleFramebuffer image) {close();previous=image;}
    void draw(int width,int height,float opacity) {
        if(previous==null)return;
        if(opacity<=0 || shader==null){close();return;}
        previous.setTexFilter(GL11.GL_LINEAR);
        RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        try {
            RenderSystem.viewport(0,0,width,height);RenderSystem.setShader(()->shader);
            shader.addSampler("Previous",previous.getColorAttachment());
            shader.getUniformOrDefault("Viewport").set((float)width,(float)height);
            shader.getUniformOrDefault("Opacity").set(opacity);
            var quad=Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,VertexFormats.POSITION);
            quad.vertex(0,0,0);quad.vertex(0,height,0);quad.vertex(width,height,0);quad.vertex(width,0,0);
            BufferRenderer.drawWithGlobalProgram(quad.end());
        } finally {RenderSystem.depthMask(true);RenderSystem.enableDepthTest();}
    }
    public void close() {if(previous!=null){previous.delete();previous=null;}}
}
