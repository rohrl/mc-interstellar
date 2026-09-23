package io.github.rohrl.interstellar.science;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExtendedSourceMetricTest {
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
    private static double[] add(double[] state,double[] derivative,double dt) {
        var result=state.clone();for(int i=0;i<4;i++)result[i]+=dt*derivative[i];return result;
    }
}
