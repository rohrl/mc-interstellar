package io.github.rohrl.interstellar.science;

import io.github.rohrl.interstellar.science.FiniteTerrainRay.Point;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MixedWorldTest {
    private static final Point A=new Point(0,0,0),B=new Point(128,0,0),M=new Point(25,3,-4);
    @Test void spatialCoefficientMatchesIndependentHamiltonianAcceleration() {
        for(double body:new double[]{0,1})for(double r:new double[]{.2,.7,.999,1.001,2,10})for(double mu:new double[]{-.95,-.4,0,.4,.95}) {
            var p=new Point(r,0,0);var d=new Point(mu,Math.sqrt(1-mu*mu),0);
            var simple=new Point(1,0,0).subtract(d.scale(mu)).scale(MixedWorld.coefficient(r,mu,.1,body));
            assertTrue(simple.subtract(MixedWorld.affineAcceleration(p,d,.1,body)).length()<1e-11,"r="+r+", mu="+mu+", body="+body);
        }
    }
    @Test void masslessCompositionMatchesTheEstablishedWormholeReference() {
        var model=new MixedWorld(A,B,M,0,0);
        for(double x:new double[]{0,12,20,40,128}) {
            var eye=new Point(x,6,-100);var look=new Point(0,0,1);
            var actual=model.reference(eye,look,.025);var expected=LocalWormhole.reference(A,B,eye,look,8,.01);
            assertEquals(expected.limited()?-3:expected.passages(),actual.code());
            assertTrue(actual.direction().subtract(expected.direction()).length()<.0001);
        }
    }
    @Test void mixedReferenceConvergesAndBothFieldsAffectTheSameRay() {
        for(double body:new double[]{0,4}) {
            var model=new MixedWorld(A,B,M,2,body);var eye=new Point(24,6,-100);var look=new Point(0,0,1);
            var a=model.reference(eye,look,.02);var b=model.reference(eye,look,.01);
            assertEquals(a.code(),b.code());assertTrue(a.direction().subtract(b.direction()).length()<.0002,"body="+body+", coarse="+a+", fine="+b);
            var wormholeOnly=new MixedWorld(A,B,M,0,0).reference(eye,look,.02);
            assertTrue(a.code()!=wormholeOnly.code() || a.direction().subtract(wormholeOnly.direction()).length()>.001);
        }
    }
    @Test void sourceOrderDoesNotChangeMixedImages() {
        var first=new MixedWorld(A,B,M,2,0);var swapped=new MixedWorld(B,A,M,2,0);
        var eye=new Point(8,6,-100);var look=new Point(0,0,1);
        var x=first.reference(eye,look,.03);var y=swapped.reference(eye,look,.03);
        assertEquals(x.code(),y.code());assertTrue(x.direction().subtract(y.direction()).length()<1e-10);
    }
}
