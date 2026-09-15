package io.github.rohrl.interstellar.science;

import org.junit.jupiter.api.Test;
import static io.github.rohrl.interstellar.science.FiniteTerrainRay.*;
import static org.junit.jupiter.api.Assertions.*;

class FiniteTerrainRayTest {
    private static final Point SOURCE=new Point(16,16,16);
    private static Scene scene(int bx,int by,int bz,double height) {
        return new Scene() {
            public int side() {return 32;}
            public int value(int x,int y,int z) {return x==bx&&y==by&&z==bz?3:0;}
            public double height(int x,int y,int z) {return height;}
        };
    }
    @Test void radialOccluderPrecedesHorizonAndEmptyRayReachesHorizon() {
        Point camera=new Point(16.5,16.5,2),direction=SOURCE.subtract(camera).unit();
        var hit=trace(scene(16,16,6,1),SOURCE,camera,direction,2,.1,1e-10);
        assertEquals(Outcome.HIT,hit.outcome());assertEquals(6,hit.z());
        var dark=trace(scene(-1,-1,-1,1),SOURCE,camera,direction,2,.1,1e-10);
        assertEquals(Outcome.CAPTURED,dark.outcome());
        assertTrue(dark.invariantError()<1e-9);
    }
    @Test void flatOutsideEntryRespectsThinSnowAndMissesParallelRay() {
        var snow=scene(16,16,0,.125);
        var hit=trace(snow,SOURCE,new Point(16.5,16.0625,-10),new Point(0,0,1),0,.1,1e-10);
        assertEquals(Outcome.HIT,hit.outcome());assertEquals(0,hit.z());
        assertEquals(Outcome.MISSING,trace(snow,SOURCE,new Point(16.5,16.2,-10),new Point(0,0,1),0,.1,1e-10).outcome());
        assertEquals(Outcome.MISSING,trace(snow,SOURCE,new Point(-1,16,-10),new Point(0,0,1),0,.1,1e-10).outcome());
    }
    @Test void emptySceneCaptureMatchesAnalyticStaticShadowOnBothBranches() {
        var empty=scene(-1,-1,-1,1);
        for(double radius:new double[]{1.1,1.4,2,8}) for(double mu:new double[]{-.99,-.8,-.2,.2,.8,.99}) {
            Point camera=SOURCE.add(new Point(0,0,radius));
            var ray=trace(empty,SOURCE,camera,new Point(Math.sqrt(1-mu*mu),0,mu),1,.1,1e-10);
            double impact=radius*Math.sqrt(1-mu*mu)/Math.sqrt(1-1/radius),critical=1.5*Math.sqrt(3);
            boolean captured=radius>=1.5?mu<0&&impact<critical:mu<=0||impact>critical;
            assertEquals(captured?Outcome.CAPTURED:Outcome.MISSING,ray.outcome(),"r="+radius+" mu="+mu);
            assertTrue(ray.invariantError()<1e-7);
        }
    }
    @Test void curvedSurfaceHitIsStableUnderIndependentRefinement() {
        Scene wall=new Scene() {
            public int side() {return 32;}
            public int value(int x,int y,int z) {return z==28?3:0;}
            public double height(int x,int y,int z) {return 1;}
        };
        Point camera=new Point(16,16,2),direction=new Point(.45,0,1);
        var a=trace(wall,SOURCE,camera,direction,2,.1,1e-9);
        var b=trace(wall,SOURCE,camera,direction,2,.05,1e-11);
        assertEquals(Outcome.HIT,b.outcome());assertTrue(a.sameHit(b));
        assertTrue(b.invariantError()<1e-8);
    }
    @Test void invalidRaysAreRejected() {
        assertThrows(IllegalArgumentException.class,()->trace(scene(-1,-1,-1,1),SOURCE,SOURCE,new Point(1,0,0),1,.1,1e-10));
        assertThrows(IllegalArgumentException.class,()->trace(scene(-1,-1,-1,1),SOURCE,new Point(1,1,1),new Point(0,0,0),1,.1,1e-10));
    }
}
