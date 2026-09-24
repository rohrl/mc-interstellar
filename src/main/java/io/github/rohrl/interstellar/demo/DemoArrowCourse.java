package io.github.rohrl.interstellar.demo;

import java.util.List;

/** Deterministic launches, tuned for the reference 64-block source and default gravity. */
public final class DemoArrowCourse {
    private DemoArrowCourse() { }
    public record Shot(String name,int x,int y,int z,int dx,int dz,double vx,double vy,double vz) {
        public double muzzleX() {return x+.5+.7*dx;}
        public double muzzleY() {return y+.5;}
        public double muzzleZ() {return z+.5+.7*dz;}
    }
    public static final List<Shot> SHOTS=List.of(
            new Shot("loop",-6,81,1,0,1,0,.15,1.45),
            new Shot("flyby",-22,87,7,1,0,2,.2,0),
            new Shot("return",-8,82,1,-1,0,-.6,.55,0),
            new Shot("capture",1,85,-22,0,1,0,.15,1.3));
}
