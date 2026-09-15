package io.github.rohrl.interstellar.client;

import io.github.rohrl.interstellar.Interstellar;
import io.github.rohrl.interstellar.config.OpticalSettings;
import io.github.rohrl.interstellar.science.CriticalRays;
import io.github.rohrl.interstellar.science.FreeFallRay;
import net.minecraft.client.gl.ShaderProgram;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** Explicit 252-ray stress test, including off-screen directions and full orbital angles. */
final class LabCriticalValidation {
    static String run(ShaderProgram shader, float radius, boolean falling, boolean lensing,
                      OpticalSettings.Quality quality, Runnable draw) {
        int previousDraw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int previousRead=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousBuffer=GL11.glGetInteger(GL30.GL_RENDERBUFFER_BINDING);
        int[] viewport=new int[4]; GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);
        int framebuffer=GL30.glGenFramebuffers(), color=GL30.glGenRenderbuffers();
        var pixels=BufferUtils.createFloatBuffer(4);
        int totalMismatch=0,totalUnresolved=0;
        try {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER,framebuffer);
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER,color);
            GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER,GL30.GL_RGBA32F,1,1);
            GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,GL30.GL_RENDERBUFFER,color);
            if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER)!=GL30.GL_FRAMEBUFFER_COMPLETE)
                throw new IllegalStateException("Critical ray framebuffer incomplete");
            GL11.glViewport(0,0,1,1);
            shader.getUniformOrDefault("Diagnostic").set(2f);
            shader.getUniformOrDefault("Lensing").set(1f);
            for (var preset : OpticalSettings.Quality.values()) {
                shader.getUniformOrDefault("IntegrationStep").set(preset.step());
                for (int scene=0;scene<7;scene++) {
                    float r=new float[]{8,1.5f,1.05f,8,1,.7067775f,.35f}[scene];
                    boolean fall=scene>=3;
                    shader.getUniformOrDefault("CameraRadius").set(r);
                    shader.getUniformOrDefault("Falling").set(fall?1f:0f);
                    double boundary=CriticalRays.cosine(r,fall);
                    double max=0; int mismatch=0,unresolved=0,turns=0,compared=0,cpuUnresolved=0;
                    for (int power=2;power<=7;power++) for (int side : new int[]{-1,1}) {
                        float mu=(float)(boundary+side*Math.pow(10,-power));
                        if (Math.abs(mu)>1) throw new IllegalStateException("Stress ray outside local sky");
                        shader.getUniformOrDefault("TargetRadial").set(mu);
                        draw.run(); pixels.clear();
                        GL11.glReadPixels(0,0,1,1,GL11.GL_RGBA,GL11.GL_FLOAT,pixels);
                        double flow=1/Math.sqrt(r);
                        var cpu=FreeFallRay.trace(r,fall?mu:(mu-flow)/(1-flow*mu),1e-12);
                        int expected=switch(cpu.outcome()) { case DARK->0; case SKY->1; case UNRESOLVED->2; };
                        float status=pixels.get(2), angle=pixels.get(3);
                        boolean invalid=!Float.isFinite(angle) || (status!=0 && status!=1 && status!=2);
                        // Budget exhaustion is distinct from a wrong resolved outcome.
                        if (expected==2) cpuUnresolved++;
                        if (invalid || (expected!=2 && status!=2 && status!=expected)) mismatch++;
                        if (status==2) unresolved++;
                        double error=Double.NaN;
                        if (!invalid && status==1 && expected==1) {
                            error=Math.abs(angle-cpu.angle()); max=Math.max(max,error); compared++;
                            if ((int)Math.floor(angle/(2*Math.PI))!=(int)Math.floor(cpu.angle()/(2*Math.PI))) turns++;
                        }
                        Interstellar.LOGGER.info("Critical ray: quality={}, r={}, falling={}, offset={}e-{}, mu={}, GPU status={}, phi={}, CPU status={}, phi={}, raw error={}",
                                preset,r,fall,side,power,mu,status,angle,expected,cpu.angle(),error);
                    }
                    totalMismatch+=mismatch; totalUnresolved+=unresolved;
                    Interstellar.LOGGER.info("Critical summary: quality={}, r={}, falling={}, wrong resolved={}, unresolved={}, escaped compared={}, raw max={} rad, turn-bin differences={}, CPU unresolved={}",
                            preset,r,fall,mismatch,unresolved,compared,max,turns,cpuUnresolved);
                }
            }
        } finally {
            shader.getUniformOrDefault("Diagnostic").set(0f);
            shader.getUniformOrDefault("CameraRadius").set(radius);
            shader.getUniformOrDefault("Falling").set(falling?1f:0f);
            shader.getUniformOrDefault("Lensing").set(lensing?1f:0f);
            shader.getUniformOrDefault("IntegrationStep").set(quality.step());
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,previousDraw);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,previousRead);
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER,previousBuffer);
            GL11.glViewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            GL30.glDeleteRenderbuffers(color); GL30.glDeleteFramebuffers(framebuffer);
        }
        return "Critical: "+totalMismatch+" wrong, "+totalUnresolved+" unresolved | details logged";
    }
}