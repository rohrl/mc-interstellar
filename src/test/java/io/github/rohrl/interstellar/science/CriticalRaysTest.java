package io.github.rohrl.interstellar.science;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CriticalRaysTest {
    @Test void boundaryMatchesHorizonAndLocalFrameTransform() {
        assertEquals((1-6.75)/(1+6.75),CriticalRays.cosine(1,true),1e-15);
        assertEquals(0,CriticalRays.cosine(1.5,false),1e-15);
        for (double r : new double[]{1.05,1.5,2,8,64}) {
            double mu=CriticalRays.cosine(r,false), flow=1/Math.sqrt(r);
            assertEquals((mu-flow)/(1-flow*mu),CriticalRays.cosine(r,true),2e-14);
        }
    }
    @Test void bothSidesOfBoundaryAndUnwrappedConvergence() {
        for (double r : new double[]{(double).35f,.35,.7067775,1,8}) {
            double mu=CriticalRays.cosine(r,true);
            assertEquals(FreeFallRay.Outcome.DARK,FreeFallRay.trace(r,mu-1e-5,1e-12).outcome());
            double previous=0;
            for (double offset : new double[]{1e-3,1e-4,1e-5,1e-6}) {
                var ray=FreeFallRay.trace(r,mu+offset,1e-12);
                var refined=FreeFallRay.trace(r,mu+offset,1e-13);
                assertEquals(FreeFallRay.Outcome.SKY,ray.outcome());
                assertEquals(ray.outcome(),refined.outcome());
                assertEquals(ray.angle(),refined.angle(),2e-4);
                assertTrue(ray.angle()>previous);
                previous=ray.angle();
            }
            assertTrue(previous>2*Math.PI,"Must exercise a full orbit, not only its final direction");
        }
    }
    @Test void strongDeflectionGrowsLogarithmicallyAtHorizon() {
        double mu=CriticalRays.cosine(1,true);
        double a=FreeFallRay.trace(1,mu+1e-5,1e-13).angle();
        double b=FreeFallRay.trace(1,mu+1e-6,1e-13).angle();
        assertEquals(Math.log(10),b-a,.002);
    }
}