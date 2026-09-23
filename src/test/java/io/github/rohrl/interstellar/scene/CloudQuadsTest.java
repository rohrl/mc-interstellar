package io.github.rohrl.interstellar.scene;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class CloudQuadsTest {
    @Test void eligibilityRequiresFiniteNondegenerateAxisAlignedRectangles() {
        for(int axis=0;axis<3;axis++)for(int sign:new int[]{-1,1}) {
            float[] q=quad(axis,sign);assertTrue(CloudQuads.rectangle(q,0));
            float[] bent=q.clone();bent[24+axis]+=.001f;assertFalse(CloudQuads.rectangle(bent,0));
            float[] trapezoid=q.clone();trapezoid[24+(axis+1)%3]+=.5f;assertFalse(CloudQuads.rectangle(trapezoid,0));
            float[] invalid=q.clone();invalid[axis]=Float.NaN;assertFalse(CloudQuads.rectangle(invalid,0));
        }
        assertFalse(CloudQuads.rectangle(new float[48],0));
    }
    @Test void mixedForestPreservesNativeCloudCornersAndActorRecordsAcrossLeafAndTextureRows() {
        int actors=117,clouds=713;float[] data=new float[actors*36+clouds*72];
        for(int a=0;a<actors;a++)for(int j=0;j<36;j++)data[a*36+j]=j%12==3?2:a*100+j;
        for(int q=0;q<clouds;q++) {
            float[] quad=quad(q%3,q%2==0?1:-1);
            for(int v=0;v<4;v++){quad[v*12+4]=q;quad[v*12+5]=v;quad[v*12+8]=q+.125f;}
            int[] corners={0,1,2,2,3,0};
            for(int v=0;v<6;v++)System.arraycopy(quad,corners[v]*12,data,actors*36+q*72+v*12,12);
        }
        float[] original=data.clone();var trees=MovingMeshTrees.build(data,actors+clouds*2,true);
        assertArrayEquals(original,data);assertEquals(actors*9,trees.cloudVertexBase());
        var baseline=MovingMeshTrees.build(data,actors+clouds*2);
        assertArrayEquals(Arrays.copyOf(baseline.triangles(),actors*36),Arrays.copyOf(trees.triangles(),actors*36));
        assertEquals(actors*36+clouds*48,trees.triangles().length);
        boolean[] seen=new boolean[clouds];
        for(int q=0;q<clouds;q++) {
            int p=actors*36+q*48,id=(int)trees.triangles()[p+4];assertFalse(seen[id]);seen[id]=true;
            float[] expected=QuadVertices.pack(Arrays.copyOfRange(original,actors*36+id*72,actors*36+(id+1)*72),2);
            CloudQuads.describe(expected,0);
            assertArrayEquals(expected,Arrays.copyOfRange(trees.triangles(),p,p+48));
        }
        int end=trees.actorNodes()+trees.cloudNodes(),covered=0;
        for(int n=trees.actorNodes();n<end;n++) {
            int p=n*12,count=(int)trees.nodes()[p+8],first=(int)trees.nodes()[p+7];
            assertTrue(trees.nodes()[p+3]>n && trees.nodes()[p+3]<=end);assertTrue(count<=4);
            if(count>0){assertTrue(first>=0 && first+count<=clouds);covered+=count;}
            for(int q=first;q<first+count;q++)for(int v=0;v<4;v++)for(int a=0;a<3;a++) {
                float x=trees.triangles()[actors*36+q*48+v*12+a];
                assertTrue(x>=trees.nodes()[p+a] && x<=trees.nodes()[p+4+a]);
            }
        }
        assertEquals(clouds,covered);
        assertEquals(0,MovingMeshTrees.build(new float[0],0,true).cloudNodes());
    }
    @Test void planeMetadataReconstructsTheOrientedNativeBasis() {
        for(int axis=0;axis<3;axis++)for(int sign:new int[]{-1,1}) {
            float[] q=quad(axis,sign),original=q.clone();CloudQuads.describe(q,0);
            int code=(int)q[6],first=code/3,second=3-axis-first;
            assertEquals(axis,code%3);
            assertEquals(1,(q[12+first]-q[first])*q[18],1e-6f);
            assertEquals(1,(q[36+second]-q[second])*q[19],1e-6f);
            assertEquals(192f*sign,q[7]);
            for(int i=0;i<48;i++)if(i!=6 && i!=7 && i!=18 && i!=19)assertEquals(original[i],q[i]);
        }
    }
    private static float[] quad(int axis,int sign) {
        float[] q=new float[48];float[][] corners={{-12,-4},{12,-4},{12,4},{-12,4}};
        for(int v=0;v<4;v++){int p=v*12;q[p+axis]=192.33f;q[p+(axis+1)%3]=corners[v][0]*sign;q[p+(axis+2)%3]=corners[v][1];q[p+3]=6;}
        return q;
    }
}
