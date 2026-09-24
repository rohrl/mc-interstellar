package io.github.rohrl.interstellar.science;

/** Presentation frame: static outside 1.25 rs, freely falling inside 1.05 rs.
 * This changes the optical tetrad, not the Minecraft player's velocity. */
public final class HorizonObserver {
    private HorizonObserver() {}
    public static double fallingCosine(double radiusRatio,double cameraCosine) {
        if(!Double.isFinite(radiusRatio) || radiusRatio<=0 || !Double.isFinite(cameraCosine) || Math.abs(cameraCosine)>1)
            throw new IllegalArgumentException("Invalid observer");
        double t=Math.clamp((radiusRatio-1.05)/.2,0,1);
        double boost=t*t*(3-2*t)/Math.sqrt(radiusRatio);
        return (cameraCosine-boost)/(1-boost*cameraCosine);
    }
}
