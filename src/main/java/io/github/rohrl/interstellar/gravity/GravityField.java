package io.github.rohrl.interstellar.gravity;

/** Explicit gameplay force in blocks/tick²; not a timelike Schwarzschild integrator. */
public record GravityField(double x,double y,double z,int count,double bodyRadius,double horizonRadius,double strength) {
    public record Vector(double x,double y,double z) {
        public double length() {return Math.sqrt(x*x+y*y+z*z);}
    }
    public boolean supported() {return count>0&&bodyRadius<=16&&horizonRadius<=12;}
    public double reach() {return 2*Math.min(32,Math.max(Math.max(bodyRadius,horizonRadius)+2,20*Math.sqrt(count/64.0)));}
    public Vector acceleration(double px,double py,double pz) {
        double dx=x-px,dy=y-py,dz=z-pz,r=Math.sqrt(dx*dx+dy*dy+dz*dz),outer=reach();
        if(!supported()||r>=outer||r<1e-12)return new Vector(0,0,0);
        double t=Math.max(0,(r/outer-.6)/.4);
        double taper=1-t*t*t*(10+t*(-15+6*t));
        // Uniform interior mass for extended bodies; bounded force inside captured regions too.
        double core=Math.max(bodyRadius,horizonRadius),distance=Math.max(r,core);
        double factor=strength*count/(distance*distance*distance)*taper;
        factor=Math.min(factor,.35/r);
        return new Vector(dx*factor,dy*factor,dz*factor);
    }
    public double closestDistanceSquared(double ax,double ay,double az,double bx,double by,double bz) {
        double dx=bx-ax,dy=by-ay,dz=bz-az,length=dx*dx+dy*dy+dz*dz;
        double t=length==0?0:Math.max(0,Math.min(1,((x-ax)*dx+(y-ay)*dy+(z-az)*dz)/length));
        double rx=ax+t*dx-x,ry=ay+t*dy-y,rz=az+t*dz-z;
        return rx*rx+ry*ry+rz*rz;
    }
    /** A body touching the horizon is captured even if a solid source corner stops its centre. */
    public boolean intersectsBox(double minX,double minY,double minZ,double maxX,double maxY,double maxZ) {
        if(horizonRadius<=0)return false;
        double dx=Math.max(minX-x,Math.max(0,x-maxX)),dy=Math.max(minY-y,Math.max(0,y-maxY)),dz=Math.max(minZ-z,Math.max(0,z-maxZ));
        return dx*dx+dy*dy+dz*dz<=horizonRadius*horizonRadius*(1+1e-12);
    }
    /** Earliest segment/horizon contact, or -1. Starts inside capture immediately. */
    public double captureFraction(double ax,double ay,double az,double bx,double by,double bz) {
        if(horizonRadius<=0)return -1;
        double px=ax-x,py=ay-y,pz=az-z,dx=bx-ax,dy=by-ay,dz=bz-az;
        double c=px*px+py*py+pz*pz-horizonRadius*horizonRadius;
        if(c<=0)return 0;
        double a=dx*dx+dy*dy+dz*dz,b=px*dx+py*dy+pz*dz,disc=b*b-a*c;
        if(a==0||b>=0||disc<0)return -1;
        // Rationalized entering root avoids subtracting two nearly equal numbers.
        double t=c/(-b+Math.sqrt(disc));return t>=0&&t<=1?t:-1;
    }
}
