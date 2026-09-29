package io.github.rohrl.interstellar.wormhole;

/** Client-local preparation progress. Only a presented passage can finish opening. */
public final class OpeningProgress {
    public static final double REVEAL_SECONDS=.35,IDLE_RADIUS=.8;
    private boolean paired,prepared,revealing,presented,open;
    private double percent,age,reveal;
    public void reset(boolean paired) {
        this.paired=paired;prepared=revealing=presented=open=false;percent=age=reveal=0;
    }
    public void advance(double work,boolean opticalPrepared,double seconds) {
        double dt=Math.max(0,Math.min(.1,seconds));age+=dt;prepared|=opticalPrepared;
        if(!paired)return;
        double target=Math.min(99,95*Math.clamp(work,0,1)+(prepared?4:0));
        percent=Math.max(percent,Math.min(target,percent+85*dt));
        if(revealing && presented) {reveal=Math.min(REVEAL_SECONDS,reveal+dt);open=reveal>=REVEAL_SECONDS;}
    }
    public boolean canReveal(boolean regionsComplete) {return paired && prepared && regionsComplete && percent>=99-1e-6;}
    public void beginReveal() {revealing=true;}
    public void presented() {if(revealing)presented=true;}
    public boolean revealing() {return revealing;}
    public boolean open() {return open;}
    public int percent() {return open?100:(int)Math.min(99,percent);}
    public double radius(double target) {return Math.max(.1,Math.min(1,age/.3)*IDLE_RADIUS)+(target-IDLE_RADIUS)*(paired?percent/99:0);}
    public float previousImageOpacity() {double t=reveal/REVEAL_SECONDS;return (float)(1-t*t*(3-2*t));}
}
