package io.github.rohrl.interstellar.scene;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class MeshTreeTest {
    @Test void emptyAndCoincidentGeometryTerminate() {
        assertEquals(0,new MeshTree(new float[0],0).size());
        float[] triangles=new float[36*100];
        var tree=new MeshTree(triangles,100);
        verify(tree.nodes(),triangles,100);
    }
    @Test void randomScenePreservesEveryTriangleAndConservativeSubtreeBounds() {
        var random=new Random(1024);
        int count=1200;float[] triangles=new float[count*36];
        for(int i=0;i<count;i++) {
            for(int v=0;v<3;v++)for(int a=0;a<3;a++)triangles[i*36+v*12+a]=random.nextFloat()*500-250;
            triangles[i*36+3]=i; // Payload must stay attached when BVH construction reorders triangles.
        }
        var tree=new MeshTree(triangles,count);
        verify(tree.nodes(),triangles,count);
        boolean[] found=new boolean[count];
        for(int i=0;i<count;i++) {int id=(int)triangles[i*36+3];assertFalse(found[id]);found[id]=true;}
        for(boolean value:found)assertTrue(value);
    }
    private void verify(float[] nodes,float[] triangles,int count) {
        verify(nodes,triangles,count,3);
    }
    @Test void quadTreeKeepsNonplanarFourthCornersAndEveryPayload() {
        var random=new Random(721);int count=1200;var data=new float[count*48];
        for(int q=0;q<count;q++) {
            for(int v=0;v<4;v++)for(int axis=0;axis<3;axis++)data[q*48+v*12+axis]=random.nextFloat()*500-250;
            data[q*48+3]=q;
        }
        var tree=new MeshTree(data,count,4);verify(tree.nodes(),data,count,4);
        boolean[] seen=new boolean[count];
        for(int q=0;q<count;q++){int id=(int)data[q*48+3];assertFalse(seen[id]);seen[id]=true;}
        for(boolean value:seen)assertTrue(value);
    }
    private void verify(float[] nodes,float[] triangles,int count,int vertices) {
        assertEquals(nodes.length/12,(int)nodes[3]);
        boolean[] found=new boolean[count];
        for(int n=0;n<nodes.length/12;n++) {
            int base=n*12,escape=(int)nodes[base+3],first=(int)nodes[base+7],size=(int)nodes[base+8];
            assertTrue(escape>n && escape<=nodes.length/12);
            if(size>0) {
                assertTrue(size<=(vertices==4?4:8));
                assertEquals(n+1,escape);
                for(int i=first;i<first+size;i++) {
                    assertFalse(found[i]);found[i]=true;
                    for(int v=0;v<vertices;v++)for(int a=0;a<3;a++) {
                        float coordinate=triangles[i*vertices*12+v*12+a];
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
