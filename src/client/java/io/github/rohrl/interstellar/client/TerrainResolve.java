package io.github.rohrl.interstellar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.*;
import org.lwjgl.opengl.GL11;

/** Cheap spatial edge smoothing and linear reconstruction; never filters the vanilla HUD. */
final class TerrainResolve {
    private static ShaderProgram shader;
    static void setShader(ShaderProgram next) {shader=next;}
    static void draw(SimpleFramebuffer target,int width,int height,boolean edge) {
        if(shader==null) {target.draw(width,height);return;}
        target.setTexFilter(GL11.GL_LINEAR);
        try {
            RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableBlend();
            RenderSystem.viewport(0,0,width,height);RenderSystem.setShader(()->shader);
            shader.addSampler("Scene",target.getColorAttachment());
            shader.getUniformOrDefault("Viewport").set((float)width,(float)height);
            shader.getUniformOrDefault("Texel").set(1f/target.textureWidth,1f/target.textureHeight);
            shader.getUniformOrDefault("EdgeAA").set(edge?1f:0f);
            var quad=Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,VertexFormats.POSITION);
            quad.vertex(0,0,0);quad.vertex(0,height,0);quad.vertex(width,height,0);quad.vertex(width,0,0);
            BufferRenderer.drawWithGlobalProgram(quad.end());
        } finally {
            target.setTexFilter(GL11.GL_NEAREST);
            RenderSystem.depthMask(true);RenderSystem.enableDepthTest();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        }
    }
}
