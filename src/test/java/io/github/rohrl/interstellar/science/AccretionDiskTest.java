package io.github.rohrl.interstellar.science;

import io.github.rohrl.interstellar.config.AccretionSettings;
import io.github.rohrl.interstellar.science.FiniteTerrainRay.Point;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccretionDiskTest {
    @Test void finiteAnnulusAndSegmentBounds() {
        var up=new Point(0,1,0);
        assertEquals(.5,AccretionDisk.crossing(new Point(4,1,0),new Point(4,-1,0),up,10));
        for(double r:new double[]{0,2,3,10,12})assertEquals(-1,AccretionDisk.crossing(new Point(r,1,0),new Point(r,-1,0),up,10));
        assertEquals(-1,AccretionDisk.crossing(new Point(4,2,0),new Point(4,1,0),up,10));
        assertEquals(-1,AccretionDisk.crossing(new Point(4,0,0),new Point(5,0,0),up,10));
        var axis=new Point(1,1,0).unit();
        assertEquals(.5,AccretionDisk.crossing(new Point(4,-4,0).add(axis),new Point(4,-4,0).subtract(axis),axis,10),1e-12);
    }
    @Test void orbitAndSpectrumLandmarks() {
        assertEquals(.5,AccretionDisk.speed(3),1e-12);
        assertEquals(0,AccretionDisk.temperature(3),1e-12);
        assertEquals(1,AccretionDisk.temperature(49.0/12),1e-12);
        assertTrue(AccretionDisk.temperature(10)<AccretionDisk.temperature(5));
        assertEquals(3,AccretionDisk.staticShift(3,100,-1)/AccretionDisk.staticShift(3,100,1),1e-12);
        assertEquals(Math.sqrt(.5),AccretionDisk.staticShift(3,Double.POSITIVE_INFINITY,0),1e-12);
    }
    @Test void onlyRealLargeHolesByDefault() {
        var defaults=AccretionSettings.defaults();
        assertFalse(defaults.visible(false,100));assertFalse(defaults.visible(true,15));
        assertTrue(defaults.visible(true,16));
        assertFalse(new AccretionSettings(0,true,1,10,15,16).visible(true,100));
        assertTrue(new AccretionSettings(2,true,1,10,15,16).visible(true,2));
        assertThrows(IllegalArgumentException.class,()->new AccretionSettings(1,true,Float.NaN,10,15,16));
    }
}
