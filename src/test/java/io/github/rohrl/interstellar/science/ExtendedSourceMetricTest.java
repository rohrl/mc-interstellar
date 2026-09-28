package io.github.rohrl.interstellar.science;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExtendedSourceMetricTest {
    @Test void outgoingInverseRadiusStepsMustNotSkipAFiniteWall() {
        // Exact straight-ray limit u''=-u, impact parameter0.5, starting radius8.
        // A16-block estimate can cross u=0 before the ray tests the wall at x=28.
        double rs=Math.sqrt(3)/32,u=rs/8,energy=rs/.5,v=-Math.sqrt(energy*energy-u*u);
        double unsafe=Math.min(.08,16/(rs*Math.hypot(u,v)/(u*u)));
        assertTrue(u*Math.cos(unsafe)+v*Math.sin(unsafe)<0,"Reproduces premature sky escape");
        double phi=0,x=8,y=0,expectedY=20*.0625/Math.sqrt(1-.0625*.0625);
        for(int i=0;i<12;i++) {
            double h=Math.min(Math.min(.08,16/(rs*Math.hypot(u,v)/(u*u))),.5*u/-v);
            double nextU=u*Math.cos(h)+v*Math.sin(h),nextV=v*Math.cos(h)-u*Math.sin(h);
            assertTrue(nextU>0);phi+=h;
            double nextX=rs/nextU*Math.cos(phi),nextY=rs/nextU*Math.sin(phi);
            if(nextX>=28) {
                assertEquals(expectedY,y+(nextY-y)*(28-x)/(nextX-x),1e-11,"Finite wall remains on the tested chord");return;
            }
            u=nextU;v=nextV;x=nextX;y=nextY;
        }
        fail("Wall was skipped");
    }
    @Test void finiteCentreAndContinuousBoundary() {
        for(double c:new double[]{.0625,.25,.5625,.8,.95,.999}) {
            var metric=new ExtendedSourceMetric(c,1);
            assertTrue(metric.lapse(0)>0);assertEquals(1,metric.inverseRadial(0));
            assertEquals(0,metric.lapseDerivative(0));
            assertEquals(metric.lapse(1),metric.lapse(1-1e-8),1e-8);
            assertEquals(metric.inverseRadial(1),metric.inverseRadial(1-1e-8),2e-8);
            assertEquals(metric.lapseDerivative(1),metric.lapseDerivative(1-1e-8),2e-5);
        }
    }
    @Test void angularPathsAgreeWithIndependentAffineHamiltonian() {
        for(double c:new double[]{.0625,.25,.5625})for(double start:new double[]{.4,6})for(double tangent:new double[]{.025,.075,.12,.2,.35}) {
            var m=new ExtendedSourceMetric(c,1);double mu=-Math.sqrt(1-tangent*tangent);
            double u=c/start,v=-mu*u*Math.sqrt(m.inverseRadial(start))/tangent;
            double energy=u*u*m.lapse(start)/(tangent*tangent),phi=0,h=.0001;
            boolean escaped=false;
            for(int i=0;i<160000;i++) {
                double oldU=u,oldPhi=phi;
                double a=m.angularAcceleration(u,energy),ub=u+h*v/2,vb=v+h*a/2;
                double b=m.angularAcceleration(ub,energy),uc=u+h*vb/2,vc=v+h*b/2;
                double d=m.angularAcceleration(uc,energy),ue=u+h*vc,ve=v+h*d;
                double e=m.angularAcceleration(ue,energy);
                u+=h*(v+2*vb+2*vc+ve)/6;v+=h*(a+2*b+2*d+e)/6;phi+=h;
                if(v<0&&u<=c/start) {phi=oldPhi+h*(oldU-c/start)/(oldU-u);escaped=true;break;}
            }
            assertTrue(escaped,"Angular ray did not escape");
            double invariant=m.angularInvariant(u,v,energy);
            assertEquals(0,invariant,Math.max(1e-6,energy*.001));
            double[] state={start,0,mu/Math.sqrt(m.inverseRadial(start)),tangent};
            double e2=m.lapse(start),dt=.0005;double reference=0;escaped=false;
            for(int i=0;i<100000;i++) {
                double[] old=state;double[] a=m.affineDerivative(old,e2);
                double[] b=m.affineDerivative(add(old,a,dt/2),e2),d=m.affineDerivative(add(old,b,dt/2),e2);
                double[] e=m.affineDerivative(add(old,d,dt),e2);state=old.clone();
                for(int j=0;j<4;j++)state[j]+=dt*(a[j]+2*b[j]+2*d[j]+e[j])/6;
                double r=Math.hypot(state[0],state[1]);
                if(i>10&&r>=start) {
                    double fraction=(start-Math.hypot(old[0],old[1]))/(r-Math.hypot(old[0],old[1]));
                    reference=Math.atan2(old[1]+fraction*(state[1]-old[1]),old[0]+fraction*(state[0]-old[0]));
                    if(reference<0)reference+=2*Math.PI;escaped=true;break;
                }
            }
            assertTrue(escaped,"Affine ray did not escape");
            assertEquals(reference,phi,1e-3,"C="+c+", tangent="+tangent);
        }
    }
    @Test void splitSurfaceStepAgreesWithIndependentHamiltonianCrossing() {
        // Exercise both sides of the discontinuous radial metric derivative.
        // The reference evolves Cartesian Hamiltonian variables, not u(phi).
        for(double c:new double[]{.0625,.309,.5625,.8})for(boolean inside:new boolean[]{false,true})for(double tangent:new double[]{.1,.4,.8}) {
            var m=new ExtendedSourceMetric(c,1);double start=inside?.99:1.01;
            double mu=(inside?1:-1)*Math.sqrt(1-tangent*tangent),u=c/start;
            double v=-mu*u*Math.sqrt(m.inverseRadial(start))/tangent;
            double energy=u*u*m.lapse(start)/(tangent*tangent);
            double surfaceV=Math.copySign(Math.sqrt(energy-c*c*(1-c)),v);
            double h=2*(c-u)/(v+surfaceV);
            for(int i=0;i<3;i++) {
                double[] at=surfaceRk(m,u,v,energy,h,inside);
                h+=(c-at[0])/at[1];
            }
            assertTrue(h>0 && h<.08,"Fixture crosses within one production-sized angular step");
            double[] state={start,0,mu/Math.sqrt(m.inverseRadial(start)),tangent};
            double dt=.00002;boolean crossed=false;
            for(int i=0;i<20000;i++) {
                double[] old=state,a=m.affineDerivative(old,m.lapse(start));
                double[] b=m.affineDerivative(add(old,a,dt/2),m.lapse(start));
                double[] d=m.affineDerivative(add(old,b,dt/2),m.lapse(start));
                double[] e=m.affineDerivative(add(old,d,dt),m.lapse(start));state=old.clone();
                for(int k=0;k<4;k++)state[k]+=dt*(a[k]+2*b[k]+2*d[k]+e[k])/6;
                double r=Math.hypot(state[0],state[1]);
                if(inside?r>=1:r<=1) {
                    double previous=Math.hypot(old[0],old[1]),fraction=(1-previous)/(r-previous);
                    double phi=Math.atan2(old[1]+fraction*(state[1]-old[1]),old[0]+fraction*(state[0]-old[0]));
                    assertEquals(phi,h,3e-6,"C="+c+", inside="+inside+", tangent="+tangent);
                    crossed=true;break;
                }
            }
            assertTrue(crossed);
        }
    }
    @Test void grazingEntryCanBeHiddenByTwoExteriorEndpoints() {
        for(double c:new double[]{.0625,.309,.5625}) {
            var m=new ExtendedSourceMetric(c,1);double u=c/1.00001,energy=c*c*(1-c)+1e-8;
            double v=Math.sqrt(energy-u*u*(1-u));
            double[] end=surfaceRk(m,u,v,energy,.08,false);
            assertTrue(end[0]<c && end[1]<0,"Both endpoints outside although the ray crosses the surface");
            double h=2*(c-u)/(v+Math.sqrt(energy-c*c*(1-c)));
            for(int i=0;i<3;i++) {double[] at=surfaceRk(m,u,v,energy,h,false);h+=(c-at[0])/at[1];}
            double[] at=surfaceRk(m,u,v,energy,h,false);
            assertTrue(h>0 && h<.08);assertEquals(c,at[0],1e-10);
            assertTrue(at[1]>0,"Use the entry crossing, not the later exit");
            // Continue from that exact boundary: a shallow interior excursion
            // must split at its exit too, rather than retain the interior ODE.
            double surfaceV=Math.sqrt(energy-c*c*(1-c));
            end=surfaceRk(m,c,surfaceV,energy,.08,true);
            assertTrue(end[0]<c);
            h=-2*surfaceV/surfaceAcceleration(m,c,energy,true);
            for(int i=0;i<3;i++) {at=surfaceRk(m,c,surfaceV,energy,h,true);h+=(c-at[0])/at[1];}
            at=surfaceRk(m,c,surfaceV,energy,h,true);
            assertTrue(h>0 && h<.08);assertEquals(c,at[0],1e-10);
            assertEquals(-surfaceV,at[1],1e-9,"Exit has the opposite radial slope");
        }
    }
    private static double surfaceAcceleration(ExtendedSourceMetric m,double u,double energy,boolean inside) {
        if(!inside)return 1.5*u*u-u;
        double c=m.compactness(),t=c*c*c/(u*u),d=1-c;
        return energy*t*(3-c-2*t)/(2*u*d*d)-u;
    }
    private static double[] surfaceRk(ExtendedSourceMetric m,double u,double v,double energy,double h,boolean inside) {
        double a=surfaceAcceleration(m,u,energy,inside),bv=v+h*a/2;
        double b=surfaceAcceleration(m,u+h*v/2,energy,inside),cv=v+h*b/2;
        double c=surfaceAcceleration(m,u+h*bv/2,energy,inside),dv=v+h*c;
        double d=surfaceAcceleration(m,u+h*cv,energy,inside);
        return new double[]{u+h*(v+2*bv+2*cv+dv)/6,v+h*(a+2*b+2*c+d)/6};
    }
    private static double[] add(double[] state,double[] derivative,double dt) {
        var result=state.clone();for(int i=0;i<4;i++)result[i]+=dt*derivative[i];return result;
    }
}
