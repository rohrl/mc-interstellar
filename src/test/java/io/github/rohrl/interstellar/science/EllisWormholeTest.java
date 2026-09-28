package io.github.rohrl.interstellar.science;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EllisWormholeTest {
    @Test void isotropicChartsDescribeTheSameMetric() {
        var m=new EllisWormhole(8);
        for(double radius:new double[]{.5,2,4,7,20,100}) {
            double ell=m.properDistance(radius),omega=m.conformalFactor(radius);
            assertEquals(m.arealRadius(ell),omega*radius,1e-12);
            double eps=radius*1e-5;
            assertEquals(omega,(m.properDistance(radius+eps)-m.properDistance(radius-eps))/(2*eps),1e-7);
            assertEquals(Math.abs(ell),m.properDistance(m.coordinateRadius(ell)),1e-12);
        }
    }
    @Test void centralRayCrossesWithoutHorizonOrAcceleration() {
        var m=new EllisWormhole(8);var q=new EllisWormhole.Ray(3,-1,0);
        q=m.step(q,0,10);
        assertEquals(-7,q.ell(),0);assertEquals(-1,q.radialMomentum(),0);assertEquals(0,q.angle(),0);
        assertEquals(1,m.invariant(q,0),0);
    }
    @Test void throatCriticalRayRemainsCircular() {
        var m=new EllisWormhole(8);var q=new EllisWormhole.Ray(0,0,0);
        for(int i=0;i<1000;i++)q=m.step(q,8,.01);
        assertEquals(0,q.ell(),0);assertEquals(0,q.radialMomentum(),0);assertEquals(1.25,q.angle(),1e-12);
        assertEquals(1,m.invariant(q,8),0);
    }
    @Test void numericalRaysAgreeWithIndependentEllipticIntegrals() {
        var m=new EllisWormhole(1);double extent=1000;
        for(double impact:new double[]{0,.2,.8,.99,1.01,1.2,2,5}) {
            var q=new EllisWormhole.Ray(extent,-Math.sqrt(1-impact*impact/(extent*extent+1)),0);
            boolean escaped=false;
            for(int i=0;i<200000;i++) {
                double h=.003*Math.max(1,Math.abs(q.ell()));
                q=m.step(q,impact,h);
                assertEquals(1,m.invariant(q,impact),2e-8);
                if(impact<1?q.ell()<-extent:q.radialMomentum()>0 && q.ell()>extent) {escaped=true;break;}
            }
            assertTrue(escaped,"Ray did not reach an asymptotic end: b="+impact);
            // Add the asymptotic tails. Their omitted curvature is O(a²b/L³).
            double total=q.angle()+Math.asin(impact/m.arealRadius(extent))+Math.asin(impact/m.arealRadius(q.ell()));
            double exact=impact<1?m.transmittedAngle(impact):Math.PI+m.reflectedDeflection(impact);
            assertEquals(exact,total,2e-6,"b="+impact);
        }
    }
    @Test void rayReversalAndEndSymmetry() {
        var m=new EllisWormhole(2);var start=new EllisWormhole.Ray(3,-.8,0);double impact= Math.sqrt(.36*13);
        var q=start;var mirror=new EllisWormhole.Ray(-3,.8,0);
        for(int i=0;i<600;i++) {q=m.step(q,impact,.01);mirror=m.step(mirror,impact,.01);}
        assertEquals(-q.ell(),mirror.ell(),1e-12);assertEquals(q.angle(),mirror.angle(),1e-12);
        q=new EllisWormhole.Ray(q.ell(),-q.radialMomentum(),q.angle());
        for(int i=0;i<600;i++)q=m.step(q,-impact,.01);
        assertEquals(start.ell(),q.ell(),1e-9);assertEquals(-start.radialMomentum(),q.radialMomentum(),1e-9);assertEquals(0,q.angle(),1e-9);
    }
    @Test void transferIsReversibleAndPreservesLengthsAndHandedness() {
        var m=new EllisWormhole(8);
        for(double[] point:new double[][]{{0,0,-4},{2,3,-1},{-7,1,4}}) {
            var mapped=m.transfer(point);assertArrayEquals(point,m.transfer(mapped),1e-12);
            double[] v={.2,-.4,.9};var moved=m.transferVector(point,v);
            assertArrayEquals(v,m.transferVector(mapped,moved),1e-12);
            double radius=norm(point),other=norm(mapped);
            assertEquals(norm(v)*m.conformalFactor(radius),norm(moved)*m.conformalFactor(other),1e-12);
            double eps=1e-5;var ahead=point.clone();for(int j=0;j<3;j++)ahead[j]+=eps*v[j];
            var image=m.transfer(ahead);for(int j=0;j<3;j++)assertEquals(moved[j],(image[j]-mapped[j])/eps,4e-6);
            double[] x=m.transferVector(point,new double[]{1,0,0}),y=m.transferVector(point,new double[]{0,1,0}),z=m.transferVector(point,new double[]{0,0,1});
            double determinant=x[0]*(y[1]*z[2]-y[2]*z[1])-x[1]*(y[0]*z[2]-y[2]*z[0])+x[2]*(y[0]*z[1]-y[1]*z[0]);
            assertTrue(determinant>0,"A crossing must not mirror the player's camera");
        }
    }
    private static double norm(double[] a){return Math.sqrt(a[0]*a[0]+a[1]*a[1]+a[2]*a[2]);}
}
