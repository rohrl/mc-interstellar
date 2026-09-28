package io.github.rohrl.interstellar.science;

/** Ultrastatic Ellis geometry: ds²=-dt²+dl²+(l²+a²)dΩ². Units c=1.
 * The throat's areal radius is a; its isotropic coordinate radius is a/2.
 * Reference equations: James et al., arXiv:1502.03809, Eqs. 1,2,16.
 */
public record EllisWormhole(double throat) {
    public EllisWormhole {
        if(!Double.isFinite(throat) || throat<=0)throw new IllegalArgumentException("Positive finite throat radius required");
    }
    public record Ray(double ell,double radialMomentum,double angle) {}
    public double arealRadius(double ell) {return Math.hypot(ell,throat);}
    public double mouthRadius() {return throat/2;}
    /** Signed proper radial distance in the current end's isotropic chart. */
    public double properDistance(double coordinateRadius) {
        if(!(coordinateRadius>0))throw new IllegalArgumentException("Chart radius must be positive");
        return coordinateRadius-throat*throat/(4*coordinateRadius);
    }
    /** Radius in the end indicated by the sign of ell; always outside its mouth. */
    public double coordinateRadius(double ell) {return (arealRadius(ell)+Math.abs(ell))/2;}
    public double conformalFactor(double coordinateRadius) {return 1+throat*throat/(4*coordinateRadius*coordinateRadius);}
    public double invariant(Ray q,double impact) {
        return q.radialMomentum*q.radialMomentum+impact*impact/(q.ell*q.ell+throat*throat);
    }
    public Ray derivative(Ray q,double impact) {
        double rr=q.ell*q.ell+throat*throat;
        return new Ray(q.radialMomentum,impact*impact*q.ell/(rr*rr),impact/rr);
    }
    private static Ray add(Ray a,Ray b,double h) {
        return new Ray(a.ell+h*b.ell,a.radialMomentum+h*b.radialMomentum,a.angle+h*b.angle);
    }
    /** Affine Hamiltonian RK4 reference; impact parameter is conserved. */
    public Ray step(Ray q,double impact,double h) {
        Ray a=derivative(q,impact),b=derivative(add(q,a,h/2),impact);
        Ray c=derivative(add(q,b,h/2),impact),d=derivative(add(q,c,h),impact);
        return new Ray(q.ell+h*(a.ell+2*b.ell+2*c.ell+d.ell)/6,
                q.radialMomentum+h*(a.radialMomentum+2*b.radialMomentum+2*c.radialMomentum+d.radialMomentum)/6,
                q.angle+h*(a.angle+2*b.angle+2*c.angle+d.angle)/6);
    }
    /** Exact asymptotic reflected-ray deflection, Nakajima & Asada arXiv:1204.3710. */
    public double reflectedDeflection(double impact) {
        if(!(impact>throat))throw new IllegalArgumentException("Reflected rays require b>a");
        return 2*ellipticK(throat/impact)-Math.PI;
    }
    /** Exact total angular advance from one asymptotic end to the other. */
    public double transmittedAngle(double impact) {
        if(!(Math.abs(impact)<throat))throw new IllegalArgumentException("Transmitted rays require |b|<a");
        return 2*impact/throat*ellipticK(impact/throat);
    }
    private static double ellipticK(double k) {
        double a=1,b=Math.sqrt(1-k*k);
        for(int i=0;i<32 && Math.abs(a-b)>2e-16*a;i++) {double next=(a+b)/2;b=Math.sqrt(a*b);a=next;}
        return Math.PI/(2*a);
    }
    /** Inversion plus z reflection: orientation-preserving transition between
     * two exterior charts. Position offsets are measured from their mouth centres.
     */
    public double[] transfer(double[] offset) {
        double rr=dot(offset,offset),scale=throat*throat/(4*rr);
        if(!(rr>0))throw new IllegalArgumentException("Cannot invert chart origin");
        return new double[]{scale*offset[0],scale*offset[1],-scale*offset[2]};
    }
    /** Differential of transfer: transports velocity and camera axes consistently. */
    public double[] transferVector(double[] offset,double[] vector) {
        double rr=dot(offset,offset),scale=throat*throat/(4*rr),radial=2*dot(offset,vector)/rr;
        if(!(rr>0))throw new IllegalArgumentException("Cannot invert chart origin");
        return new double[]{scale*(vector[0]-radial*offset[0]),scale*(vector[1]-radial*offset[1]),-scale*(vector[2]-radial*offset[2])};
    }
    private static double dot(double[] a,double[] b) {return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];}
}
