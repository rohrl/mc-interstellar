package io.github.rohrl.interstellar.science;

/** Planar Schwarzschild null orbit, r_s=1. Diagnostic CPU integration only. */
public final class ExteriorRay {
    private ExteriorRay() { }
    public enum Outcome { ESCAPED, CAPTURED, UNRESOLVED }
    public record Result(Outcome outcome, double angle, double maximumInvariantError) { }

    /**
     * radialCosine is the camera-frame looking direction dotted with outward radial unit vector.
     * mass=0 is the flat reference; mass=1 is Schwarzschild. Observer is static and exterior.
     * Integrates u'' = 1.5 mass u^2 - u, with u=1/r, using fixed-step RK4.
     */
    public static Result trace(double radius, double radialCosine, double mass, double step) {
        if (!Double.isFinite(radius) || radius <= mass || radius <= 0 ||
                !Double.isFinite(radialCosine) || Math.abs(radialCosine) > 1 ||
                (mass != 0 && mass != 1) || !Double.isFinite(step) || step < 0.0001 || step > 0.05) {
            throw new IllegalArgumentException("Invalid exterior ray or integration step");
        }
        if (Math.abs(radialCosine) > 1 - 1e-12) {
            return new Result(radialCosine < 0 && mass > 0 ? Outcome.CAPTURED : Outcome.ESCAPED,
                    Math.acos(radialCosine), 0);
        }
        double u = 1 / radius;
        double p = -radialCosine * u * Math.sqrt(1 - mass * u) /
                Math.sqrt(1 - radialCosine * radialCosine);
        double invariant = p * p + u * u - mass * u * u * u;
        double error = 0;
        double phi = 0;
        int limit = (int) Math.ceil(16 / step);
        for (int i = 0; i < limit; i++) {
            double a = acceleration(u, mass);
            double b = acceleration(u + step * p / 2, mass);
            double c = acceleration(u + step * (p + step * a / 2) / 2, mass);
            double d = acceleration(u + step * (p + step * b / 2), mass);
            double nextU = u + step * (p + 2 * (p + step * a / 2) +
                    2 * (p + step * b / 2) + p + step * c) / 6;
            double nextP = p + step * (a + 2 * b + 2 * c + d) / 6;
            if (nextU <= 0) {
                return new Result(Outcome.ESCAPED, phi + step * u / (u - nextU), error);
            }
            error = Math.max(error, Math.abs(nextP * nextP + nextU * nextU -
                    mass * nextU * nextU * nextU - invariant));
            if (mass > 0 && nextU >= 1 / mass) {
                return new Result(Outcome.CAPTURED, phi + step, error);
            }
            u = nextU;
            p = nextP;
            phi += step;
        }
        return new Result(Outcome.UNRESOLVED, phi, error);
    }

    private static double acceleration(double u, double mass) { return 1.5 * mass * u * u - u; }
}
