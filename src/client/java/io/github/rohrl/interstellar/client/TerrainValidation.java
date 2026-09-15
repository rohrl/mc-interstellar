package io.github.rohrl.interstellar.client;

import io.github.rohrl.interstellar.Interstellar;
import io.github.rohrl.interstellar.science.BoxRay;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;

/** Compares GPU straight-ray hits to a brute-force independent slab reference; then audits off-screen hits. */
final class TerrainValidation {
    static String run(ShaderProgram shader,TerrainSnapshot scene,Vec3d camera,Vec3d forward,Vec3d right,Vec3d up,
                      double aspect,boolean lensing,Runnable draw) {
        int rayBudget=Math.min(1536,12_000_000/Math.max(1,scene.occupied.size()));
        final int w=Math.min(48,Math.max(1,(int)Math.sqrt(rayBudget*1.5)));
        final int h=Math.min(32,Math.max(1,rayBudget/w));
        int[] packNames={GL11.GL_PACK_ALIGNMENT,GL11.GL_PACK_ROW_LENGTH,GL11.GL_PACK_SKIP_ROWS,GL11.GL_PACK_SKIP_PIXELS};
        int[] packSaved=new int[4];for(int i=0;i<4;i++)packSaved[i]=GL11.glGetInteger(packNames[i]);
        int packBuffer=GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
        int previousDraw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),previousRead=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousBuffer=GL11.glGetInteger(GL30.GL_RENDERBUFFER_BINDING);
        int[] viewport=new int[4];GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);
        int framebuffer=GL30.glGenFramebuffers(),color=GL30.glGenRenderbuffers();
        var pixels=BufferUtils.createFloatBuffer(w*h*4);
        int mismatch=0,flatHits=0,lensedHits=0,outside=0,unresolved=0;
        try {
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,0);
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT,1);
            for(int i=1;i<4;i++)GL11.glPixelStorei(packNames[i],0);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER,framebuffer);GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER,color);
            GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER,GL30.GL_RGBA32F,w,h);
            GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,GL30.GL_RENDERBUFFER,color);
            if(GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER)!=GL30.GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Terrain diagnostic framebuffer incomplete");
            GL11.glViewport(0,0,w,h);shader.getUniformOrDefault("Diagnostic").set(1f);
            // Float camera uniforms are the actual GPU inputs; compare against those values.
            camera=new Vec3d((float)camera.x,(float)camera.y,(float)camera.z);
            for(int mode=0;mode<2;mode++) {
                shader.getUniformOrDefault("Lensing").set((float)mode);draw.run();pixels.clear();
                GL11.glReadPixels(0,0,w,h,GL11.GL_RGBA,GL11.GL_FLOAT,pixels);
                for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                    int offset=4*(x+y*w),status=Math.round(pixels.get(offset+3));
                    int gx=Math.round(pixels.get(offset)),gy=Math.round(pixels.get(offset+1)),gz=Math.round(pixels.get(offset+2));
                    if(mode==0) {
                        // glReadPixels starts at the bottom; screenUv.y starts at the top.
                        double sx=((x+.5)/w*2-1)*.7002075382*aspect,sy=((y+.5)/h*2-1)*.7002075382;
                        Vec3d direction=forward.add(right.multiply(sx)).add(up.multiply(sy)).normalize();
                        double nearest=Double.POSITIVE_INFINITY;int best=-1;
                        for(int index:scene.occupied) {
                            int bx=index%TerrainSnapshot.SIDE,bz=(index/TerrainSnapshot.SIDE)%TerrainSnapshot.SIDE,by=index/(TerrainSnapshot.SIDE*TerrainSnapshot.SIDE);
                            double t=BoxRay.entry(camera.x,camera.y,camera.z,direction.x,direction.y,direction.z,bx,by,bz,scene.height(index));
                            if(t<nearest) {nearest=t;best=index;}
                        }
                        if(best>=0)flatHits++;
                        int gpuIndex=gx+gz*TerrainSnapshot.SIDE+gy*TerrainSnapshot.SIDE*TerrainSnapshot.SIDE;
                        if(best<0?status!=0:status!=scene.value(best)||gpuIndex!=best) {
                            if(mismatch<5) Interstellar.LOGGER.info("Terrain mismatch pixel=({},{}), expected index={} t={}, GPU cell=({},{},{}) value={}",x,y,best,nearest,gx,gy,gz,status);
                            mismatch++;
                        }
                    } else {
                        if(status==-2)unresolved++;
                        if(status>=3) {
                            lensedHits++;
                            Vec3d d=new Vec3d(gx+.5,gy+.5,gz+.5).subtract(camera);
                            double depth=d.dotProduct(forward);
                            // Two-block margin avoids counting cells straddling the ordinary FOV edge.
                            if(depth<-2 || Math.abs(d.dotProduct(right))>depth*.7002075382*aspect+2 ||
                                    Math.abs(d.dotProduct(up))>depth*.7002075382+2)outside++;
                        }
                    }
                }
            }
        } finally {
            for(int i=0;i<4;i++)GL11.glPixelStorei(packNames[i],packSaved[i]);
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,packBuffer);
            shader.getUniformOrDefault("Diagnostic").set(0f);shader.getUniformOrDefault("Lensing").set(lensing?1f:0f);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,previousDraw);GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,previousRead);
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER,previousBuffer);GL11.glViewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            GL30.glDeleteRenderbuffers(color);GL30.glDeleteFramebuffers(framebuffer);
        }
        Interstellar.LOGGER.info("Terrain diagnostic: {}x{}, aspect={}, flat mismatches={}, flat hits={}, lensed opaque hits={}, outside ordinary FOV with margin={}, lensed unresolved={}; flat reference brute-force cube slabs, not curved-ray validation",
                w,h,aspect,mismatch,flatHits,lensedHits,outside,unresolved);
        return "Flat check: "+mismatch+" mismatches | off-screen lensed hits: "+outside;
    }
}
