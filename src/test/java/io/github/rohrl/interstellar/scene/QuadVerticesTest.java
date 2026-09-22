package io.github.rohrl.interstellar.scene;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class QuadVerticesTest {
    @Test void roundTripKeepsBothNativeTrianglesAndAllAttributesBitForBit() {
        var random=new Random(141);int count=900;var original=new float[count*48];
        for(int i=0;i<original.length;i++)original[i]=(random.nextFloat()-.5f)*1000;
        original[0]=-0f;
        var triangles=new float[count*72];int[] order={0,1,2,2,3,0};
        for(int q=0;q<count;q++)for(int v=0;v<6;v++)System.arraycopy(original,q*48+order[v]*12,triangles,q*72+v*12,12);
        float[] saved=triangles.clone(),packed=QuadVertices.pack(triangles,count*2);
        assertArrayEquals(saved,triangles);
        assertEquals(original.length,packed.length);
        for(int i=0;i<original.length;i++)assertEquals(Float.floatToRawIntBits(original[i]),Float.floatToRawIntBits(packed[i]));
        // Both halves retain the exact old interpolation inputs, including nonplanar positions.
        for(int q=0;q<count;q++)for(int v=0;v<6;v++)for(int a=0;a<12;a++)
            assertEquals(Float.floatToRawIntBits(triangles[q*72+v*12+a]),Float.floatToRawIntBits(packed[q*48+order[v]*12+a]));
    }
    @Test void refusesTrianglesThatNoLongerShareTheNativeCorners() {
        assertThrows(IllegalArgumentException.class,()->QuadVertices.pack(new float[36],1));
        float[] triangles=new float[72];triangles[60]=1;
        assertThrows(IllegalArgumentException.class,()->QuadVertices.pack(triangles,2));
    }
    @Test void diagnosticFixturesExerciseBothHalvesWithoutChangingActiveVertices() {
        var triangles=new float[12*36];for(int i=0;i<triangles.length;i++)triangles[i]=i;
        var quads=QuadVertices.fixture(triangles,triangles.length);
        for(int t=0;t<12;t++) {
            int[] active=(t&1)==0?new int[]{0,1,2}:new int[]{2,3,0};
            for(int v=0;v<3;v++)for(int a=0;a<12;a++)assertEquals(triangles[t*36+v*12+a],quads[t*48+active[v]*12+a]);
            int a=(t&1)==0?2:0,b=(t&1)==0?3:1;
            for(int i=0;i<12;i++)assertEquals(quads[t*48+a*12+i],quads[t*48+b*12+i]);
        }
    }
}
