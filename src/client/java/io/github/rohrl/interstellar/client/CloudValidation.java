package io.github.rohrl.interstellar.client;

import io.github.rohrl.interstellar.Interstellar;
import io.github.rohrl.interstellar.scene.MeshTree;
import io.github.rohrl.interstellar.scene.MovingMeshTrees;
import io.github.rohrl.interstellar.scene.QuadVertices;
import net.minecraft.client.gl.ShaderProgram;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/** Analytic native-diagonal colour/fog checks against the actual GPU cloud program. */
final class CloudValidation {
    private static final float[][] COLOURS={{1,0,0},{0,0,1},{1,1,0},{0,1,1}};
    static int run(ShaderProgram shader,Runnable draw,MeshArena terrain,MeshArena nodes,MeshArena compact,
                   MeshArena moving,MeshArena movingNodes,MeshArena movingCompact,boolean quads,boolean packed) {
        var pixel=BufferUtils.createFloatBuffer(4);int failures=0,checks=0;float worst=0;
        shader.getUniformOrDefault("Source").set(0f,0f,-100f);
        for(int axis=0;axis<3;axis++)for(int side:new int[]{-1,1})for(int kind:new int[]{5,6})
            for(int probe=0;probe<8;probe++)for(int fog=0;fog<2;fog++) {
            // Both halves, the diagonal, and a point outside the rectangular face.
            float s=probe==0?.25f:probe==1?.75f:probe==2?.5f:probe==3?1.125f:probe==4?0:probe==5?1:.375f;
            float t=probe==2?.5f:probe==6?0:probe==7?1:.625f;
            float[] camera=new float[3],forward=new float[3];camera[axis]=side>0?0:20;forward[axis]=side;
            camera[(axis+1)%3]=-2+4*t;camera[(axis+2)%3]=-2+4*s;
            shader.getUniformOrDefault("Camera").set(camera[0],camera[1],camera[2]);
            shader.getUniformOrDefault("Forward").set(forward[0],forward[1],forward[2]);
            float[] right=new float[3],up=new float[3];right[(axis+1)%3]=1;up[(axis+2)%3]=-side;
            shader.getUniformOrDefault("Right").set(right[0],right[1],right[2]);shader.getUniformOrDefault("Up").set(up[0],up[1],up[2]);
            shader.getUniformOrDefault("TerrainFogRange").set(fog==1?0f:10000f,fog==1?20f:20000f,0f);
            shader.getUniformOrDefault("TerrainFogColour").set(.3f,.4f,.5f,1f);
            // Opaque green, two-sided background on the far side of the cloud.
            float[] background=face(axis,side>0?30:-10,1000,2,false),cloud=face(axis,10,2,kind,true);
            // Isolate the cloud diagonal from the unrelated background's diagonal:
            // float rounding at a shared triangle edge can otherwise reveal black.
            for(int v=0;v<6;v++)background[v*12+(axis+1)%3]+=.75f;
            if(quads)background=QuadVertices.pack(background,2);
            var tree=new MeshTree(background,quads?1:2,quads?4:3);float[] nodeData=tree.nodes();
            terrain.write(0,background,background.length);nodes.write(0,nodeData,nodeData.length);compact.write(0,nodeData,nodeData.length);
            var forest=MovingMeshTrees.build(cloud,2,packed);float[] mv=forest.triangles(),mn=forest.nodes();
            moving.write(0,mv,mv.length);movingNodes.write(0,mn,mn.length);movingCompact.write(0,mn,mn.length);
            shader.getUniformOrDefault("MeshNodeCount").set((float)tree.size());
            shader.getUniformOrDefault("MovingNodeCount").set((float)forest.actorNodes());
            shader.getUniformOrDefault("CloudNodeCount").set((float)forest.cloudNodes());
            shader.getUniformOrDefault("CloudVertexBase").set((float)forest.cloudVertexBase());
            // This fixture is only called with the separate-root diagnostic programs.
            draw.run();pixel.clear();GL11.glReadPixels(0,0,1,1,GL11.GL_RGBA,GL11.GL_FLOAT,pixel);
            float[] expected=fog==1?new float[]{.3f,.4f,.5f}:new float[]{0,1,0};
            if(probe!=3 && (kind==6 || side>0)) {
                int[] corners=t<=s?new int[]{0,1,2}:new int[]{2,3,0};
                float[] weights=t<=s?new float[]{1-s,s-t,t}:new float[]{s,t-s,1-t};
                float distance=0;float[] colour=new float[3];
                float[][] xy={{-2,-2},{-2,2},{2,2},{2,-2}};
                for(int v=0;v<3;v++) {
                    int corner=corners[v];double du=xy[corner][0]-camera[(axis+1)%3],dv=xy[corner][1]-camera[(axis+2)%3];
                    distance+=weights[v]*(float)Math.sqrt(100+du*du+dv*dv);
                    for(int c=0;c<3;c++)colour[c]+=weights[v]*COLOURS[corner][c];
                }
                float f=fog==1?distance/20:0;f=f*f*(3-2*f);
                for(int c=0;c<3;c++)expected[c]=.5f*(colour[c]*(1-f)+(.3f+c*.1f)*f)+.5f*expected[c];
            }
            // The background is 30 blocks away, beyond the fog end in fog cases.
            float error=0;for(int c=0;c<3;c++)error=Math.max(error,Math.abs(pixel.get(c)-expected[c]));
            checks++;worst=Math.max(worst,error);
            if(!Float.isFinite(error) || error>3e-5f){failures++;Interstellar.LOGGER.error("Cloud fixture mismatch: axis={}, side={}, kind={}, probe={}, fog={}, actual=({},{},{}), expected=({},{},{}), error={}",axis,side,kind,probe,fog,pixel.get(0),pixel.get(1),pixel.get(2),expected[0],expected[1],expected[2],error);}
        }
        Interstellar.LOGGER.info("Cloud fixture complete: checks={}, failures={}, max channel error={}; native diagonal, vertex colour/fog, culling, three axes, both sides, bounds",checks,failures,worst);
        return failures;
    }
    private static float[] face(int axis,float at,float size,float kind,boolean colour) {
        float[][] xy={{-size,-size},{-size,size},{size,size},{size,-size}};int[] order={0,1,2,2,3,0};float[] data=new float[72];
        for(int v=0;v<6;v++) {
            int p=v*12,c=order[v];data[p+axis]=at;data[p+(axis+1)%3]=xy[c][0];data[p+(axis+2)%3]=xy[c][1];
            data[p+3]=kind;data[p+4]=data[p+5]=data[p+6]=data[p+7]=.5f;data[p+11]=1;
            for(int a=0;a<3;a++)data[p+8+a]=colour?COLOURS[c][a]:a==1?1:0;
        }
        return data;
    }
}
