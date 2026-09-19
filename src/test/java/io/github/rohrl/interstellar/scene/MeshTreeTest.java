package io.github.rohrl.interstellar.scene;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class MeshTreeTest {
    @Test void emptyAndCoincidentGeometryTerminate() {
        for(boolean surfaceArea:new boolean[]{false,true}) {
            assertEquals(0,new MeshTree(new float[0],0,surfaceArea).size());
            float[] triangles=new float[36*100];
            var tree=new MeshTree(triangles,100,surfaceArea);
            verify(tree.nodes(),triangles,100);
        }
    }
    @Test void randomScenePreservesEveryTriangleAndConservativeSubtreeBounds() {
        var random=new Random(1024);
        int count=1200;float[] triangles=new float[count*36];
        for(int i=0;i<count;i++) {
            for(int v=0;v<3;v++)for(int a=0;a<3;a++)triangles[i*36+v*12+a]=random.nextFloat()*500-250;
            triangles[i*36+3]=i; // Payload must stay attached when BVH construction reorders triangles.
        }
        for(boolean surfaceArea:new boolean[]{false,true}) {
            float[] reordered=triangles.clone();var tree=new MeshTree(reordered,count,surfaceArea);
            verify(tree.nodes(),reordered,count);
            boolean[] found=new boolean[count];
            for(int i=0;i<count;i++) {
                int id=(int)reordered[i*36+3];assertFalse(found[id]);found[id]=true;
                for(int a=0;a<36;a++)assertEquals(triangles[id*36+a],reordered[i*36+a]);
            }
            for(boolean value:found)assertTrue(value);
        }
    }
    @Test void planarAndSkewedScenesKeepValidBoundsAndEscapeLinks() {
        int count=2000;float[] triangles=new float[count*36];
        for(int i=0;i<count;i++)for(int v=0;v<3;v++) {
            triangles[i*36+v*12]=(i==count-1?10000:i%40)+v;
            triangles[i*36+v*12+1]=i/40;
            triangles[i*36+v*12+2]=0;
        }
        for(boolean surfaceArea:new boolean[]{false,true}) {
            float[] reordered=triangles.clone();var tree=new MeshTree(reordered,count,surfaceArea);
            verify(tree.nodes(),reordered,count);
        }
    }
    private void verify(float[] nodes,float[] triangles,int count) {
        assertEquals(nodes.length/12,(int)nodes[3]);
        boolean[] found=new boolean[count];
        for(int n=0;n<nodes.length/12;n++) {
            int base=n*12,escape=(int)nodes[base+3],first=(int)nodes[base+7],size=(int)nodes[base+8];
            assertTrue(escape>n && escape<=nodes.length/12);
            if(size>0) {
                assertEquals(n+1,escape);
                for(int i=first;i<first+size;i++) {
                    assertFalse(found[i]);found[i]=true;
                    for(int v=0;v<3;v++)for(int a=0;a<3;a++) {
                        float coordinate=triangles[i*36+v*12+a];
                        assertTrue(coordinate>=nodes[base+a] && coordinate<=nodes[base+4+a]);
                    }
                }
            } else {
                int left=n+1,right=(int)nodes[left*12+3];
                assertTrue(right<escape);assertEquals(escape,(int)nodes[right*12+3]);
                for(int child:new int[]{left,right})for(int a=0;a<3;a++) {
                    assertTrue(nodes[child*12+a]>=nodes[base+a]);
                    assertTrue(nodes[child*12+4+a]<=nodes[base+4+a]);
                }
            }
        }
        for(boolean value:found)assertTrue(value);
    }
}
