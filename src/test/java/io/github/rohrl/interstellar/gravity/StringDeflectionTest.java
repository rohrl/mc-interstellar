package io.github.rohrl.interstellar.gravity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StringDeflectionTest {
    private final GravityField field=new GravityField(0,10,0,64,3.5,3.5,.05);
    @Test void attachmentsAndAbsentFieldStayFixed() {
        assertEquals(0,StringDeflection.offset(field,0,0,0,8,0).length());
        assertEquals(0,StringDeflection.offset(field,0,0,0,8,1).length());
        assertEquals(0,StringDeflection.offset(null,0,0,0,8,.5).length());
        assertEquals(0,StringDeflection.offset(field,100,0,0,8,.5).length());
    }
    @Test void sagFollowsForceIncludingUpwardPullAndRemainsBounded() {
        var d=StringDeflection.offset(field,0,0,0,8,.5);
        assertTrue(d.y()>0);assertEquals(0,d.x());assertEquals(0,d.z());
        for(int i=1;i<100;i++) {
            double t=i/100.0;
            var offset=StringDeflection.offset(field,1,8,0,100,t);
            assertTrue(Double.isFinite(offset.length()));assertTrue(offset.length()<=2*4*t*(1-t)+1e-12);
        }
    }
}
