package io.github.rohrl.interstellar.science;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HorizonObserverTest {
    @Test void agreesWithStaticExteriorAndIsRegularAcrossHorizon() {
        for(double r:new double[]{1.25,1.5,2,8})for(double mu:new double[]{-.95,-.6,0,.8}) {
            var a=FreeFallRay.trace(r,HorizonObserver.fallingCosine(r,mu),1e-11);
            var b=ExteriorRay.trace(r,mu,1,.0002);
            assertEquals(switch(b.outcome()) {case ESCAPED->FreeFallRay.Outcome.SKY;case CAPTURED->FreeFallRay.Outcome.DARK;case UNRESOLVED->FreeFallRay.Outcome.UNRESOLVED;},a.outcome());
            if(a.outcome()==FreeFallRay.Outcome.SKY)assertEquals(b.angle(),a.angle(),2e-5);
        }
        for(double r:new double[]{.001,.1,.5,.999999,1,1.000001,1.05})for(double mu:new double[]{-1,-.7,0,.8,1})
            assertEquals(mu,HorizonObserver.fallingCosine(r,mu),1e-12);
    }
}
