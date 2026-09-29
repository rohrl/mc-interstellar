package io.github.rohrl.interstellar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.rohrl.interstellar.Interstellar;
import io.github.rohrl.interstellar.science.LocalWormhole;
import io.github.rohrl.interstellar.science.FiniteTerrainRay.Point;
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
            set(shader,"MixedOptics",0);
            set(shader,"WormholeInfluence",0);
            set(shader,"MeshNodeCount",0);set(shader,"MovingNodeCount",0);set(shader,"CloudNodeCount",0);
            set(shader,"MeshClouds",0);set(shader,"PathStep",.45f);set(shader,"MeshStepLimit",16);
            vector(shader,"Source",0,0,0);vector(shader,"OtherSource",1024,16,512);
            vector(shader,"Right",1,0,0);vector(shader,"Up",0,1,0);shader.getUniformOrDefault("ViewSlopes").set(2.3f,1.3f,0f,0f);
            var pixels=BufferUtils.createFloatBuffer(W*H*4);
            var local=local(shader,draw,pixels);total+=(int)local[0];wrong+=(int)local[1];maximum=Math.max(maximum,local[2]);
            var mixed=mixed(shader,draw,pixels);total+=(int)mixed[0];wrong+=(int)mixed[1];maximum=Math.max(maximum,mixed[2]);
        } finally {
            set(shader,"Diagnostic",0);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,oldDraw);GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,oldRead);
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER,oldRenderbuffer);RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            GL30.glDeleteRenderbuffers(colour);GL30.glDeleteFramebuffers(framebuffer);
        }
        String result="World optics GPU reference: "+total+" rays, "+wrong+" mismatches; max direction error="+maximum;
        Interstellar.LOGGER.info("{}; elapsed={}ms",result,(System.nanoTime()-started)/1e6);
        if(wrong>0)throw new IllegalStateException(result);return result;
    }
    private static double[] local(ShaderProgram shader,Runnable draw,java.nio.FloatBuffer pixels) {
        var a=new Point(0,0,0);var b=new Point(128,0,0);int total=0,wrong=0;double max=0;
        var eyes=new java.util.ArrayList<Point>();var looks=new java.util.ArrayList<Point>();
        for(double x:new double[]{0,2,8,12,20,40,128}) {eyes.add(new Point(x,2,-100));looks.add(new Point(0,0,1));}
        for(double x:new double[]{57,57.5,57.59,57.599,57.601}) {eyes.add(new Point(x,0,-100));looks.add(new Point(0,0,1));}
        for(double x:new double[]{63.99,64,64.01}) {eyes.add(new Point(x,0,-100));looks.add(new Point(0,0,-1));}
        for(double x:new double[]{-64,64}) {eyes.add(new Point(64,0,-100));looks.add(new Point(x,0,100));}
        eyes.add(new Point(1,1,-6));looks.add(new Point(0,0,1));
        shader.getUniformOrDefault("ViewSlopes").set(0f,0f,0f,0f);
        TerrainScreen.setPathQuality(shader,.08f,4,2);
        for(int swap=0;swap<2;swap++)for(int i=0;i<eyes.size();i++) {
            var first=swap==0?a:b;var second=swap==0?b:a;
            vector(shader,"Source",first);vector(shader,"OtherSource",second);
            set(shader,"WormholeInfluence",(float)LocalWormhole.influence(8,128));
            var eye=eyes.get(i);var look=looks.get(i).unit();
            vector(shader,"Camera",eye);vector(shader,"Forward",look);
            var reference=LocalWormhole.reference(first,second,eye,look,8,.02);
            draw.run();pixels.clear();GL11.glReadPixels(0,0,W,H,GL11.GL_RGBA,GL11.GL_FLOAT,pixels);
            double error=new Point(pixels.get(0),pixels.get(1),pixels.get(2)).subtract(reference.direction()).length();
            boolean bad=!Double.isFinite(error) || error>.001 || pixels.get(3)!=(reference.limited()?-3:reference.passages());
            total++;max=Math.max(max,error);
            if(bad){wrong++;Interstellar.LOGGER.warn("Local wormhole GPU mismatch swap={} ray={} CPU={} GPU={},{},{},{} error={}",swap,i,reference,pixels.get(0),pixels.get(1),pixels.get(2),pixels.get(3),error);}
        }
        vector(shader,"Source",a);vector(shader,"OtherSource",0,0,-128);
        vector(shader,"Camera",0,0,-64);vector(shader,"Forward",0,0,1);
        draw.run();pixels.clear();GL11.glReadPixels(0,0,W,H,GL11.GL_RGBA,GL11.GL_FLOAT,pixels);
        total++;if(pixels.get(3)!=-3){wrong++;Interstellar.LOGGER.warn("Local wormhole loop budget: GPU={},{},{},{}",pixels.get(0),pixels.get(1),pixels.get(2),pixels.get(3));}
        Interstellar.LOGGER.info("Local wormhole Cartesian reference: {} rays, {} mismatches; max direction error={}",total,wrong,max);
        return new double[]{total,wrong,max};
    }
    private static void set(ShaderProgram s,String name,float v) {s.getUniformOrDefault(name).set(v);}
    private static double[] mixed(ShaderProgram shader,Runnable draw,java.nio.FloatBuffer pixels) {
        var a=new Point(0,0,0);var b=new Point(128,0,0);var mass=new Point(25,3,-4);
        vector(shader,"Source",a);vector(shader,"OtherSource",b);vector(shader,"MassSource",mass);
        set(shader,"MixedOptics",1);set(shader,"PortalOpen",1);set(shader,"PortalCount",2);
        int total=0,wrong=0;double maximum=0;
        for(var config:new double[][]{{0,0},{2,0},{2,4},{.5,2}})for(double x:new double[]{0,8,16,24,25,32,64,128}) {
            var model=new io.github.rohrl.interstellar.science.MixedWorld(a,b,mass,config[0],config[1]);
            var eye=new Point(x,6,-100);var look=new Point(0,0,1);
            set(shader,"MassRadius",(float)config[0]);set(shader,"MassBodyRadius",(float)config[1]);
            vector(shader,"Camera",eye);vector(shader,"Forward",look);
            var reference=model.reference(eye,look,.02);
            draw.run();pixels.clear();GL11.glReadPixels(0,0,W,H,GL11.GL_RGBA,GL11.GL_FLOAT,pixels);
            double error=reference.code()<0?0:new Point(pixels.get(0),pixels.get(1),pixels.get(2)).subtract(reference.direction()).length();
            boolean bad=!Double.isFinite(error) || error>.001 || pixels.get(3)!=reference.code();
            total++;maximum=Math.max(maximum,error);
            if(bad){wrong++;Interstellar.LOGGER.warn("Mixed GPU mismatch rs={} body={} x={} CPU={} GPU={},{},{},{} error={}",config[0],config[1],x,reference,pixels.get(0),pixels.get(1),pixels.get(2),pixels.get(3),error);}
        }
        Interstellar.LOGGER.info("Mixed world Hamiltonian/midpoint reference: {} rays, {} mismatches; max direction error={}",total,wrong,maximum);
        return new double[]{total,wrong,maximum};
    }
    private static void vector(ShaderProgram s,String name,float x,float y,float z) {s.getUniformOrDefault(name).set(x,y,z);}
    private static void vector(ShaderProgram s,String name,Point p) {vector(s,name,(float)p.x(),(float)p.y(),(float)p.z());}
}
