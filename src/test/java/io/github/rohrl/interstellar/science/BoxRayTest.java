package io.github.rohrl.interstellar.science;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BoxRayTest {
    @Test void thinSnowTopSidesAndEmptySpace() {
        for(int layers=1;layers<=8;layers++) {
            double height=layers/8.0;
            assertEquals(2-height,BoxRay.entry(.5,2,.5,0,-1,0,0,0,0,height),1e-12);
            assertEquals(1,BoxRay.entry(-1,height/2,.5,1,0,0,0,0,0,height));
            assertEquals(Double.POSITIVE_INFINITY,BoxRay.entry(-1,height+.01,.5,1,0,0,0,0,0,height));
        }
    }
    @Test void axisAlignedFacesAndInsideOrigin() {
        assertEquals(4,BoxRay.entry(.5,.5,-4,0,0,1,0,0,0));
        assertEquals(3,BoxRay.entry(.5,.5,4,0,0,-1,0,0,0));
        assertEquals(0,BoxRay.entry(.5,.5,.5,1,0,0,0,0,0));
    }
    @Test void rejectsBehindAndParallelOutside() {
        assertEquals(Double.POSITIVE_INFINITY,BoxRay.entry(.5,.5,-4,0,0,-1,0,0,0));
        assertEquals(Double.POSITIVE_INFINITY,BoxRay.entry(1,.5,-4,0,0,1,0,0,0));
    }
    @Test void nearestOccluderAndDiagonal() {
        assertTrue(BoxRay.entry(.5,.5,-4,0,0,1,0,0,0)<BoxRay.entry(.5,.5,-4,0,0,1,0,0,2));
        assertEquals(2,BoxRay.entry(-2,-2,-2,1,1,1,0,0,0));
        assertEquals(1,BoxRay.entry(-3,-3,-3,1,1,1,-2,-2,-2));
    }
}
