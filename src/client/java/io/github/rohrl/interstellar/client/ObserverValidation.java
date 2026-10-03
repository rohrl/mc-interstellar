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
        int count=0,colours=0,disks=0,seams=0;double worst=0;
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
            // Display contracts, not a claim that the assumed spectra are measured
            // material properties. Exercise the same radiance code as GL and RTX.
            shader.getUniformOrDefault("Diagnostic").set(6f);vector(shader,"Forward",axis);
            for(double beta:new double[]{0,.99,-.99})for(float shift:new float[]{0,.06f,1})for(boolean brightness:new boolean[]{false,true})for(var input:new Point[]{new Point(0,0,0),new Point(.2,.5,.8),new Point(1,1,1)}) {
                vector(shader,"ObserverVelocity",axis.scale(beta));vector(shader,"Source",input);
                shader.getUniformOrDefault("ObserverEffects").set(1f,shift,brightness?1f:0f);
                draw.run();pixel.clear();GL11.glReadPixels(0,0,1,1,GL11.GL_RGBA,GL11.GL_FLOAT,pixel);
                var actual=new Point(pixel.get(0),pixel.get(1),pixel.get(2));
                double peak=Math.max(actual.x(),Math.max(actual.y(),actual.z())),inputPeak=Math.max(input.x(),Math.max(input.y(),input.z()));
                boolean valid=Double.isFinite(peak)&&actual.x()>=0&&actual.y()>=0&&actual.z()>=0&&peak<=1.00001;
                if(beta==0 || shift==0&&!brightness)valid&=actual.subtract(input).length()<1e-6;
                if(inputPeak==0)valid&=peak==0;
                if(shift>0)valid&=peak>=.06*inputPeak-1e-6;
                if(Math.abs(beta)==.99 && shift==1 && input.x()==1) {
                    valid&=peak>=.05999&&peak<=.061;
                    valid&=beta>0?actual.z()>actual.x():actual.x()>actual.z();
                }
                if(!valid)throw new IllegalStateException("Observer colour contract: beta="+beta+", shift="+shift+", brightness="+brightness+", input="+input+", actual="+actual);
                colours++;
            }
            shader.getUniformOrDefault("Diagnostic").set(7f);
            shader.getUniformOrDefault("DiskSource").set(0f,0f,0f,1f);
            shader.getUniformOrDefault("DiskAxis").set(0f,1f,0f);
            shader.getUniformOrDefault("DiskSettings").set(10f,1f,0f,7500f);
            shader.getUniformOrDefault("Lensing").set(1f);
            vector(shader,"Camera",new Point(0,100,0));
            for(double r:new double[]{2,3.5,5,9,11})for(double z:new double[]{-2,0,2}) {
                var start=new Point(r,1,z);var end=new Point(r,-1,-z);
                vector(shader,"Source",start);vector(shader,"Forward",end);
                double t=io.github.rohrl.interstellar.science.AccretionDisk.crossing(start,end,new Point(0,1,0),10);
                // At (r,0,0), orbital tangent is -Z; chord has no radial part.
                double shift=t<0?0:io.github.rohrl.interstellar.science.AccretionDisk.staticShift(r,100,z/Math.sqrt(1+z*z));
                draw.run();pixel.clear();GL11.glReadPixels(0,0,1,1,GL11.GL_RGBA,GL11.GL_FLOAT,pixel);
                double error=Math.max(Math.abs(pixel.get(0)-t),Math.abs(pixel.get(1)-shift));
                if(!Double.isFinite(error)||error>1e-4)throw new IllegalStateException("Disk fixture mismatch: r="+r+", z="+z+", error="+error);
                worst=Math.max(worst,error);disks++;
            }
            // Shared noise corners must remain continuous after long animation
            // histories and on either side of negative-coordinate grid lines.
            shader.getUniformOrDefault("Diagnostic").set(8f);
            for(int seed:new int[]{0,4096,65536})for(int cell:new int[]{-19,-1,0,1,23})for(boolean vertical:new boolean[]{false,true}) {
                vector(shader,"Forward",new Point(seed,0,0));
                Point previous=null;
                for(double side:new double[]{-1,1}) {
                    double across=cell+side*.0001;
                    vector(shader,"Source",vertical?new Point(.371,across,0):new Point(across,.371,0));
                    draw.run();pixel.clear();GL11.glReadPixels(0,0,1,1,GL11.GL_RGBA,GL11.GL_FLOAT,pixel);
                    var actual=new Point(pixel.get(0),pixel.get(1),pixel.get(2));
                    if(previous!=null && (!Double.isFinite(actual.length())||actual.subtract(previous).length()>.002))
                        throw new IllegalStateException("Disk noise seam: seed="+seed+", cell="+cell+", vertical="+vertical);
                    previous=actual;
                }
                seams++;
            }
        } finally {
            shader.getUniformOrDefault("DiskSource").set(0f,0f,0f,0f);
            shader.getUniformOrDefault("Diagnostic").set(0f);vector(shader,"ObserverVelocity",new Point(0,0,0));
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,oldDraw);GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,oldRead);
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER,oldRb);GL30.glDeleteFramebuffers(fb);GL30.glDeleteRenderbuffers(colour);
            RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);RenderSystem.depthMask(true);
        }
        String result="Observer fixture: "+count+" rays, "+colours+" colour contracts, "+disks+" disk cases and "+seams+" noise seams passed; maximum error="+worst;
        Interstellar.LOGGER.info(result);return result;
    }
    private static void vector(ShaderProgram shader,String name,Point p){shader.getUniformOrDefault(name).set((float)p.x(),(float)p.y(),(float)p.z());}
}
