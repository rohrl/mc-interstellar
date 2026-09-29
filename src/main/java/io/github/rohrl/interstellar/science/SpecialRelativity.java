package io.github.rohrl.interstellar.science;

/** Local Lorentz optics; sight vectors point from observer toward the light source. */
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

    public record Ray(FiniteTerrainRay.Point direction,double doppler) {}
    /** Inverse aberration: camera-frame sight direction to the baseline observer's sky.
     * Independent four-vector boost, including the photon propagation sign. */
    public static Ray toBaseline(FiniteTerrainRay.Point sight,FiniteTerrainRay.Point velocity) {
        double beta=velocity.length();validateBeta(beta);var n=sight.unit();
        if(beta==0)return new Ray(n,1);
        double gamma=lorentzFactor(beta);var e=velocity.scale(1/beta);
        var photon=n.scale(-1);double parallel=photon.dot(e);
        double frequency=gamma*(1+beta*parallel);
        var momentum=photon.add(e.scale((gamma-1)*parallel+gamma*beta));
        return new Ray(momentum.scale(-1/frequency).unit(),1/frequency);
    }

    private static void validateBeta(double beta) {
        if (!Double.isFinite(beta) || Math.abs(beta) >= 1.0) {
            throw new IllegalArgumentException("Observer beta must be finite and strictly between -1 and 1");
        }
    }
}
