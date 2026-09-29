package io.github.rohrl.interstellar.science;

import io.github.rohrl.interstellar.science.FiniteTerrainRay.Point;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LocalWormholeTest {
    private static final Point A=new Point(0,0,0),B=new Point(128,0,0);
    @Test void compactProfileKeepsEllisNearThroatAndHasNoExtraCircularOrbits() {
        for(double distance:new double[]{20,100,200,1145}) {
            double outer=LocalWormhole.influence(8,distance),inner=(8+outer)/2;
            assertTrue(outer*2<distance);
            assertEquals(-128/(8.1*(8.1*8.1+64)),LocalWormhole.gradient(8.1,8,outer),1e-12);
            assertEquals(0,LocalWormhole.gradient(outer,8,outer),0);
            assertEquals(0,LocalWormhole.gradient(outer+100,8,outer),0);
            for(double r=8.001;r<outer;r+=.01)assertTrue(1+r*LocalWormhole.gradient(r,8,outer)>0);
            assertEquals(LocalWormhole.gradient(inner-1e-6,8,outer),LocalWormhole.gradient(inner+1e-6,8,outer),1e-6);
        }
    }
    @Test void unrelatedRaysAndBothSidesOfTheOldMidplaneKeepTheSameWorld() {
        var d=new Point(0,0,-1);
        for(double x:new double[]{-128,63.99,64,64.01,256}) {
            var result=LocalWormhole.reference(A,B,new Point(x,0,-100),d,8,.04);
            assertEquals(d,result.direction());assertEquals(0,result.passages());assertFalse(result.limited());
        }
    }
    @Test void eitherVisibleMouthCanBeEnteredFromOneCamera() {
        var eye=new Point(64,0,-100);
        for(var target:new Point[]{A,B}) {
            var look=target.subtract(eye).unit();var ray=LocalWormhole.reference(A,B,eye,look,8,.04);
            assertEquals(1,ray.passages());assertFalse(ray.limited());
            var expected=new Point(-look.x(),-look.y(),look.z());
            assertTrue(ray.direction().subtract(expected).length()<1e-8);
        }
    }
    @Test void swappingNearestMouthCannotSwitchOrDeleteTheWorld() {
        var eye=new Point(64,5,-80);var look=new Point(-60,-2,80);
        var a=LocalWormhole.reference(A,B,eye,look,8,.025);
        var b=LocalWormhole.reference(B,A,eye,look,8,.025);
        assertEquals(a.passages(),b.passages());assertEquals(a.limited(),b.limited());
        assertTrue(a.direction().subtract(b.direction()).length()<1e-12);
        assertTrue(a.position().subtract(b.position()).length()<1e-10);
    }
    @Test void repeatedPassagesHaveAnExplicitBudget() {
        var ray=LocalWormhole.reference(A,new Point(0,0,-128),new Point(0,0,-64),new Point(0,0,1),8,.04);
        assertTrue(ray.limited());assertEquals(4,ray.passages());
    }
    @Test void finerReferenceConvergesForBentReflectedAndTransmittedRays() {
        for(double x:new double[]{2,8,12,20,40}) {
            var eye=new Point(x,2,-100);var look=new Point(0,0,1);
            var coarse=LocalWormhole.reference(A,B,eye,look,8,.04);
            var fine=LocalWormhole.reference(A,B,eye,look,8,.02);
            assertEquals(coarse.passages(),fine.passages());assertEquals(coarse.limited(),fine.limited());
            assertTrue(coarse.direction().subtract(fine.direction()).length()<1e-7,"x="+x);
        }
    }
}
