package io.github.rohrl.interstellar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.rohrl.interstellar.Interstellar;
import io.github.rohrl.interstellar.scene.MeshTree;
import io.github.rohrl.interstellar.scene.MovingMeshTrees;
import io.github.rohrl.interstellar.scene.QuadVertices;
import net.minecraft.client.gl.ShaderProgram;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Analytic source-over fixtures in the actual GPU material path, using the caller's diagnostic FBO. */
final class MaterialValidation {
    private MaterialValidation() {}
    static int run(ShaderProgram shader,Runnable draw,MeshArena triangles,MeshArena nodes,MeshArena compact,boolean quads,boolean splitMoving) {
        int texture=GL11.glGenTextures(),oldTexture=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int pbo=GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        int[] names={GL11.GL_UNPACK_ALIGNMENT,GL11.GL_UNPACK_ROW_LENGTH,GL11.GL_UNPACK_SKIP_ROWS,GL11.GL_UNPACK_SKIP_PIXELS},saved=new int[4];
        int failures=0,checks=0;float worst=0;
        try(var movingVertices=new MeshArena(1,false,4096);var movingNodes=new MeshArena(1,false,4096);var movingCompact=new MeshArena(1,true)) {
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,0);
            for(int i=0;i<4;i++){saved[i]=GL11.glGetInteger(names[i]);GL11.glPixelStorei(names[i],i==0?4:0);}
            RenderSystem.bindTexture(texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
            var white=BufferUtils.createFloatBuffer(4).put(new float[]{1,1,1,.5f});white.flip();
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL30.GL_RGBA32F,1,1,0,GL11.GL_RGBA,GL11.GL_FLOAT,white);
            for(String name:new String[]{"Atlas","Lightmap","LocalLight","Distant"})shader.addSampler(name,texture);
            shader.addSampler("DistantAppearance",movingVertices.texture);
            shader.addSampler("DistantLight",movingNodes.texture);
            shader.addSampler("CompactMovingNodes",movingCompact.texture);
            set(shader,"Diagnostic",0);set(shader,"Lensing",0);set(shader,"RaySamples",1);set(shader,"MeshEntities",1);set(shader,"MeshClouds",1);
            set(shader,"MaterialLimit",32);set(shader,"FastBounds",1);set(shader,"FastFetch",1);set(shader,"MovingNodeCount",0);
            shader.getUniformOrDefault("Camera").set(.25f,.375f,0f);shader.getUniformOrDefault("Source").set(0f,0f,-100f);
            shader.getUniformOrDefault("ViewSlopes").set(0f,0f,0f,0f);
            shader.getUniformOrDefault("TerrainFogRange").set(10000f,20000f,0f);shader.getUniformOrDefault("TerrainFogColour").set(0f,0f,0f,0f);
            var pixels=BufferUtils.createFloatBuffer(4);
            for(int layout=0;layout<(splitMoving?4:3);layout++)for(int kind=0;kind<7;kind++)for(int reverse=0;reverse<2;reverse++)for(int cache=0;cache<2;cache++) {
                // Red at10, blue at20, opaque green at30. Cases exercise glass,
                // entity alpha, opaque occlusion and native nearest-cloud semantics.
                float[] data=new float[6*36];
                boolean grazing=kind==6;
                shader.getUniformOrDefault("Camera").set(.25f,.375f,grazing?511.9999f:0f);
                shader.getUniformOrDefault("Forward").set(grazing?1f:0f,0f,grazing?.0001f:1f);
                plane(data,reverse==0?0:4,grazing?512.004f:kind==2?5:30,0,0,1,0);
                plane(data,2,grazing?512.002f:20,kind==3 || kind==5?6:-3,0,0,1);
                plane(data,reverse==0?4:0,grazing?512:10,kind==1?8:kind==4 || kind==5?6:-3,1,0,0);
                // Exercise actual actor/cloud samplers and offsets, including empty
                // actor or cloud forests and nearest hits shared with static terrain.
                float[] staticData=new float[data.length],movingData=new float[data.length];int statics=0,movers=0;
                for(int t=0;t<6;t++) {
                    boolean dynamic=layout==1 || layout==3 || layout==2 && data[t*36+3]!=0;
                    System.arraycopy(data,t*36,dynamic?movingData:staticData,(dynamic?movers++:statics++)*36,36);
                }
                data=quads?QuadVertices.pack(staticData,statics):staticData;
                var tree=new MeshTree(data,quads?statics/2:statics,quads?4:3);var nodeData=tree.nodes();
                triangles.write(0,data,data.length);nodes.write(0,nodeData,nodeData.length);compact.write(0,nodeData,nodeData.length);
                float[] movingNodeData;
                if(splitMoving) {
                    int[] owners=null;
                    if(layout==3) {owners=new int[movers];for(int t=0;t<movers;t++)owners[t]=t/2;}
                    var forests=MovingMeshTrees.build(movingData,movers,owners);movingData=forests.triangles();movingNodeData=forests.nodes();
                    set(shader,"MovingNodeCount",forests.actorNodes());set(shader,"CloudNodeCount",forests.cloudNodes());
                } else {
                    var movingTree=new MeshTree(movingData,movers);movingNodeData=movingTree.nodes();
                    set(shader,"MovingNodeCount",movingTree.size());set(shader,"CloudNodeCount",0);
                }
                movingVertices.write(0,movingData,movers*36);movingNodes.write(0,movingNodeData,movingNodeData.length);movingCompact.write(0,movingNodeData,movingNodeData.length);
                set(shader,"MeshNodeCount",tree.size());set(shader,"EmptyCells",cache);
                draw.run();pixels.clear();GL11.glReadPixels(0,0,1,1,GL11.GL_RGBA,GL11.GL_FLOAT,pixels);
                float[] expected=kind==2?new float[]{0,1,0}:kind==5?new float[]{.5f,.5f,0}:new float[]{.5f,.25f,.25f};
                float error=0;for(int c=0;c<3;c++)error=Math.max(error,Math.abs(pixels.get(c)-expected[c]));
                checks++;worst=Math.max(worst,error);
                if(!Float.isFinite(error) || error>2e-5f) {failures++;Interstellar.LOGGER.error("Material fixture mismatch: layout={}, case={}, reverse={}, cache={}, actual=({},{},{}), error={}",layout,kind,reverse,cache,pixels.get(0),pixels.get(1),pixels.get(2),error);}
            }
        } finally {
            for(int i=0;i<4;i++)GL11.glPixelStorei(names[i],saved[i]);GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,pbo);
            RenderSystem.bindTexture(oldTexture);RenderSystem.deleteTexture(texture);
        }
        Interstellar.LOGGER.info("Material fixture complete: checks={}, failures={}, max channel error={}; ordered alpha, opaque occlusion, two-sided entity, cloud ordering/depth, reversed input and empty-cache variants",checks,failures,worst);
        return failures;
    }
    private static void set(ShaderProgram shader,String name,float value) {shader.getUniformOrDefault(name).set(value);}
    private static void plane(float[] data,int triangle,float z,float material,float r,float g,float b) {
        float[][] corners={{-1000,-1000},{-1000,1000},{1000,1000},{1000,-1000}};int[] indices={0,1,2,2,3,0};
        for(int i=0;i<6;i++) {int p=triangle*36+i*12;var corner=corners[indices[i]];data[p]=corner[0];data[p+1]=corner[1];data[p+2]=z;data[p+3]=material;
            data[p+4]=data[p+5]=.5f;data[p+6]=data[p+7]=.5f;data[p+8]=r;data[p+9]=g;data[p+10]=b;data[p+11]=1;}
    }
}
