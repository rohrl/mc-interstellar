package io.github.rohrl.interstellar.scene;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class MovingMeshTreesTest {
    @Test void forestsPreservePayloadBoundsAndVisitEveryTriangleExactlyOnce() {
        var random=new Random(731);
        for(int mode=0;mode<4;mode++) {
            int count=mode==0?0:1200;float[] data=new float[count*36];
            for(int i=0;i<count;i++)for(int v=0;v<3;v++) {
                int p=i*36+v*12;
                for(int a=0;a<12;a++)data[p+a]=random.nextFloat()*500-250;
                data[p+3]=mode==1?1:mode==2?6:i%3==0?5:i%3==1?6:8;
                data[p+4]=i;
            }
            float[] original=data.clone();var result=MovingMeshTrees.build(data,count);
            assertArrayEquals(original,data);boolean[] seen=new boolean[count];
            walk(result,0,result.actorNodes(),false,original,seen);
            walk(result,result.actorNodes(),result.actorNodes()+result.cloudNodes(),true,original,seen);
            for(boolean found:seen)assertTrue(found);
        }
    }
    private void walk(MovingMeshTrees scene,int start,int end,boolean cloud,float[] original,boolean[] seen) {
        float[] nodes=scene.nodes(),data=scene.triangles();
        if(start==end)return;
        assertEquals(end,(int)nodes[start*12+3]);
        for(int n=start;n<end;n++) {
            int p=n*12,escape=(int)nodes[p+3],first=(int)nodes[p+7],count=(int)nodes[p+8];
            assertTrue(escape>n && escape<=end);
            if(count==0) {
                int left=n+1,right=(int)nodes[left*12+3];assertTrue(right<escape);
                assertEquals(escape,(int)nodes[right*12+3]);
                for(int child:new int[]{left,right})for(int a=0;a<3;a++) {
                    assertTrue(nodes[child*12+a]>=nodes[p+a]);assertTrue(nodes[child*12+a+4]<=nodes[p+a+4]);
                }
            } else {
                assertTrue(count<=8);assertEquals(n+1,escape);
                for(int i=first;i<first+count;i++) {
                    int id=(int)data[i*36+4];assertFalse(seen[id]);seen[id]=true;
                    assertEquals(cloud,data[i*36+3]==5 || data[i*36+3]==6);
                    assertArrayEquals(Arrays.copyOfRange(original,id*36,id*36+36),Arrays.copyOfRange(data,i*36,i*36+36));
                    for(int v=0;v<3;v++)for(int a=0;a<3;a++) {
                        float value=data[i*36+v*12+a];assertTrue(value>=nodes[p+a] && value<=nodes[p+a+4]);
                    }
                }
            }
        }
    }
    @Test void rangeBuilderLeavesOutsideVerticesUntouchedAndRejectsInvalidRanges() {
        float[] data=new float[36*20];for(int i=0;i<data.length;i++)data[i]=i;
        float[] original=data.clone();new MeshTree(data,3,14,3);
        assertArrayEquals(Arrays.copyOfRange(original,0,108),Arrays.copyOfRange(data,0,108));
        assertArrayEquals(Arrays.copyOfRange(original,17*36,20*36),Arrays.copyOfRange(data,17*36,20*36));
        assertThrows(IllegalArgumentException.class,()->new MeshTree(data,-1,1,3));
        assertThrows(IllegalArgumentException.class,()->new MeshTree(data,20,1,3));
    }
}
