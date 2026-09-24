package io.github.rohrl.interstellar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.SimpleFramebuffer;

/** Curved model mask followed by a small screen-space edge filter, only while needed. */
final class GlowingOutline implements AutoCloseable {
    static final ShaderProgram[] rays=new ShaderProgram[2];
    static ShaderProgram edge;
    private SimpleFramebuffer target;
    void render(int w,int h,Runnable trace) {
        var client=MinecraftClient.getInstance();
        if(target==null || target.textureWidth!=w || target.textureHeight!=h) {
            if(target!=null)target.delete();target=new SimpleFramebuffer(w,h,false,false);
        }
        try {
            target.beginWrite(true);RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableBlend();
            trace.run();
            client.getFramebuffer().beginWrite(true);RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(()->edge);edge.addSampler("Scene",target.getColorAttachment());
            int width=client.getWindow().getFramebufferWidth(),height=client.getWindow().getFramebufferHeight();
            edge.getUniformOrDefault("Viewport").set((float)width,(float)height);
            edge.getUniformOrDefault("Texel").set(1f/w,1f/h);TerrainScreen.drawQuad(width,height);
        } finally {
            client.getFramebuffer().beginWrite(true);RenderSystem.depthMask(true);RenderSystem.enableDepthTest();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        }
    }
    public void close() {if(target!=null)target.delete();}
}
