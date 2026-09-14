package io.github.rohrl.interstellar.science;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExteriorRayTest {
    @Test void flatRaysRecoverTheirOriginalSkyDirection() {
        for (double mu : new double[]{-.99, -.7, 0, .4, .99}) {
            var result = ExteriorRay.trace(8, mu, 0, .001);
            assertEquals(ExteriorRay.Outcome.ESCAPED, result.outcome());
            assertEquals(Math.acos(mu), result.angle(), 1e-8);
        }
    }

    @Test void captureBoundaryAgreesWithIndependentAnalyticShadowAngle() {
        for (double radius : new double[]{1.1, 1.5, 2, 8, 64}) {
            double alpha = new Schwarzschild(1).staticShadowHalfAngle(radius);
            assertEquals(ExteriorRay.Outcome.CAPTURED,
                    ExteriorRay.trace(radius, -Math.cos(alpha - .005), 1, .001).outcome());
            assertEquals(ExteriorRay.Outcome.ESCAPED,
                    ExteriorRay.trace(radius, -Math.cos(alpha + .005), 1, .001).outcome());
        }
    }

    @Test void fixedGpuStepConvergesToFineReferenceAwayFromCriticalRay() {
        for (double mu : new double[]{-.85, -.4, 0, .4, .95}) {
            var fine = ExteriorRay.trace(8, mu, 1, .001);
            var coarse = ExteriorRay.trace(8, mu, 1, .02);
            assertEquals(fine.outcome(), coarse.outcome());
            assertEquals(fine.angle(), coarse.angle(), 2e-5);
            assertTrue(coarse.maximumInvariantError() < 1e-7);
        }
    }

    @Test void weakFieldMatchesLeadingEinsteinDeflection() {
        double radius = 10000;
        double impact = 100;
        double sine = impact * Math.sqrt(1 - 1 / radius) / radius;
        var result = ExteriorRay.trace(radius, -Math.sqrt(1 - sine * sine), 1, .001);
        assertEquals(ExteriorRay.Outcome.ESCAPED, result.outcome());
        double deflection = result.angle() - Math.PI + Math.asin(impact / radius);
        assertEquals(2 / impact, deflection, .0004); // O((r_s/b)^2) terms are expected.
    }
}
