package io.github.rohrl.interstellar.science;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FreeFallRayTest {
    @Test void agreesWithExteriorRaysAfterLocalLorentzTransformation() {
        for (double radius : new double[]{2, 4, 8, 64}) for (double staticMu : new double[]{-.98,-.85,-.4,0,.6,.99}) {
            double flow = 1 / Math.sqrt(radius);
            double fallingMu = (staticMu-flow)/(1-flow*staticMu);
            var pg = FreeFallRay.trace(radius, fallingMu, 1e-11);
            var exterior = ExteriorRay.trace(radius, staticMu, 1, .0002);
            assertEquals(exterior.outcome() == ExteriorRay.Outcome.ESCAPED ? FreeFallRay.Outcome.SKY : FreeFallRay.Outcome.DARK, pg.outcome());
            if (pg.outcome() == FreeFallRay.Outcome.SKY) {
                assertEquals(exterior.angle(), pg.angle(), 2e-6, "r="+radius+" mu="+staticMu);
                assertTrue(pg.maximumImpactError() < 1e-7);
            }
        }
    }
    @Test void outwardPastRadialRayConnectsToSkyEvenInsideHorizon() {
        for (double r : new double[]{.35,.9,1,1.1,8}) {
            var ray = FreeFallRay.trace(r, 1, 1e-11);
            assertEquals(FreeFallRay.Outcome.SKY, ray.outcome());
            assertEquals(0, ray.angle());
        }
    }
    @Test void horizonSkyIsContinuousForNoncriticalDirections() {
        for (double mu : new double[]{-.6,-.2,.4,.9}) {
            var outside = FreeFallRay.trace(1.000001, mu, 1e-11);
            var at = FreeFallRay.trace(1, mu, 1e-11);
            var inside = FreeFallRay.trace(.999999, mu, 1e-11);
            assertEquals(FreeFallRay.Outcome.SKY, at.outcome());
            assertEquals(at.angle(), outside.angle(), 2e-5);
            assertEquals(at.angle(), inside.angle(), 2e-5);
        }
    }
    @Test void horizonCaptureBoundaryMatchesClosedFormAndRefinementConverges() {
        double criticalMu = (1-6.75)/(1+6.75);
        assertEquals(FreeFallRay.Outcome.DARK, FreeFallRay.trace(1, criticalMu-.001, 1e-11).outcome());
        var fine = FreeFallRay.trace(1, criticalMu+.001, 1e-12);
        var coarse = FreeFallRay.trace(1, criticalMu+.001, 1e-9);
        assertEquals(FreeFallRay.Outcome.SKY, fine.outcome());
        assertEquals(fine.angle(), coarse.angle(), 1e-5);
    }
    @Test void criticalOrbitIsUnresolvedButItsOutwardBranchEscapes() {
        assertEquals(FreeFallRay.Outcome.UNRESOLVED, FreeFallRay.trace(1, (1-6.75)/(1+6.75), 1e-11).outcome());
        double radius = 8, flow = 1/Math.sqrt(radius);
        double staticMu = Math.sqrt(1-6.75*(1-1/radius)/(radius*radius));
        assertEquals(FreeFallRay.Outcome.SKY, FreeFallRay.trace(radius, (staticMu-flow)/(1-flow*staticMu), 1e-11).outcome());
    }
    @Test void rejectsInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> FreeFallRay.trace(0, 0, 1e-10));
        assertThrows(IllegalArgumentException.class, () -> FreeFallRay.trace(1, Double.NaN, 1e-10));
        assertThrows(IllegalArgumentException.class, () -> FreeFallRay.trace(1, 0, 0));
    }
}
