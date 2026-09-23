package io.github.rohrl.interstellar.scene;

import org.junit.jupiter.api.Test;
import java.nio.IntBuffer;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class QuantizedMeshBoundsTest {
    @Test void allSignedGridValuesAndTheirNeighboursRoundOutward() {
        float[] node=new float[12];var packed=IntBuffer.allocate(4);
        for(int q=Short.MIN_VALUE;q<=Short.MAX_VALUE;q++) {
            float value=q/16f;
            for(float f:new float[]{Math.nextDown(value),value,Math.nextUp(value)}) {
                for(int a=0;a<3;a++)node[a]=node[a+4]=f;
                QuantizedMeshBounds.write(node,12,packed);
                boolean expectedFallback=f< -2048f || f>2047.9375f;
                assertEquals(expectedFallback,(packed.get(3)&QuantizedMeshBounds.FALLBACK)!=0);
                if(!expectedFallback)for(int a=0;a<3;a++) {
                    float low=(short)packed.get(a)/16f,high=(packed.get(a)>>16)/16f;
                    assertTrue(low<=f);assertTrue(high>=f);assertTrue(high-low<=.0625f);
                }
            }
        }
    }
    @Test void multirowPayloadPreservesEscapesAndContainsEveryOriginalBox() {
        var random=new Random(42);int count=QuantizedMeshBounds.WIDTH*3+7;
        float[] data=new float[count*12];var packed=IntBuffer.allocate(count*4);
        for(int i=0;i<count;i++) {
            data[i*12+3]=i%17==0?-1:random.nextInt(1<<23);
            for(int a=0;a<3;a++) {
                float low=random.nextFloat()*3900-1950;
                data[i*12+a]=low;data[i*12+a+4]=low+random.nextFloat()*10;
            }
        }
        float[] original=data.clone();QuantizedMeshBounds.write(data,data.length,packed);assertArrayEquals(original,data);
        for(int i=0;i<count;i++) {
            assertEquals((int)data[i*12+3],(packed.get(i*4+3)<<8)>>8);
            assertEquals(0,packed.get(i*4+3)&QuantizedMeshBounds.FALLBACK);
            for(int a=0;a<3;a++) {
                assertTrue((short)packed.get(i*4+a)/16f<=data[i*12+a]);
                assertTrue((packed.get(i*4+a)>>16)/16f>=data[i*12+a+4]);
            }
        }
    }
    @Test void overflowUsesExactFallbackAndInvalidLinksFailBeforeUpload() {
        float[] node=new float[12];node[3]=-1;var packed=IntBuffer.allocate(4);
        for(float f:new float[]{-3000,3000,Float.NEGATIVE_INFINITY,Float.POSITIVE_INFINITY,Float.NaN}) {
            node[0]=node[4]=f;QuantizedMeshBounds.write(node,12,packed);
            assertNotEquals(0,packed.get(3)&QuantizedMeshBounds.FALLBACK);assertEquals(-1,(packed.get(3)<<8)>>8);
        }
        node[3]=1<<23;assertThrows(IllegalArgumentException.class,()->QuantizedMeshBounds.write(node,12,packed));
        assertThrows(IllegalArgumentException.class,()->QuantizedMeshBounds.write(node,11,packed));
    }
}
