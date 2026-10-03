package io.github.rohrl.interstellar.science;

import io.github.rohrl.interstellar.science.FiniteTerrainRay.Point;

/** Independent double reference for the thin-disk shader. Distances in r_s. */
public final class AccretionDisk {
    private AccretionDisk(){}
    public static double crossing(Point start,Point end,Point axis,double outer) {
        double a=start.dot(axis),b=end.dot(axis);
        if(a*b>0 || Math.abs(a-b)<1e-8)return -1;
        double t=a/(a-b),r=start.add(end.subtract(start).scale(t)).length();
        return t>1e-6&&t<=1&&r>3&&r<outer?t:-1;
    }
    public static double speed(double r){return Math.sqrt(1/(2*(r-1)));}
    public static double temperature(double r) {
        double peak=49.0/12;
        return Math.pow(((1-Math.sqrt(3/r))/(r*r*r))/((1-Math.sqrt(3/peak))/(peak*peak*peak)),.25);
    }
    /** Energy contraction -u.p for a circular emitter and static observer. */
    public static double staticShift(double r,double observerRadius,double alongBackward) {
        double emitterUt=1/Math.sqrt(1-1.5/r);
        double observerUt=1/Math.sqrt(1-1/observerRadius);
        return observerUt/(emitterUt*(1+speed(r)*alongBackward));
    }
}
