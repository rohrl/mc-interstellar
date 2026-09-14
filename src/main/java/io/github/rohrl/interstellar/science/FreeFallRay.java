package io.github.rohrl.interstellar.science;

/** Independent PG-time reference: adaptive Dormand-Prince, not the GPU's inverse-radius RK4. */
public final class FreeFallRay {
    private FreeFallRay() { }
    public enum Outcome { SKY, DARK, UNRESOLVED }
    public record Result(Outcome outcome, double angle, double maximumImpactError, int steps) { }
    private static final double CRITICAL = 1.5 * Math.sqrt(3);

    /** Past-directed local looking cosine in a frame freely falling from rest at infinity; r_s=c=1. */
    public static Result trace(double radius, double mu, double tolerance) {
        if (!Double.isFinite(radius) || radius < .1 || radius > 10000 || !Double.isFinite(mu)
                || Math.abs(mu) > 1 || !Double.isFinite(tolerance) || tolerance < 1e-13 || tolerance > 1e-5)
            throw new IllegalArgumentException("Invalid free-fall reference ray");
        double flow = 1 / Math.sqrt(radius);
        double energy = 1 + flow * mu;
        double angular = radius * Math.sqrt(Math.max(0, 1 - mu * mu));
        double impact = angular / energy;
        // Constants of motion classify whether this past ray connects to our asymptotic sky.
        // DARK is an unilluminated boundary, not an assertion of hitting the future singularity.
        if (energy > 0 && Math.abs(impact - CRITICAL) < 1e-11 &&
                (radius >= 1.5 ? mu + flow <= 0 : mu + flow > 0))
            return new Result(Outcome.UNRESOLVED, Double.NaN, 0, 0);
        if (energy <= 0 || (radius >= 1.5
                ? mu + flow < 0 && impact <= CRITICAL
                : mu + flow <= 0 || impact >= CRITICAL))
            return new Result(Outcome.DARK, Double.NaN, 0, 0);
        if (mu == 1) return new Result(Outcome.SKY, 0, 0, 0);
        double[] y = {radius, 0, Math.acos(mu)};
        double target = Math.max(radius * 2, Math.max(128, 16 * impact));
        double step = .02 * radius, invariantError = 0;
        for (int attempt = 0; attempt < 30000; attempt++) {
            step = Math.min(step, .1 * y[0]);
            double[][] k = new double[7][];
            k[0] = derivative(y);
            k[1] = derivative(stage(y, step, k, new double[]{1.0/5}));
            k[2] = derivative(stage(y, step, k, new double[]{3.0/40,9.0/40}));
            k[3] = derivative(stage(y, step, k, new double[]{44.0/45,-56.0/15,32.0/9}));
            k[4] = derivative(stage(y, step, k, new double[]{19372.0/6561,-25360.0/2187,64448.0/6561,-212.0/729}));
            k[5] = derivative(stage(y, step, k, new double[]{9017.0/3168,-355.0/33,46732.0/5247,49.0/176,-5103.0/18656}));
            double[] next = stage(y, step, k, new double[]{35.0/384,0,500.0/1113,125.0/192,-2187.0/6784,11.0/84});
            k[6] = derivative(next);
            double[] lower = stage(y, step, k, new double[]{5179.0/57600,0,7571.0/16695,393.0/640,-92097.0/339200,187.0/2100,1.0/40});
            double error = 0;
            for (int j = 0; j < 3; j++) error = Math.max(error,
                    Math.abs(next[j] - lower[j]) / (tolerance * (j == 0 ? Math.max(1, Math.abs(next[j])) : 1)));
            if (!Double.isFinite(error) || step < 1e-13)
                return new Result(Outcome.UNRESOLVED, Double.NaN, invariantError, attempt + 1);
            if (error <= 1) {
                y = next;
                double measured = y[0] * Math.sin(y[2]) / (1 + Math.cos(y[2]) / Math.sqrt(y[0]));
                invariantError = Math.max(invariantError, Math.abs(measured - impact) / Math.max(1, impact));
                if (y[0] >= target) return new Result(Outcome.SKY,
                        y[1] + tail(y[0], impact), invariantError, attempt + 1);
            }
            step *= error == 0 ? 4 : Math.max(.2, Math.min(4, .9 * Math.pow(error, -.2)));
        }
        return new Result(Outcome.UNRESOLVED, Double.NaN, invariantError, 30000);
    }

    // s=-t_PG; state is (r, phi, psi), mu=cos(psi). Null speed is one in the falling frame.
    private static double[] derivative(double[] y) {
        double a = 1 / Math.sqrt(y[0]), mu = Math.cos(y[2]), sine = Math.sin(y[2]);
        return new double[]{mu + a, sine / y[0], -sine * (1 + 1.5 * a * mu) / y[0]};
    }
    private static double[] stage(double[] y, double h, double[][] k, double[] weights) {
        double[] value = y.clone();
        for (int i = 0; i < weights.length; i++) for (int j = 0; j < 3; j++) value[j] += h * weights[i] * k[i][j];
        return value;
    }
    // Independent far-field angular quadrature, avoiding a finite-radius sky approximation.
    private static double tail(double r, double b) {
        double h = 1 / (r * 64), sum = 0;
        for (int i = 0; i <= 64; i++) {
            double u = i * h;
            sum += (i == 0 || i == 64 ? 1 : i % 2 == 0 ? 2 : 4) * b / Math.sqrt(1 - b*b*u*u*(1-u));
        }
        return sum * h / 3;
    }
}
