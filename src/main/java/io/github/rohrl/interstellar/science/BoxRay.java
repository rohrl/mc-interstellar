package io.github.rohrl.interstellar.science;

/** Independent ray/unit-cube slab intersection for scene-data diagnostics. */
public final class BoxRay {
    private BoxRay() { }
    public static double entry(double ox,double oy,double oz,double dx,double dy,double dz,int x,int y,int z) {
        double lo=0,hi=Double.POSITIVE_INFINITY;
        for(int axis=0;axis<3;axis++) {
            double o=axis==0?ox:axis==1?oy:oz,d=axis==0?dx:axis==1?dy:dz;
            int lower=axis==0?x:axis==1?y:z;
            if(Math.abs(d)<1e-15) {if(o<lower || o>=lower+1) return Double.POSITIVE_INFINITY;}
            else {
                double a=(lower-o)/d,b=(lower+1-o)/d;
                lo=Math.max(lo,Math.min(a,b));hi=Math.min(hi,Math.max(a,b));
                if(hi<lo) return Double.POSITIVE_INFINITY;
            }
        }
        return hi>=lo?lo:Double.POSITIVE_INFINITY;
    }
}