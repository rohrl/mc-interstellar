package io.github.rohrl.interstellar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.rohrl.interstellar.Interstellar;
import io.github.rohrl.interstellar.science.EllisWormhole;
import net.minecraft.client.gl.ShaderProgram;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Checks the actual GPU optical solver against a much finer independent CPU trajectory.
 * Does not certify materials or finite mesh intersections; those have separate image checks. */
final class WormholeValidation {
    private static final int W=13,H=9;
    static String run(ShaderProgram shader,Runnable draw) {
        int framebuffer=GL30.glGenFramebuffers(),colour=GL30.glGenRenderbuffers();
        int oldDraw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),oldRead=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int oldRenderbuffer=GL11.glGetInteger(GL30.GL_RENDERBUFFER_BINDING);
        int[] viewport=new int[4];GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);
        long started=System.nanoTime();int total=0,wrong=0;double maximum=0;
        try(var pack=new TerrainReplay.PackState()) {
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,0);
            for(int name:new int[]{GL11.GL_PACK_ROW_LENGTH,GL11.GL_PACK_SKIP_PIXELS,GL11.GL_PACK_SKIP_ROWS})GL11.glPixelStorei(name,0);
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT,1);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER,framebuffer);GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER,colour);
            GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER,GL30.GL_RGBA32F,W,H);
            GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,GL30.GL_RENDERBUFFER,colour);
            if(GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER)!=GL30.GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Wormhole fixture framebuffer incomplete");
            RenderSystem.viewport(0,0,W,H);RenderSystem.disableDepthTest();RenderSystem.disableBlend();RenderSystem.setShader(()->shader);
            set(shader,"Diagnostic",4);set(shader,"Lensing",1);set(shader,"Radius",16);set(shader,"WormholeExtent",768);
            set(shader,"MeshNodeCount",0);set(shader,"MovingNodeCount",0);set(shader,"CloudNodeCount",0);
            set(shader,"MeshClouds",0);set(shader,"PathStep",.45f);set(shader,"MeshStepLimit",16);
            vector(shader,"Source",0,0,0);vector(shader,"OtherSource",1024,16,512);
            vector(shader,"Right",1,0,0);vector(shader,"Up",0,1,0);shader.getUniformOrDefault("ViewSlopes").set(2.3f,1.3f,0f,0f);
            var pixels=BufferUtils.createFloatBuffer(W*H*4);
            for(float radius:new float[]{.25f,.5f,1,4,7.99f,8,8.01f,12,32,96,256})for(int facing:new int[]{-1,1}) {
                vector(shader,"Camera",0,0,-radius);vector(shader,"Forward",0,0,facing);
                TerrainScreen.setPathQuality(shader,.08f,4,Math.max(radius,64/radius)/16.0);
                draw.run();pixels.clear();GL11.glReadPixels(0,0,W,H,GL11.GL_RGBA,GL11.GL_FLOAT,pixels);
                for(int y=0;y<H;y++)for(int x=0;x<W;x++) {
                    double dx=((x+.5)/W*2-1)*2.3f,dy=((y+.5)/H*2-1)*1.3f;
                    var reference=reference(radius,dx,dy,facing);int at=(x+y*W)*4;
                    double error=0;boolean bad=pixels.get(at+3)!=reference[3];
                    for(int c=0;c<3;c++) {double d=pixels.get(at+c)-reference[c];error+=d*d;bad|=!Float.isFinite(pixels.get(at+c));}
                    error=Math.sqrt(error);maximum=Math.max(maximum,error);bad|=error>.001;
                    total++;if(bad) {wrong++;if(wrong<=8)Interstellar.LOGGER.warn("Ellis GPU mismatch R={} facing={} pixel={},{} CPU={} GPU={},{},{},{} error={}",radius,facing,x,y,java.util.Arrays.toString(reference),pixels.get(at),pixels.get(at+1),pixels.get(at+2),pixels.get(at+3),error);}
                }
            }
            // Exact unstable circular null orbit on the throat.
            vector(shader,"Camera",0,0,-8);vector(shader,"Forward",1,0,0);shader.getUniformOrDefault("ViewSlopes").set(0f,0f,0f,0f);
            draw.run();pixels.clear();GL11.glReadPixels(0,0,W,H,GL11.GL_RGBA,GL11.GL_FLOAT,pixels);
            total++;if(pixels.get(3)!=2)wrong++;
        } finally {
            set(shader,"Diagnostic",0);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,oldDraw);GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,oldRead);
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER,oldRenderbuffer);RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            GL30.glDeleteRenderbuffers(colour);GL30.glDeleteFramebuffers(framebuffer);
        }
        String result="Ellis GPU reference: "+total+" rays, "+wrong+" mismatches; max direction error="+maximum;
        Interstellar.LOGGER.info("{}; elapsed={}ms",result,(System.nanoTime()-started)/1e6);
        if(wrong>0)throw new IllegalStateException(result);return result;
    }
    private static double[] reference(double radius,double dx,double dy,int facing) {
        var metric=new EllisWormhole(1);double r=radius/16,ell=r-.25/r;
        double length=Math.sqrt(dx*dx+dy*dy+1),mu=-facing/length,sine=Math.hypot(dx,dy)/length;
        double impact=Math.sqrt(1+ell*ell)*sine;var q=new EllisWormhole.Ray(ell,mu,0);
        boolean escaped=false;
        for(int i=0;i<200000;i++) {
            q=metric.step(q,impact,.0005*Math.max(1,Math.abs(q.ell())));
            if(Math.abs(q.ell())>2048 && q.ell()*q.radialMomentum()>0) {escaped=true;break;}
        }
        if(!escaped)throw new IllegalStateException("Ellis CPU reference did not escape");
        double angle=q.angle()+impact/Math.abs(q.ell()),transverse=Math.hypot(dx,dy);
        return new double[]{transverse==0?0:Math.sin(angle)*dx/transverse,transverse==0?0:Math.sin(angle)*dy/transverse,
            (q.ell()<0?1:-1)*Math.cos(angle),q.ell()<0?1:0};
    }
    private static void set(ShaderProgram s,String name,float v) {s.getUniformOrDefault(name).set(v);}
    private static void vector(ShaderProgram s,String name,float x,float y,float z) {s.getUniformOrDefault(name).set(x,y,z);}
}
