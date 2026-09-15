package io.github.rohrl.interstellar.science;

/** Local outward looking cosine on the sky/dark boundary, r_s=1. */
public final class CriticalRays {
    private CriticalRays() { }
    public static double cosine(double radius, boolean falling) {
        if (!Double.isFinite(radius) || radius < .1 || radius > 64 || (!falling && radius <= 1))
            throw new IllegalArgumentException("Unsupported observer radius");
        double discriminant = Math.sqrt(Math.max(0, 1-6.75*(1-1/radius)/(radius*radius)));
        double sign = radius >= 1.5 ? -1 : 1;
        if (!falling) return sign*discriminant;
        // Solve b_c^2(1+mu/sqrt(r))^2=r^2(1-mu^2), choosing the approaching branch.
        return (-6.75/Math.sqrt(radius)+sign*radius*radius*discriminant)/(radius*radius+6.75/radius);
    }
}