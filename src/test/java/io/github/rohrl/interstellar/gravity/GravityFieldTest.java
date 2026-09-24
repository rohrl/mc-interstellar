package io.github.rohrl.interstellar.gravity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GravityFieldTest {
    private final GravityField hole=new GravityField(0,0,0,64,Math.sqrt(12),Math.sqrt(12),.05);
    @Test void inverseSquareLiftAndFiniteCentre() {
        assertEquals(4,hole.acceleration(0,-4,0).length()/hole.acceleration(0,-8,0).length(),1e-12);
        assertTrue(hole.acceleration(0,-4,0).y()>.08,"Nearby pull exceeds ordinary downward mob gravity");
        assertTrue(hole.acceleration(0,-12,0).y()<.08);
        assertEquals(0,hole.acceleration(0,0,0).length());
    }
    @Test void smoothCutoffAndInterior() {
        assertEquals(40,hole.reach());
        assertTrue(hole.acceleration(30,0,0).length()>0,"Previously unaffected perimeter now attracts");
        assertEquals(0,hole.acceleration(40,0,0).length());assertEquals(0,hole.acceleration(41,0,0).length());
        assertTrue(hole.acceleration(40-1e-4,0,0).length()<1e-12);
        double r=hole.bodyRadius();assertEquals(hole.acceleration(r-1e-7,0,0).length(),hole.acceleration(r+1e-7,0,0).length(),1e-7);
    }
    @Test void sweptCaptureOrdersFastHitsAndMisses() {
        assertEquals((20-Math.sqrt(12))/40,hole.captureFraction(-20,0,0,20,0,0),1e-12);
        assertEquals(-1,hole.captureFraction(-20,4,0,20,4,0));assertEquals(-1,hole.captureFraction(-20,0,0,-30,0,0));
        assertEquals(0,hole.captureFraction(0,0,0,50,0,0));
        var star=new GravityField(0,0,0,27,Math.sqrt(27)/2,0,.05);
        assertEquals(-1,star.captureFraction(-20,0,0,20,0,0));
    }
    @Test void surfaceContactCapturesWithoutAFalseBoxCornerCapture() {
        assertTrue(hole.intersectsBox(3.4,-.5,-.5,4.4,.5,.5));
        assertFalse(hole.intersectsBox(3.5,3.5,3.5,4.5,4.5,4.5));
        assertFalse(hole.intersectsBox(-4,-4,-4,-3.5,-3.5,-3.5));
    }
    @Test void momentumIsRadialAndExteriorOrbitMatchesIndependentCircle() {
        var field=new GravityField(0,0,0,64,Math.sqrt(12),0,.05);
        double radius=8,omega=Math.sqrt(.05*64/(radius*radius*radius)),period=2*Math.PI/omega;
        double x=radius,y=0,vx=0,vy=radius*omega,dt=period/20000;
        for(int i=0;i<20000;i++) {
            var a=field.acceleration(x,y,0);vx+=a.x()*dt/2;vy+=a.y()*dt/2;
            x+=vx*dt;y+=vy*dt;a=field.acceleration(x,y,0);vx+=a.x()*dt/2;vy+=a.y()*dt/2;
        }
        assertEquals(radius,x,1e-5);assertEquals(0,y,1e-5);assertEquals(radius*radius*omega,x*vy-y*vx,1e-10);
    }
}
