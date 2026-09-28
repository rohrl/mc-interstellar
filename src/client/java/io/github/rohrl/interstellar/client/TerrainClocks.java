package io.github.rohrl.interstellar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.rohrl.interstellar.Interstellar;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.SimpleFramebuffer;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Consumer;

/** Developer-only invocation latency attribution; never used in timed production draws. */
final class TerrainClocks {
    static final boolean ENABLED=Boolean.getBoolean("interstellar.shaderClocks");
    // ordinary/horizon frame, query-only/detailed instrumentation, probe/mask pass
    static final ShaderProgram[][][] PROGRAMS=new ShaderProgram[2][2][2];
    static boolean supported() {return GL.getCapabilities().GL_ARB_shader_clock;}
    private TerrainClocks() {}
    static void capture(int w,int h,int mask,boolean horizon,ShaderProgram[] baseline,
                        Consumer<ShaderProgram> configure,Runnable draw,String scene) {
        if(!supported())throw new IllegalStateException("GL_ARB_shader_clock unavailable");
        SimpleFramebuffer target=new SimpleFramebuffer(w*2,h,false,false);
        FloatBuffer reference=MemoryUtil.memAllocFloat(w*2*h*4),pixels=MemoryUtil.memAllocFloat(w*2*h*4);
        int oldPack=GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING),oldUnpack=GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        int oldTexture=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int[] queries={GL15.glGenQueries(),GL15.glGenQueries()};
        StringBuilder csv=new StringBuilder("pass,detail,repeat,activeRays,totalTicks,queryInclusiveTicks,geometrySearchTicks,candidateShadeTicks,orbitStepTicks,otherTicks,invalidRays,gpuMs\n");
        StringBuilder checks=new StringBuilder(scene+"\nClock units are undefined and local to an invocation. Shares are latency attribution, not GPU elapsed-time percentages.\n");
        try {
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,0);GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D,target.getColorAttachment());
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL30.GL_RGBA32F,w*2,h,0,GL11.GL_RGBA,GL11.GL_FLOAT,(FloatBuffer)null);
            target.beginWrite(false);
            if(GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER)!=GL30.GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Clock framebuffer incomplete");
            for(int pass=0;pass<2;pass++) {
                ShaderProgram[] variants={baseline[pass],PROGRAMS[horizon?1:0][0][pass],PROGRAMS[horizon?1:0][1][pass]};
                double[][] times=new double[3][7];
                // Same compiled clock program can emit colours for output/overhead comparisons.
                for(int iteration=0;iteration<10;iteration++)for(int order=0;order<3;order++) {
                    int variant=iteration%2==0?order:2-order;
                    double ms=render(variants[variant],0,w,h,mask,target,configure,draw,queries);
                    if(iteration>=3)times[variant][iteration-3]=ms;
                }
                for(double[] row:times)Arrays.sort(row);
                render(variants[0],0,w,h,mask,target,configure,draw,queries);read(reference,w,h);
                for(int detail=0;detail<2;detail++) {
                    ShaderProgram program=variants[detail+1];
                    render(program,0,w,h,mask,target,configure,draw,queries);read(pixels,w,h);
                    long different=0,overTolerance=0;double maximum=0;
                    for(int i=0;i<pixels.capacity();i++) {
                        float a=reference.get(i),b=pixels.get(i);double error=Math.abs(a-b);
                        if(!Float.isFinite(a)||!Float.isFinite(b))throw new IllegalStateException("Nonfinite clock validation colour");
                        if(a!=b)different++;if(error>1e-5)overTolerance++;maximum=Math.max(maximum,error);
                    }
                    checks.append("pass=").append(pass).append(" detail=").append(detail)
                        .append(" baselineGpuMedianMs=").append(times[0][3]).append(" instrumentedColourGpuMedianMs=").append(times[detail+1][3])
                        .append(" differentComponents=").append(different).append(" componentsAbove1e-5=").append(overTolerance)
                        .append(" maxColourError=").append(maximum).append('\n');
                    for(int repeat=0;repeat<3;repeat++) {
                        double ms=render(program,1,w,h,mask,target,configure,draw,queries);read(pixels,w,h);
                        double[] sum=new double[4];long active=0,invalid=0;
                        for(int i=0;i<w*2*h;i++) {
                            double query=pixels.get(i*4),shade=pixels.get(i*4+1),orbit=pixels.get(i*4+2),total=pixels.get(i*4+3);
                            if(total==0)continue;active++;
                            if(!Double.isFinite(total)||!Double.isFinite(query)||!Double.isFinite(shade)||!Double.isFinite(orbit)
                                    ||query<0||shade<0||orbit<0||query<shade-32||total<query+orbit-32||total<0)invalid++;
                            sum[0]+=query;sum[1]+=shade;sum[2]+=orbit;sum[3]+=total;
                        }
                        csv.append(pass==0?"probe":"mask").append(',').append(detail).append(',').append(repeat).append(',').append(active)
                            .append(',').append(sum[3]).append(',').append(sum[0]).append(',').append(sum[0]-sum[1])
                            .append(',').append(sum[1]).append(',').append(sum[2]).append(',').append(sum[3]-sum[0]-sum[2])
                            .append(',').append(invalid).append(',').append(ms).append('\n');
                    }
                }
            }
            Path directory=Path.of("profiles");Files.createDirectories(directory);
            Path file=directory.resolve("clocks-"+System.currentTimeMillis()+".csv");Files.writeString(file,csv);
            Files.writeString(Path.of(file+".txt"),checks);
            Interstellar.LOGGER.info("Shader clock profile completed: {}; {}",file.toAbsolutePath(),checks);
        } catch(java.io.IOException failure) {throw new IllegalStateException("Cannot write shader clock profile",failure);}
        finally {
            GL15.glDeleteQueries(queries[0]);GL15.glDeleteQueries(queries[1]);GL11.glBindTexture(GL11.GL_TEXTURE_2D,oldTexture);
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,oldPack);GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,oldUnpack);
            MemoryUtil.memFree(reference);MemoryUtil.memFree(pixels);target.delete();
        }
    }
    private static void read(FloatBuffer buffer,int w,int h) {
        buffer.clear();GL11.glReadPixels(0,0,w*2,h,GL11.GL_RGBA,GL11.GL_FLOAT,buffer);
    }
    private static double render(ShaderProgram program,float output,int w,int h,int mask,SimpleFramebuffer target,
                                 Consumer<ShaderProgram> configure,Runnable draw,int[] queries) {
        configure.accept(program);program.addSampler("PendingRays",mask);
        program.getUniformOrDefault("ClockOutput").set(output);RenderSystem.setShader(()->program);
        target.beginWrite(false);RenderSystem.viewport(0,0,w*2,h);GL11.glClearColor(0,0,0,0);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        GL33.glQueryCounter(queries[0],GL33.GL_TIMESTAMP);
        for(int sample=0;sample<2;sample++) {
            RenderSystem.viewport(sample*w,0,w,h);program.getUniformOrDefault("SampleOffset").set(sample==0?-.25f:.25f);draw.run();
        }
        GL33.glQueryCounter(queries[1],GL33.GL_TIMESTAMP);
        long end=GL33.glGetQueryObjectui64(queries[1],GL15.GL_QUERY_RESULT),start=GL33.glGetQueryObjectui64(queries[0],GL15.GL_QUERY_RESULT);
        return (end-start)/1e6;
    }
}
