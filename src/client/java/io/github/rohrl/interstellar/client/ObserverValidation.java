package io.github.rohrl.interstellar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.rohrl.interstellar.Interstellar;
import io.github.rohrl.interstellar.science.SpecialRelativity;
import io.github.rohrl.interstellar.science.FiniteTerrainRay.Point;
import net.minecraft.client.gl.ShaderProgram;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Actual float shader boost versus an independent double photon four-vector transform. */
final class ObserverValidation {
    static String run(ShaderProgram shader,Runnable draw) {
        int fb=GL30.glGenFramebuffers(),colour=GL30.glGenRenderbuffers();
        int oldDraw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),oldRead=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int oldRb=GL11.glGetInteger(GL30.GL_RENDERBUFFER_BINDING);int[] viewport=new int[4];GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);
        int count=0;double worst=0;
        try(var pack=new TerrainReplay.PackState()) {
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,0);
            for(int name:new int[]{GL11.GL_PACK_ROW_LENGTH,GL11.GL_PACK_SKIP_PIXELS,GL11.GL_PACK_SKIP_ROWS})GL11.glPixelStorei(name,0);
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT,1);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER,fb);GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER,colour);
            GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER,GL30.GL_RGBA32F,1,1);
            GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,GL30.GL_RENDERBUFFER,colour);
            if(GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER)!=GL30.GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Observer framebuffer incomplete");
            RenderSystem.viewport(0,0,1,1);RenderSystem.disableDepthTest();RenderSystem.disableBlend();RenderSystem.setShader(()->shader);
            shader.getUniformOrDefault("Diagnostic").set(5f);shader.getUniformOrDefault("ViewSlopes").set(0f,0f,0f,0f);
            var pixel=BufferUtils.createFloatBuffer(4);
            var axis=new Point(.6,0,.8);
            for(double beta:new double[]{0,.1,.5,.9,.99})for(var n:new Point[]{axis,axis.scale(-1),new Point(0,1,0),new Point(-.8,0,.6),new Point(1,2,3).unit()})for(boolean aberration:new boolean[]{false,true}) {
                var velocity=axis.scale(beta);vector(shader,"Forward",n);vector(shader,"ObserverVelocity",velocity);
                shader.getUniformOrDefault("ObserverEffects").set(aberration?1f:0f,0f,0f);
                var reference=aberration?SpecialRelativity.toBaseline(n,velocity):new SpecialRelativity.Ray(n,SpecialRelativity.lorentzFactor(beta)*(1+velocity.dot(n)));
                draw.run();pixel.clear();GL11.glReadPixels(0,0,1,1,GL11.GL_RGBA,GL11.GL_FLOAT,pixel);
                var actual=new Point(pixel.get(0),pixel.get(1),pixel.get(2));
                double error=Math.max(actual.subtract(reference.direction()).length(),Math.abs(pixel.get(3)-reference.doppler())/reference.doppler());
                if(!Double.isFinite(error)||error>1e-4)throw new IllegalStateException("Observer fixture mismatch: beta="+beta+", aberration="+aberration+", error="+error);
                worst=Math.max(worst,error);count++;
            }
        } finally {
            shader.getUniformOrDefault("Diagnostic").set(0f);vector(shader,"ObserverVelocity",new Point(0,0,0));
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,oldDraw);GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,oldRead);
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER,oldRb);GL30.glDeleteFramebuffers(fb);GL30.glDeleteRenderbuffers(colour);
            RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);RenderSystem.depthMask(true);
        }
        String result="Observer fixture: "+count+" rays passed; maximum direction/relative Doppler error="+worst;
        Interstellar.LOGGER.info(result);return result;
    }
    private static void vector(ShaderProgram shader,String name,Point p){shader.getUniformOrDefault(name).set((float)p.x(),(float)p.y(),(float)p.z());}
}
