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

/** Explicit developer diagnostics. Counter readbacks block and must never be timed as production. */
final class TerrainProfile {
    static final boolean ENABLED=Boolean.getBoolean("interstellar.profile");
    static final ShaderProgram[] cloudPrograms=new ShaderProgram[3];
    static final ShaderProgram[] movingPrograms=new ShaderProgram[3];
    static final ShaderProgram[][] programs=new ShaderProgram[3][3];
    // Triangle counts are leaf entries BEFORE material/cloud rejection, not all full intersection tests.
    private static final String[] METRICS={"orbitSteps","chords","terrainNodes","movingNodes",
            "terrainTriangles","movingPrimitives","terrainCandidates","movingCandidates",
            "terrainReuse","movingReuse","terrainLeaves","movingLeaves",
            "transparentContinuations","probePending","rays","exhausted",
            "cloudPrimitives","actorTriangles","cloudCandidates","actorCandidates"};
    private static final String[] PASSES={"probe","mask","full"};
    private static final double[][] cpuTimes=new double[4][600];
    private static final int[] cpuCounts=new int[4];
    private static final String[] CPU_NAMES={"actorCapture","cloudCapture","movingTree","movingUpload"};
    private TerrainProfile() {}
    static long cpuStart() {return ENABLED?System.nanoTime():0;}
    static void cpuEnd(int stage,long start) {
        if(!ENABLED)return;
        double[] values=cpuTimes[stage];values[cpuCounts[stage]++]=(System.nanoTime()-start)/1e6;
        if(cpuCounts[stage]==values.length) {
            double mean=Arrays.stream(values).average().orElse(0);Arrays.sort(values);
            Interstellar.LOGGER.info("CPU profile stage={} samples=600 mean={} p50={} p95={} p99={} ms (wall time, includes driver waits)",CPU_NAMES[stage],mean,values[299],values[569],values[593]);
            cpuCounts[stage]=0;
        }
    }

    static void capture(int w,int h,int mask,boolean splitMoving,boolean cloudQuads,Consumer<ShaderProgram> configure,Runnable draw,String scene) {
        SimpleFramebuffer target=new SimpleFramebuffer(w*2,h,false,false);
        FloatBuffer pixels=MemoryUtil.memAllocFloat(w*2*h*4);
        int oldPack=GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
        int oldUnpack=GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        int oldTexture=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        StringBuilder csv=new StringBuilder("pass,metric,mean,p50,p95,p99,max,total,positivePixels\n");
        try {
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,0);GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D,target.getColorAttachment());
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL30.GL_RGBA32F,w*2,h,0,GL11.GL_RGBA,GL11.GL_FLOAT,(FloatBuffer)null);
            target.beginWrite(false);
            if(GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER)!=GL30.GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Profile framebuffer incomplete");
            for(int pass=0;pass<3;pass++) {
                ShaderProgram program=cloudQuads?cloudPrograms[pass]:splitMoving?movingPrograms[pass]:programs[0][pass];configure.accept(program);
                program.addSampler("PendingRays",mask);RenderSystem.setShader(()->program);
                for(int group=0;group<5;group++) {
                    target.beginWrite(false);RenderSystem.viewport(0,0,w*2,h);
                    GL11.glClearColor(0,0,0,0);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
                    program.getUniformOrDefault("ProfileMetric").set((float)group);
                    for(int sample=0;sample<2;sample++) {
                        RenderSystem.viewport(sample*w,0,w,h);
                        program.getUniformOrDefault("SampleOffset").set(sample==0?-.25f:.25f);draw.run();
                    }
                    pixels.clear();GL11.glReadPixels(0,0,w*2,h,GL11.GL_RGBA,GL11.GL_FLOAT,pixels);
                    for(int component=0;component<4;component++) {
                        float[] values=new float[w*2*h];double total=0;int positive=0;
                        for(int i=0;i<values.length;i++) {
                            float value=pixels.get(i*4+component);values[i]=value;total+=value;if(value>0)positive++;
                            if(group==3 && component==3 && value>0)Interstellar.LOGGER.warn("Profile exhausted pass={} sample={} x={} y={} (bottom-origin)",PASSES[pass],(i%(w*2))/w,i%w,i/(w*2));
                        }
                        Arrays.sort(values);int n=values.length;
                        csv.append(PASSES[pass]).append(',').append(METRICS[group*4+component]).append(',').append(total/n)
                            .append(',').append(values[(n-1)/2]).append(',').append(values[(int)(n*.95)-1])
                            .append(',').append(values[(int)(n*.99)-1]).append(',').append(values[n-1])
                            .append(',').append(total).append(',').append(positive).append('\n');
                    }
                }
            }
            Path directory=Path.of("profiles");Files.createDirectories(directory);
            Path file=directory.resolve("work-"+System.currentTimeMillis()+".csv");Files.writeString(file,csv);
            Files.writeString(Path.of(file+".txt"),scene+"\nFull-resolution counters, both AA rays; mask discarded pixels are zero; counts are not timings. Primitive counts include entries rejected before intersection; cloudQuads=true counts native cloud faces instead of triangles.\n");
            Interstellar.LOGGER.info("Shader work profile completed: {}; {}",file.toAbsolutePath(),scene);
        } catch(java.io.IOException failure) {throw new IllegalStateException("Cannot write profile",failure);}
        finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D,oldTexture);
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,oldPack);GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,oldUnpack);
            MemoryUtil.memFree(pixels);target.delete();
        }
    }
}
