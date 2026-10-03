package io.github.rohrl.interstellar.config;

/** Display choices for an assumed thin accretion disk, separate from source mass. */
public record AccretionSettings(int mode,boolean animation,float brightness,float outerRadius,
                                float tilt,float threshold,float glow) {
    public AccretionSettings {
        if(mode<0||mode>2||!Float.isFinite(brightness)||brightness<.1f||brightness>4
                ||!Float.isFinite(outerRadius)||outerRadius<4||outerRadius>24
                ||!Float.isFinite(tilt)||tilt<0||tilt>90
                ||!Float.isFinite(threshold)||threshold<1||threshold>128
                ||!Float.isFinite(glow)||glow<0||glow>4)
            throw new IllegalArgumentException("Accretion disk settings out of range");
    }
    public AccretionSettings(int mode,boolean animation,float brightness,float outerRadius,float tilt,float threshold) {
        this(mode,animation,brightness,outerRadius,tilt,threshold,2.4f);
    }
    public static AccretionSettings defaults(){return new AccretionSettings(1,true,1,10,15,16,2.4f);}
    public boolean visible(boolean blackHole,double radius){return blackHole&&mode!=0&&(mode==2||radius>=threshold);}
    public String modeName(){return mode==0?"Off":mode==1?"Auto: large BHs":"All BHs";}
}
