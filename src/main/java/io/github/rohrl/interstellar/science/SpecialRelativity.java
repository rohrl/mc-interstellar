package io.github.rohrl.interstellar.science;

/** Analytic one-dimensional reference values. No arbitrary-direction rendering yet. */
public final class SpecialRelativity {
    private SpecialRelativity() { }

    public static double lorentzFactor(double beta) {
        validateBeta(beta);
        return 1.0 / Math.sqrt((1.0 - beta) * (1.0 + beta));
    }

    /** Frequency multiplier for light arriving head-on from the forward direction. */
    public static double forwardDopplerFactor(double beta) {
        validateBeta(beta);
        return Math.sqrt((1.0 + beta) / (1.0 - beta));
    }

    private static void validateBeta(double beta) {
        if (!Double.isFinite(beta) || Math.abs(beta) >= 1.0) {
            throw new IllegalArgumentException("Observer beta must be finite and strictly between -1 and 1");
        }
    }
}
