package io.github.rohrl.interstellar.demo;

import io.github.rohrl.interstellar.gravity.GravityField;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DemoArrowCourseTest {
    private static final GravityField FIELD=new GravityField(2,82,2,64,Math.sqrt(12),Math.sqrt(12),GravityField.DEFAULT_STRENGTH);
    private record Result(double winding,double minX,double endX,double endZ,boolean captured) { }
    @Test void loopCompletesARevolutionWithOrdinaryGravityAndDrag() {
        var r=reference(0);
        assertTrue(Math.abs(r.winding)>2*Math.PI,"Transient orbit, not a permanent drag-free circle");
    }
    @Test void flybyEscapesWhileOutwardShotTurnsBackAndCaptureShotFallsIn() {
        var flyby=reference(1);assertFalse(flyby.captured);assertTrue(flyby.endX>10&&flyby.endZ<0);
        var turn=reference(2);assertTrue(turn.minX<DemoArrowCourse.SHOTS.get(2).muzzleX()-1);
        assertTrue(turn.endX>turn.minX+2);assertTrue(turn.captured);
        assertTrue(reference(3).captured);
    }
    /** Continuous drag/gravity RK4 reference, independent of the native-tick/substep hooks. */
    private static Result reference(int station) {
        var s=DemoArrowCourse.SHOTS.get(station);
        double[] q={s.muzzleX(),s.muzzleY(),s.muzzleZ(),s.vx(),s.vy(),s.vz()};
        double angle=0,previous=Math.atan2(q[2]-2,q[0]-2),minX=q[0];boolean captured=false;
        double h=.02;
        for(int step=0;step<8000;step++) {
            var a=derivative(q);var b=derivative(offset(q,a,h/2));var c=derivative(offset(q,b,h/2));var d=derivative(offset(q,c,h));
            for(int i=0;i<6;i++)q[i]+=h*(a[i]+2*b[i]+2*c[i]+d[i])/6;
            double next=Math.atan2(q[2]-2,q[0]-2);
            angle+=Math.atan2(Math.sin(next-previous),Math.cos(next-previous));previous=next;minX=Math.min(minX,q[0]);
            captured=FIELD.captureFraction(q[0],q[1],q[2],q[0],q[1],q[2])>=0;
            if(captured||q[1]<65)break;
        }
        return new Result(angle,minX,q[0],q[2],captured);
    }
    private static double[] derivative(double[] q) {
        var a=FIELD.acceleration(q[0],q[1],q[2]);double drag=Math.log(.99);
        return new double[]{q[3],q[4],q[5],a.x()+drag*q[3],a.y()-.05+drag*q[4],a.z()+drag*q[5]};
    }
    private static double[] offset(double[] q,double[] d,double h) {
        double[] result=q.clone();for(int i=0;i<6;i++)result[i]+=d[i]*h;return result;
    }
}
