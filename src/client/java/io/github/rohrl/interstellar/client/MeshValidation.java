package io.github.rohrl.interstellar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.rohrl.interstellar.Interstellar;
import io.github.rohrl.interstellar.scene.MeshRayFixture;
import io.github.rohrl.interstellar.science.FiniteTerrainRay;
import net.minecraft.client.gl.ShaderProgram;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Opt-in synthetic opaque geometry check. Does not certify materials or arbitrary world meshes. */
final class MeshValidation {
    private static final int W=9,H=5;
    static String run(ShaderProgram shader,Runnable draw) {
        long started=System.nanoTime();var fixture=new MeshRayFixture();
        int framebuffer=GL30.glGenFramebuffers(),colour=GL30.glGenRenderbuffers();
        int oldDraw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),oldRead=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int oldRenderbuffer=GL11.glGetInteger(GL30.GL_RENDERBUFFER_BINDING),pbo=GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
        int[] viewport=new int[4];GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);
        int[] names={GL11.GL_PACK_ALIGNMENT,GL11.GL_PACK_ROW_LENGTH,GL11.GL_PACK_SKIP_ROWS,GL11.GL_PACK_SKIP_PIXELS},saved=new int[4];
        for(int i=0;i<4;i++)saved[i]=GL11.glGetInteger(names[i]);
        int total=0,mismatches=0,inconclusive=0,unresolved=0;double invariant=0;
        try(var triangles=new MeshArena(1);var nodes=new MeshArena(1)) {
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,0);
            for(int i=0;i<4;i++)GL11.glPixelStorei(names[i],i==0?1:0);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER,framebuffer);GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER,colour);
            GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER,GL30.GL_RGBA32F,W,H);
            GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,GL30.GL_RENDERBUFFER,colour);
            if(GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER)!=GL30.GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Mesh diagnostic framebuffer incomplete");
            GL11.glViewport(0,0,W,H);
            RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableBlend();RenderSystem.setShader(()->shader);
            // All sampler names are replaced by the caller before the next ordinary draw.
            for(String name:new String[]{"Voxels","Palette","Atlas","Distant","DistantAppearance","SkyAtlas","Lightmap","LocalLight","DistantLight","SmoothAtlas","LocalSmooth","DistantSmooth"})shader.addSampler(name,triangles.texture);
            shader.addSampler("Palette",nodes.texture);
            shader.getUniformOrDefault("Viewport").set(1f,1f);
            set(shader,"MeshMode",1);set(shader,"MeshCoverage",1);set(shader,"MeshClouds",0);set(shader,"MovingNodeCount",0);
            set(shader,"Lensing",1);set(shader,"Hybrid",1);set(shader,"Diagnostic",3);set(shader,"Radius",8);
            set(shader,"MeshExtent",555);vector(shader,"Source",320.25f,320.375f,320);
            vector(shader,"Forward",0,0,1);vector(shader,"Right",1,0,0);vector(shader,"Up",0,1,0);
            var pixels=BufferUtils.createFloatBuffer(W*H*4);
            for(int distance:new int[]{32,96,148,252}) {
                float sx=32f/distance,sy=18f/distance;
                var camera=new FiniteTerrainRay.Point(320.25,320.375,320-distance);
                var reference=new FiniteTerrainRay.Result[W*H];
                int unstable=0,referenceHits=0,referenceCaptured=0;
                for(int y=0;y<H;y++)for(int x=0;x<W;x++) {
                    var direction=new FiniteTerrainRay.Point(((x+.5)/W*2-1)*sx,((y+.5)/H*2-1)*sy,1);
                    var a=FiniteTerrainRay.trace(fixture,MeshRayFixture.SOURCE,camera,direction,8,.1,1e-9);
                    var b=FiniteTerrainRay.trace(fixture,MeshRayFixture.SOURCE,camera,direction,8,.05,1e-11);
                    invariant=Math.max(invariant,b.invariantError());
                    if(a.outcome()==FiniteTerrainRay.Outcome.UNRESOLVED || b.outcome()==FiniteTerrainRay.Outcome.UNRESOLVED || !a.sameHit(b)) {unstable++;continue;}
                    reference[x+y*W]=b;
                    if(b.outcome()==FiniteTerrainRay.Outcome.HIT)referenceHits++;
                    if(b.outcome()==FiniteTerrainRay.Outcome.CAPTURED)referenceCaptured++;
                }
                inconclusive+=unstable;
                vector(shader,"Camera",320.25f,320.375f,320-distance);shader.getUniformOrDefault("ViewSlopes").set(sx,sy,0f,0f);
                for(boolean emptyCells:new boolean[]{false,true}) for(float reach:emptyCells?new float[]{16,1024}:new float[]{16}) for(boolean fastFetch:new boolean[]{false,true}) for(boolean fastBounds:new boolean[]{false,true}) for(boolean adaptive:new boolean[]{false,true}) for(boolean twoLevel:new boolean[]{false,true}) for(boolean surfaceArea:new boolean[]{false,true}) {
                    set(shader,"EmptyReach",reach);
                    set(shader,"EmptyCells",emptyCells?1:0);
                    set(shader,"FastFetch",fastFetch?1:0);
                    set(shader,"FastBounds",fastBounds?1:0);
                    set(shader,"AdaptivePath",adaptive?1:0);
                    var geometry=fixture.geometry(twoLevel,surfaceArea);
                    triangles.write(0,geometry.triangles(),geometry.triangles().length);nodes.write(0,geometry.nodes(),geometry.nodes().length);
                    set(shader,"MeshNodeCount",geometry.roots());
                    for(float step:new float[]{.45f,.225f}) {
                        set(shader,"PathStep",step);draw.run();pixels.clear();GL11.glReadPixels(0,0,W,H,GL11.GL_RGBA,GL11.GL_FLOAT,pixels);
                        int compared=0,wrong=0,failed=0;
                        for(int i=0;i<W*H;i++) {
                            var b=reference[i];int at=i*4,status=Math.round(pixels.get(at+3));
                            boolean invalid=false;for(int c=0;c<4;c++)invalid|=!Float.isFinite(pixels.get(at+c));
                            if(invalid || status==-2)failed++;
                            if(b==null)continue;compared++;
                            boolean different=invalid || status!=b.code() || status==3 && (Math.round(pixels.get(at))!=b.x() || Math.round(pixels.get(at+1))!=b.y() || Math.round(pixels.get(at+2))!=b.z());
                            if(different) {
                                if(wrong<3)Interstellar.LOGGER.info("Mesh fixture mismatch: r={}, twoLevel={}, step={}, pixel=({},{}), CPU={}, GPU=({},{},{},{})",distance,twoLevel,step,i%W,i/W,b,pixels.get(at),pixels.get(at+1),pixels.get(at+2),pixels.get(at+3));
                                wrong++;
                            }
                        }
                        total+=compared;mismatches+=wrong;unresolved+=failed;
                        Interstellar.LOGGER.info("Mesh fixture: distance={}, reach={}, emptyCells={}, fastFetch={}, fastBounds={}, adaptive={}, twoLevel={}, surfaceArea={}, pathStep={}, compared={}, mismatches={}, GPU unresolved/invalid={}, CPU inconclusive={}, hits={}, captured={}",distance,reach,emptyCells,fastFetch,fastBounds,adaptive,twoLevel,surfaceArea,step,compared,wrong,failed,unstable,referenceHits,referenceCaptured);
                    }
                }
            }
        } finally {
            set(shader,"Diagnostic",0);
            for(int i=0;i<4;i++)GL11.glPixelStorei(names[i],saved[i]);GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,pbo);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,oldDraw);GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,oldRead);
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER,oldRenderbuffer);GL11.glViewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            GL30.glDeleteRenderbuffers(colour);GL30.glDeleteFramebuffers(framebuffer);
            RenderSystem.depthMask(true);RenderSystem.enableDepthTest();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        }
        Interstellar.LOGGER.info("Mesh fixture complete: compared={}, mismatches={}, CPU inconclusive rays={}, GPU unresolved/invalid={}, max invariant={}, wall={} ms; opaque boxes, sampled cells, not arbitrary mesh/material certification",total,mismatches,inconclusive,unresolved,invariant,(System.nanoTime()-started)/1e6);
        return "Mesh fixture: "+mismatches+"/"+total+" mismatch | inconclusive "+inconclusive;
    }
    private static void set(ShaderProgram shader,String name,float value) {shader.getUniformOrDefault(name).set(value);}
    private static void vector(ShaderProgram shader,String name,float x,float y,float z) {shader.getUniformOrDefault(name).set(x,y,z);}
}
