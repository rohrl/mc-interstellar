package io.github.rohrl.interstellar.science;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BoxRayTest {
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