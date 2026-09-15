package io.github.rohrl.interstellar.science;

/** Diagnostic only: affine-parameter Dormand-Prince reference, independent of GPU inverse-radius RK4/DDA. */
public final class FiniteTerrainRay {
    private FiniteTerrainRay() { }
    public record Point(double x,double y,double z) {
        public Point add(Point p) {return new Point(x+p.x,y+p.y,z+p.z);}
        public Point subtract(Point p) {return new Point(x-p.x,y-p.y,z-p.z);}
        public Point scale(double s) {return new Point(x*s,y*s,z*s);}
        public double dot(Point p) {return x*p.x+y*p.y+z*p.z;}
        public double length() {return Math.sqrt(dot(this));}
        public Point unit() {return scale(1/length());}
        double axis(int a) {return a==0?x:a==1?y:z;}
    }
    public interface Scene {
        int side();
        int value(int x,int y,int z);
        double height(int x,int y,int z);
    }
    public enum Outcome { HIT, CAPTURED, MISSING, UNRESOLVED }
    public record Result(Outcome outcome,int x,int y,int z,int material,double invariantError,int steps) {
        public int code() {return switch(outcome) {case HIT -> material;case CAPTURED -> -1;case MISSING -> 0;case UNRESOLVED -> -2;};}
        public boolean sameHit(Result other) {return code()==other.code() && (outcome!=Outcome.HIT || x==other.x&&y==other.y&&z==other.z);}
    }
    private record Contact(double time,int x,int y,int z,int material) { }
    private static final double[][] WEIGHTS={
            {1.0/5},{3.0/40,9.0/40},{44.0/45,-56.0/15,32.0/9},
            {19372.0/6561,-25360.0/2187,64448.0/6561,-212.0/729},
            {9017.0/3168,-355.0/33,46732.0/5247,49.0/176,-5103.0/18656},
            {35.0/384,0,500.0/1113,125.0/192,-2187.0/6784,11.0/84},
            {5179.0/57600,0,7571.0/16695,393.0/640,-92097.0/339200,187.0/2100,1.0/40}};

    /** E=1, L=r*sin(alpha)/sqrt(1-r_s/r); y=(r,dr/dlambda,phi). All distances in blocks. */
    public static Result trace(Scene scene,Point source,Point camera,Point direction,double rs,double chord,double tolerance) {
        Point offset=camera.subtract(source);
        double radius=offset.length();
        if(!Double.isFinite(radius)||!Double.isFinite(rs)||rs<0||radius<=rs||!Double.isFinite(chord)||chord<=0||chord>1
                ||!Double.isFinite(tolerance)||tolerance<1e-13||tolerance>1e-5||scene.side()<1
                ||!Double.isFinite(direction.length())||direction.length()==0)
            throw new IllegalArgumentException("Invalid finite reference ray");
        Point radial=offset.unit(),look=direction.unit();
        if(rs==0) {
            Point end=camera.add(look.scale(2*(radius+source.length()+Math.sqrt(3)*scene.side())));
            double[] interval=clip(camera,end,scene.side());
            Contact hit=interval==null?null:contact(scene,camera,end,interval);
            return hit==null?terminal(Outcome.MISSING,0,0):new Result(Outcome.HIT,hit.x,hit.y,hit.z,hit.material,0,0);
        }
        double mu=Math.max(-1,Math.min(1,look.dot(radial)));
        Point tangent=look.subtract(radial.scale(mu));
        double sine=tangent.length(),angular=radius*sine/Math.sqrt(1-rs/radius);
        tangent=sine>1e-14?tangent.scale(1/sine):new Point(0,0,0);
        double[] state={radius,mu,0};
        Point position=camera;
        double step=chord*.5,invariantError=0;
        double extent=Math.sqrt(square(Math.max(Math.abs(source.x),Math.abs(scene.side()-source.x)))
                +square(Math.max(Math.abs(source.y),Math.abs(scene.side()-source.y)))
                +square(Math.max(Math.abs(source.z),Math.abs(scene.side()-source.z))));
        for(int attempt=1;attempt<=100000;attempt++) {
            if(state[1]>0 && state[0]>Math.max(extent,1.5*rs)) return terminal(Outcome.MISSING,invariantError,attempt);
            double speed=Math.hypot(state[1],angular/state[0]);
            step=Math.min(step,Math.min(chord*.5/Math.max(speed,1e-6),state[0]*.05));
            double[][] k=new double[7][];k[0]=derivative(state,angular,rs);
            for(int j=0;j<6;j++) k[j+1]=derivative(stage(state,step,k,WEIGHTS[j]),angular,rs);
            double[] next=stage(state,step,k,WEIGHTS[5]),lower=stage(state,step,k,WEIGHTS[6]);
            double error=0;
            for(int j=0;j<3;j++) error=Math.max(error,Math.abs(next[j]-lower[j])/(tolerance*Math.max(1,Math.abs(next[j]))));
            if(!Double.isFinite(error)||step<1e-12) return terminal(Outcome.UNRESOLVED,invariantError,attempt);
            if(error<=1) {
                boolean captured=rs>0&&next[0]<=rs;
                if(captured) {double fraction=(state[0]-rs)/(state[0]-next[0]);next[2]=state[2]+fraction*(next[2]-state[2]);next[0]=rs;}
                Point end=source.add(radial.scale(next[0]*Math.cos(next[2]))).add(tangent.scale(next[0]*Math.sin(next[2])));
                if(end.subtract(position).length()>chord) {step*=.5;continue;}
                double[] interval=clip(position,end,scene.side());
                if(interval!=null) {
                    Contact contact=contact(scene,position,end,interval);
                    if(contact!=null) {
                        Point hit=position.add(end.subtract(position).scale(contact.time));
                        if(hit.subtract(source).length()<rs) return terminal(Outcome.CAPTURED,invariantError,attempt);
                        return new Result(Outcome.HIT,contact.x,contact.y,contact.z,contact.material,invariantError,attempt);
                    }
                    if(interval[1]<1) return terminal(Outcome.MISSING,invariantError,attempt);
                }
                if(captured) return terminal(Outcome.CAPTURED,invariantError,attempt);
                invariantError=Math.max(invariantError,Math.abs(next[1]*next[1]+(1-rs/next[0])*square(angular/next[0])-1));
                if(next[2]>=16) return terminal(Outcome.UNRESOLVED,invariantError,attempt);
                position=end;state=next;
            }
            step*=error==0?4:Math.max(.2,Math.min(4,.9*Math.pow(error,-.2)));
        }
        return terminal(Outcome.UNRESOLVED,invariantError,100000);
    }
    private static double square(double x) {return x*x;}
    private static Result terminal(Outcome kind,double error,int steps) {return new Result(kind,0,0,0,0,error,steps);}
    private static double[] derivative(double[] s,double angular,double rs) {
        return new double[]{s[1],square(angular)/(s[0]*s[0]*s[0])*(1-1.5*rs/s[0]),angular/(s[0]*s[0])};
    }
    private static double[] stage(double[] state,double step,double[][] k,double[] weights) {
        double[] result=state.clone();
        for(int i=0;i<weights.length;i++) for(int j=0;j<3;j++) result[j]+=step*weights[i]*k[i][j];
        return result;
    }
    private static double[] clip(Point a,Point b,int side) {
        double enter=0,leave=1;
        for(int axis=0;axis<3;axis++) {
            double start=a.axis(axis),d=b.axis(axis)-start;
            if(Math.abs(d)<1e-15) {if(start<0||start>=side)return null;}
            else {double t0=-start/d,t1=(side-start)/d;enter=Math.max(enter,Math.min(t0,t1));leave=Math.min(leave,Math.max(t0,t1));}
        }
        return leave>enter?new double[]{enter,leave}:null;
    }
    private static Contact contact(Scene scene,Point a,Point b,double[] interval) {
        Point d=b.subtract(a),start=a.add(d.scale(interval[0])),end=a.add(d.scale(interval[1]));
        int[] min=new int[3],max=new int[3];
        for(int axis=0;axis<3;axis++) {
            min[axis]=Math.max(0,(int)Math.floor(Math.min(start.axis(axis),end.axis(axis))));
            max[axis]=Math.min(scene.side()-1,(int)Math.floor(Math.max(start.axis(axis),end.axis(axis))));
        }
        Contact best=null;
        // Enumerate the small chord AABB and use independent cuboid slabs, never GPU voxel stepping.
        for(int y=min[1];y<=max[1];y++) for(int z=min[2];z<=max[2];z++) for(int x=min[0];x<=max[0];x++) {
            int value=scene.value(x,y,z);if(value==0)continue;
            double t=BoxRay.entry(a.x,a.y,a.z,d.x,d.y,d.z,x,y,z,scene.height(x,y,z));
            if(t<=interval[1] && (best==null||t<best.time))best=new Contact(t,x,y,z,value);
        }
        return best;
    }
}
