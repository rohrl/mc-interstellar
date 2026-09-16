package io.github.rohrl.interstellar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/** Six directional captures of vanilla sky/cloud rendering; contains no terrain images. */
final class NativeSky implements AutoCloseable {
    private static final int SIZE=256;
    private static final float[][] DIRECTIONS={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    private static final float[][] UPS={{0,1,0},{0,1,0},{0,0,1},{0,0,-1},{0,1,0},{0,1,0}};
    private SimpleFramebuffer target;
    private long capturedTick=Long.MIN_VALUE;
    private Vec3d capturedPosition;
    int texture() {return target.getColorAttachment();}
    void update() {
        var client=MinecraftClient.getInstance();var camera=client.gameRenderer.getCamera();
        long tick=client.world.getTime();
        if(target!=null && tick>=capturedTick && tick-capturedTick<10 && camera.getPos().squaredDistanceTo(capturedPosition)<1)return;
        if(target==null) {target=new SimpleFramebuffer(SIZE*6,SIZE,true,false);target.setTexFilter(GL11.GL_LINEAR);}
        float start=RenderSystem.getShaderFogStart(),end=RenderSystem.getShaderFogEnd();
        var shape=RenderSystem.getShaderFogShape();float[] fog=RenderSystem.getShaderFogColor().clone();
        boolean scissor=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);int[] box=new int[4];GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX,box);
        var projection=new Matrix4f().perspective((float)(Math.PI/2),1,.05f,2048);
        try {
            RenderSystem.disableScissor();
            target.setClearColor(fog[0],fog[1],fog[2],1);target.clear(false);target.beginWrite(true);
            for(int face=0;face<6;face++) {
                RenderSystem.viewport(face*SIZE,0,SIZE,SIZE);
                var d=DIRECTIONS[face];var u=UPS[face];
                var view=new Matrix4f().lookAt(0,0,0,d[0],d[1],d[2],u[0],u[1],u[2]);
                client.worldRenderer.renderSky(view,projection,1,camera,false,
                        () -> BackgroundRenderer.applyFog(camera,BackgroundRenderer.FogType.FOG_SKY,client.gameRenderer.getViewDistance(),false,1));
                BackgroundRenderer.applyFog(camera,BackgroundRenderer.FogType.FOG_TERRAIN,client.gameRenderer.getViewDistance(),false,1);
                client.worldRenderer.renderClouds(new MatrixStack(),view,projection,1,camera.getPos().x,camera.getPos().y,camera.getPos().z);
            }
            capturedTick=tick;capturedPosition=camera.getPos();
        } finally {
            client.getFramebuffer().beginWrite(true);
            RenderSystem.setShaderFogStart(start);RenderSystem.setShaderFogEnd(end);RenderSystem.setShaderFogShape(shape);
            RenderSystem.setShaderFogColor(fog[0],fog[1],fog[2],fog[3]);RenderSystem.setShaderColor(1,1,1,1);
            RenderSystem.depthMask(true);RenderSystem.enableDepthTest();RenderSystem.enableCull();
            RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
            if(scissor)RenderSystem.enableScissor(box[0],box[1],box[2],box[3]);
        }
    }
    @Override public void close() {if(target!=null)target.delete();}
}
