package io.github.rohrl.interstellar.gravity;

/** Quasi-static visual sag in the local gameplay field. Endpoints stay attached;
 * this does not simulate rope mass, collisions or transfer force to the holder. */
public final class StringDeflection {
    private StringDeflection() {}
    public static GravityField.Vector offset(GravityField field,double x,double y,double z,double length,double t) {
        if(field==null || length<=0 || t<=0 || t>=1)return new GravityField.Vector(0,0,0);
        var a=field.acceleration(x,y,z);
        double window=4*t*(1-t),gain=Math.min(length*length*.25,64)*window;
        double amount=a.length()*gain,limit=Math.min(2,length*.25)*window;
        if(amount>limit)gain*=limit/amount;
        return new GravityField.Vector(a.x()*gain,a.y()*gain,a.z()*gain);
    }
}
