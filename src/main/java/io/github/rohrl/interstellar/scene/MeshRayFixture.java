package io.github.rohrl.interstellar.scene;

import io.github.rohrl.interstellar.science.FiniteTerrainRay;
import java.util.ArrayList;
import java.util.List;

/** Diagnostic geometry only: triangles on the GPU, independent unit-cell slabs on the CPU. */
public final class MeshRayFixture implements FiniteTerrainRay.Scene {
    public static final FiniteTerrainRay.Point SOURCE=new FiniteTerrainRay.Point(320.25,320.375,320);
    public static final double RADIUS=8;
    public record Box(int x,int y,int z,int maxX,int maxY,int maxZ) {
        int min(int axis) {return axis==0?x:axis==1?y:z;}
        int max(int axis) {return axis==0?maxX:axis==1?maxY:maxZ;}
    }
    public final List<Box> boxes=List.of(
            new Box(292,300,352,348,340,353), // wall behind the source
            new Box(336,310,312,337,330,344), // side occluder
            new Box(306,314,270,312,326,271), // foreground for distant observers
            new Box(240,250,472,400,390,473)); // distant background
    public int side() {return 640;}
    public int value(int x,int y,int z) {
        for(var b:boxes)if(x>=b.x&&x<b.maxX&&y>=b.y&&y<b.maxY&&z>=b.z&&z<b.maxZ)return 3;
        return 0;
    }
    public double height(int x,int y,int z) {return 1;}
    public float[] triangles(Box box) {
        var data=new float[12*MeshTree.STRIDE];int at=0;
        for(int axis=0;axis<3;axis++)for(int sign:new int[]{-1,1}) {
            int u=(axis+1)%3,v=(axis+2)%3;
            float[][] corners=new float[4][3];
            for(int i=0;i<4;i++) {
                corners[i][axis]=sign<0?box.min(axis):box.max(axis);
                corners[i][u]=(i==1||i==2)?box.max(u):box.min(u);
                corners[i][v]=i>=2?box.max(v):box.min(v);
            }
            int[] order=sign>0?new int[]{0,1,2,0,2,3}:new int[]{0,2,1,0,3,2};
            for(int index:order) {System.arraycopy(corners[index],0,data,at,3);data[at+8]=data[at+9]=data[at+10]=data[at+11]=1;at+=12;}
        }
        return data;
    }
    public record Geometry(float[] triangles,float[] nodes,int roots) { }
    public Geometry geometry(boolean twoLevel) {
        return geometry(twoLevel,false);
    }
    public Geometry geometry(boolean twoLevel,boolean surfaceArea) {
        float[] triangles=new float[boxes.size()*12*MeshTree.STRIDE];
        if(!twoLevel) {
            for(int i=0;i<boxes.size();i++)System.arraycopy(triangles(boxes.get(i)),0,triangles,i*12*MeshTree.STRIDE,12*MeshTree.STRIDE);
            var tree=new MeshTree(triangles,boxes.size()*12,surfaceArea);
            return new Geometry(triangles,tree.nodes(),tree.size());
        }
        // Keep child addresses beyond the top-level end sentinel, as the live row arena does.
        int roots=2*boxes.size()-1,childStart=64,next=childStart;
        var parts=new ArrayList<SceneTree.Part>();var children=new ArrayList<float[]>();
        for(int i=0;i<boxes.size();i++) {
            var b=boxes.get(i);float[] local=triangles(b);var tree=new MeshTree(local,12,surfaceArea);float[] nodes=tree.nodes();
            System.arraycopy(local,0,triangles,i*12*MeshTree.STRIDE,local.length);
            parts.add(new SceneTree.Part(b.x,b.y,b.z,b.maxX,b.maxY,b.maxZ,next));
            for(int n=0;n<tree.size();n++) {
                int p=n*12;nodes[p+3]=nodes[p+3]==tree.size()?-1:nodes[p+3]+next;
                nodes[p+7]+=i*12;
            }
            children.add(nodes);next+=tree.size();
        }
        float[] nodes=new float[next*12];float[] top=new SceneTree(parts).nodes();System.arraycopy(top,0,nodes,0,top.length);
        int at=childStart*12;for(float[] child:children) {System.arraycopy(child,0,nodes,at,child.length);at+=child.length;}
        return new Geometry(triangles,nodes,roots);
    }
}
