package io.github.rohrl.interstellar.science;

import io.github.rohrl.interstellar.config.CalibrationSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScientificReferenceTest {
    @Test
    void expandedObserverDistancesBracketTheAnalyticCaptureBoundary() {
        double critical=1.5*Math.sqrt(3);
        for(double radius:new double[]{16,24,32,64,128})for(double factor:new double[]{.99,1.01}) {
            double sine=critical*factor*Math.sqrt(1-1/radius)/radius;
            var ray=ExteriorRay.trace(radius,-Math.sqrt(1-sine*sine),1,.0005);
            assertEquals(factor<1?ExteriorRay.Outcome.CAPTURED:ExteriorRay.Outcome.ESCAPED,ray.outcome(),"r/r_s="+radius+", b/b_c="+factor);
            assertTrue(ray.maximumInvariantError()<1e-7);
        }
    }
    @Test
    void distantShadowApproachesCriticalImpactParameterOverDistance() {
        var source = new Schwarzschild(8.0);
        double radius = 8.0e7;
        assertEquals(12.0, source.photonSphereRadius());
        assertEquals(24.0, source.innermostStableCircularOrbitRadius());
        assertEquals(source.criticalImpactParameter() / radius,
                source.staticShadowHalfAngle(radius), 2e-14);
    }

    @Test
    void staticShadowUsesObtuseBranchBelowPhotonSphere() {
        var source = new Schwarzschild(8.0);
        assertEquals(Math.PI / 2.0, source.staticShadowHalfAngle(12.0), 2e-8);
        assertTrue(source.staticShadowHalfAngle(10.0) > Math.PI / 2.0);
        assertTrue(source.staticShadowHalfAngle(16.0) < Math.PI / 2.0);
        assertEquals(Math.PI, source.staticShadowHalfAngle(8.0 + 1e-12), 1e-6);
        assertThrows(IllegalArgumentException.class, () -> source.staticShadowHalfAngle(8.0));
        assertThrows(IllegalArgumentException.class, () -> source.staticShadowHalfAngle(4.0));
    }

    @Test
    void angularGeometryIsInvariantUnderCommonLengthScaling() {
        var small = new Schwarzschild(1.0);
        var large = new Schwarzschild(1000.0);
        for (double ratio : new double[]{1.01, 1.3, 1.5, 2.0, 8.0, 100.0}) {
            assertEquals(small.staticShadowHalfAngle(ratio),
                    large.staticShadowHalfAngle(1000.0 * ratio), 1e-12);
        }
    }

    @Test
    void dopplerIdentityReciprocityAndKnownVelocity() {
        assertEquals(1.0, SpecialRelativity.lorentzFactor(0.0));
        assertEquals(1.0, SpecialRelativity.forwardDopplerFactor(0.0));
        assertEquals(1.25, SpecialRelativity.lorentzFactor(0.6), 1e-14);
        assertEquals(2.0, SpecialRelativity.forwardDopplerFactor(0.6), 1e-14);
        for (double beta : new double[]{0.1, 0.6, 0.99, 0.999999}) {
            assertEquals(1.0, SpecialRelativity.forwardDopplerFactor(beta)
                    * SpecialRelativity.forwardDopplerFactor(-beta), 1e-14);
        }
    }

    @Test
    void invalidPhysicalInputsAreRejectedBeforeRendering() {
        for (double radius : new double[]{0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new Schwarzschild(radius));
            assertThrows(IllegalArgumentException.class, () -> new CalibrationSettings(true, radius, 64.0));
            assertThrows(IllegalArgumentException.class, () -> new CalibrationSettings(true, 8.0, radius));
        }
        for (double beta : new double[]{-1.0, 1.0, 1.1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> SpecialRelativity.lorentzFactor(beta));
            assertThrows(IllegalArgumentException.class, () -> SpecialRelativity.forwardDopplerFactor(beta));
        }
    }
}
