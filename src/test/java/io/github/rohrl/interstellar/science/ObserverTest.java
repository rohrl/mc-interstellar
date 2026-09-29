package io.github.rohrl.interstellar.science;

import io.github.rohrl.interstellar.science.FiniteTerrainRay.Point;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ObserverTest {
    @Test void boostInvertsAndPreservesPhotonNullNorm() {
        var axis=new Point(1,2,-3).unit();
        for(double beta:new double[]{0,.1,.5,.9,.99})for(var n:new Point[]{axis,axis.scale(-1),new Point(1,0,0),new Point(0,1,0),new Point(0,0,1)}) {
            var v=axis.scale(beta);var ray=SpecialRelativity.toBaseline(n,v);
            var inverse=SpecialRelativity.toBaseline(ray.direction(),v.scale(-1));
            assertEquals(1,ray.direction().length(),1e-12);
            assertTrue(inverse.direction().subtract(n).length()<1e-12);
            assertEquals(1,ray.doppler()*inverse.doppler(),2e-12);
            // Independent directional frequency formula in the baseline sky.
            assertEquals(SpecialRelativity.lorentzFactor(beta)*(1+ray.direction().dot(v)),ray.doppler(),2e-12);
        }
    }
    @Test void knownDirectionsAndAberrationSign() {
        var axis=new Point(0,0,1);double b=.9;
        assertEquals(SpecialRelativity.forwardDopplerFactor(b),SpecialRelativity.toBaseline(axis,axis.scale(b)).doppler(),1e-12);
        var seenSide=SpecialRelativity.toBaseline(new Point(1,0,0),axis.scale(-b));
        assertEquals(b,seenSide.direction().z(),1e-12); // side scenery moves toward the forward direction
        assertThrows(IllegalArgumentException.class,()->SpecialRelativity.toBaseline(axis,axis));
    }
    @Test void sprintRampsCapsAndReleasesWithoutResidualSpeed() {
        var s=new SprintObserver();double last=0;
        for(int i=0;i<300;i++){s.advance(true,.05,.99,15);assertTrue(s.beta()>=last);last=s.beta();}
        assertEquals(.99,s.beta(),1e-12);
        for(int i=0;i<7;i++)s.advance(false,.05,.99,15);
        assertEquals(0,s.beta());assertEquals(0,s.elapsed());
        s.advance(true,.05,.99,15);assertTrue(s.beta()<=.1);
        s.reset();assertEquals(0,s.beta());
    }
    @Test void capChangesAndTimeSubdivisionStayBounded() {
        var a=new SprintObserver();var b=new SprintObserver();
        for(int i=0;i<150;i++)a.advance(true,.1,.9,15);
        for(int i=0;i<300;i++)b.advance(true,.05,.9,15);
        assertEquals(a.beta(),b.beta(),1e-12);
        b.advance(true,.05,.5,15);assertEquals(.5,b.beta(),1e-12);
        assertThrows(IllegalArgumentException.class,()->a.advance(true,.05,1,15));
    }
}
