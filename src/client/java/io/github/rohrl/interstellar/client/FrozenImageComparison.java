package io.github.rohrl.interstellar.client;

import io.github.rohrl.interstellar.Interstellar;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

/** Bounded frozen comparison; screenshot/readback is excluded from paired timings. */
final class FrozenImageComparison {
    private FrozenImageComparison() {}
    static String run(TerrainScreen screen) throws Exception {
        boolean snapshots=Boolean.getBoolean("interstellar.compareMeshSnapshots");
        try(var comparison=snapshots?screen.compareMeshSnapshots():null) {return run(screen,snapshots);}
    }
    private static String run(TerrainScreen screen,boolean snapshots) throws Exception {
        var client=MinecraftClient.getInstance();int w=client.getWindow().getFramebufferWidth(),h=client.getWindow().getFramebufferHeight();
        Path path=Path.of("rtx-image","compare-"+System.currentTimeMillis());Files.createDirectories(path);
        byte[][] pixels=new byte[2][];
        try(TerrainReplay.PackState state=new TerrainReplay.PackState()) {
            for(int variant=0;variant<2;variant++) {
                screen.renderFrozenComparison(variant==1);GL11.glFinish();
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,client.getFramebuffer().fbo);
                ByteBuffer data=MemoryUtil.memAlloc(w*h*4);
                try {GL11.glReadPixels(0,0,w,h,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,data);pixels[variant]=new byte[data.capacity()];data.get(pixels[variant]);}
                finally {MemoryUtil.memFree(data);}
            }
        }
        long difference=0,squares=0;int[] histogram=new int[256];int changed=0,large=0;
        BufferedImage baseline=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB),candidate=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB),heat=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
            int at=(y*w+x)*4,a=0,b=0,diff=0,maximum=0;
            for(int channel=0;channel<3;channel++){int va=Byte.toUnsignedInt(pixels[0][at+channel]),vb=Byte.toUnsignedInt(pixels[1][at+channel]),d=Math.abs(va-vb);difference+=d;squares+=(long)d*d;maximum=Math.max(maximum,d);a=(a<<8)|va;b=(b<<8)|vb;diff=(diff<<8)|Math.min(255,d*8);}
            histogram[maximum]++;if(maximum>0)changed++;if(maximum>16)large++;
            baseline.setRGB(x,h-1-y,a);candidate.setRGB(x,h-1-y,b);heat.setRGB(x,h-1-y,diff);
        }
        ImageIO.write(baseline,"png",path.resolve("opengl.png").toFile());ImageIO.write(candidate,"png",path.resolve("rtx.png").toFile());ImageIO.write(heat,"png",path.resolve("difference-x8.png").toFile());
        double[][] wall=new double[2][16],gpu=new double[2][16];double[] vulkan=new double[16];int q0=GL15.glGenQueries(),q1=GL15.glGenQueries();
        try {
            for(int i=0;i<4;i++){screen.renderFrozenComparison(false);GL11.glFinish();screen.renderFrozenComparison(true);GL11.glFinish();}
            for(int i=0;i<16;i++)for(int order=0;order<2;order++) {
                int variant=(i+order)%2;GL11.glFinish();long start=System.nanoTime();GL33.glQueryCounter(q0,GL33.GL_TIMESTAMP);
                screen.renderFrozenComparison(variant==1);GL33.glQueryCounter(q1,GL33.GL_TIMESTAMP);GL11.glFinish();
                // GL timestamps may omit work on the external Vulkan queue. Fence both APIs;
                // keep the conservative wall time and report Vulkan's own timestamps separately.
                if(variant==1)vulkan[i]=screen.completeFrozenGpuMillis();
                wall[variant][i]=(System.nanoTime()-start)/1e6;gpu[variant][i]=(GL33.glGetQueryObjectui64(q1,GL15.GL_QUERY_RESULT)-GL33.glGetQueryObjectui64(q0,GL15.GL_QUERY_RESULT))/1e6;
            }
        } finally {GL15.glDeleteQueries(q0);GL15.glDeleteQueries(q1);}
        String report=(snapshots?"comparison=legacy voxel/height/light versus placeholders; both OpenGL; opengl.png=legacy, rtx.png=placeholders; Vulkan timing unused\n":"")+"scene="+screen.appearanceScene()+"\nquality="+screen.qualitySettings()+"\nwidth="+w+" height="+h+" camera="+screen.appearanceCamera()+" yaw="+screen.appearanceYaw()+" pitch="+screen.appearancePitch()+
            "\nRGB_mean_abs_255="+(double)difference/(w*h*3)+" RGB_RMSE_255="+Math.sqrt((double)squares/(w*h*3))+" changedPixels="+changed+" pixelsMaxErrorAbove16="+large+
            "\nmaxChannelErrorHistogram="+Arrays.toString(histogram)+"\nOpenGL_wall_ms="+Arrays.toString(wall[0])+"\nRTX_wall_ms="+Arrays.toString(wall[1])+"\nRTX_Vulkan_gpu_ms="+Arrays.toString(vulkan)+"\nOpenGL_GLtimeline_ms="+Arrays.toString(gpu[0])+"\nRTX_GLtimeline_ms="+Arrays.toString(gpu[1])+"\n";
        Files.writeString(path.resolve("comparison.txt"),report);Interstellar.LOGGER.info("RTX full-image comparison completed: {}; {}",path.toAbsolutePath(),report);
        return "RTX/OpenGL images and complete-frame timings: "+path;
    }
}
