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
            new Shot("loop",-3,80,1,0,1,0,.35,.875),
            new Shot("flyby",-14,83,7,1,0,1.1,.15,0),
            new Shot("return",-4,82,1,-1,0,-.3,.45,0),
            new Shot("capture",1,83,-15,0,1,0,.3,.95));
}
