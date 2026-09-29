package io.github.rohrl.interstellar.science;

/** Gameplay envelope for a prescribed local observer speed, independent of block/tick speed. */
public final class SprintObserver {
    /** Require intended travel as well as displacement: walls and passive pushes do not charge. */
    public static boolean walking(float forward,float sideways,double distance,boolean flying,boolean gliding,boolean swimming) {
        return (forward!=0 || sideways!=0) && distance>.015 && !flying && !gliding && !swimming;
    }
    private double elapsed,beta;
    public double beta(){return beta;}
    public double elapsed(){return elapsed;}
    public void reset(){elapsed=beta=0;}
    public void advance(boolean running,double dt,double cap,double seconds) {
        if(!Double.isFinite(dt)||dt<0||!Double.isFinite(cap)||cap<.1||cap>.99||!Double.isFinite(seconds)||seconds<=0)
            throw new IllegalArgumentException("Invalid observer envelope");
        if(running) {
            elapsed+=dt;double t=Math.min(1,elapsed/seconds);
            double target=Math.tanh(atanh(.1)+(atanh(cap)-atanh(.1))*t);
            // Ease the onset without changing the eventual cap or the sprint timer.
            beta=Math.min(target,beta+dt*1.5);
        } else {elapsed=0;beta=Math.max(0,beta-dt*3);}
    }
    private static double atanh(double x){return .5*Math.log((1+x)/(1-x));}
}
