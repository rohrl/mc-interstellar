package io.github.rohrl.interstellar.science;

/** Analytic reference quantities, not a ray tracer. Distances share one coordinate unit. */
public record Schwarzschild(double horizonRadius) {
    public Schwarzschild {
        if (!Double.isFinite(horizonRadius) || horizonRadius <= 0.0) {
            throw new IllegalArgumentException("Horizon radius must be finite and positive");
        }
    }

    public double photonSphereRadius() {
        return 1.5 * horizonRadius;
    }

    public double innermostStableCircularOrbitRadius() {
        return 3.0 * horizonRadius;
    }

    /** Impact parameter for the distant shadow boundary, not a surface radius. */
    public double criticalImpactParameter() {
        return 1.5 * Math.sqrt(3.0) * horizonRadius;
    }

    /**
     * Shadow half-angle for a static exterior observer and distant background.
     * The obtuse branch below the photon sphere is essential.
     * Does not describe a free-falling observer or an observer inside the horizon.
     */
    public double staticShadowHalfAngle(double coordinateRadius) {
        if (!Double.isFinite(coordinateRadius) || coordinateRadius <= horizonRadius) {
            throw new IllegalArgumentException("Static observer must be outside the horizon");
        }
        double inverseRadius = horizonRadius / coordinateRadius;
        double sine = 1.5 * Math.sqrt(3.0) * inverseRadius * Math.sqrt(1.0 - inverseRadius);
        double acute = Math.asin(Math.clamp(sine, 0.0, 1.0));
        return coordinateRadius >= photonSphereRadius() ? acute : Math.PI - acute;
    }
}
