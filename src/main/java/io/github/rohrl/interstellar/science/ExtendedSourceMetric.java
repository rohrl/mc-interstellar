package io.github.rohrl.interstellar.science;

/** Finite static spherical interior proxy; Schwarzschild exterior in areal coordinates.
 * Positive interior lapse is a chosen approximation, not a stellar equation of state. */
public record ExtendedSourceMetric(double radius,double surface) {
    public ExtendedSourceMetric {
        if(!Double.isFinite(radius)||!Double.isFinite(surface)||radius<=0||surface<=radius)
            throw new IllegalArgumentException("Extended surface must be outside its Schwarzschild radius");
    }
    public double compactness() {return radius/surface;}
    public double lapse(double r) {
        if(r>=surface)return 1-radius/r;
        double c=compactness(),d=1-c,x=r/surface;
        return d*d/(1-.5*c*(1+x*x));
    }
    public double inverseRadial(double r) {
        return r>=surface?1-radius/r:1-radius*r*r/(surface*surface*surface);
    }
    public double lapseDerivative(double r) {
        if(r>=surface)return radius/(r*r);
        double c=compactness(),d=1-c,x=r/surface,denominator=1-.5*c*(1+x*x);
        return d*d*c*r/(surface*surface*denominator*denominator);
    }
    public double inverseRadialDerivative(double r) {
        return r>=surface?radius/(r*r):-2*radius*r/(surface*surface*surface);
    }
    /** u=rs/r, e²=rs²/b²; d²u/dphi² from the static metric's null first integral. */
    public double angularAcceleration(double u,double energySquared) {
        double c=compactness();
        if(u<=c)return 1.5*u*u-u;
        double t=c*c*c/(u*u),d=1-c;
        return energySquared*t*(3-c-2*t)/(2*u*d*d)-u;
    }
    public double angularInvariant(double u,double slope,double energySquared) {
        double r=radius/u,b=inverseRadial(r);
        return slope*slope+u*u*b-energySquared*b/lapse(r);
    }
    /** Independent affine Hamiltonian derivative (x,y,px,py), used only by reference tests. */
    public double[] affineDerivative(double[] state,double energySquared) {
        double x=state[0],y=state[1],px=state[2],py=state[3],r=Math.hypot(x,y);
        if(r<1e-12)return new double[]{px,py,0,0};
        double nx=x/r,ny=y/r,pr=px*nx+py*ny,b=inverseRadial(r),a=lapse(r);
        double radial=.5*(energySquared*lapseDerivative(r)/(a*a)+inverseRadialDerivative(r)*pr*pr);
        double tangent=(b-1)*pr/r;
        return new double[]{px+(b-1)*pr*nx,py+(b-1)*pr*ny,
                -radial*nx-tangent*(px-pr*nx),-radial*ny-tangent*(py-pr*ny)};
    }
}
